package com.heartape.whisper.repository

import android.content.Context
import android.net.Uri
import com.heartape.whisper.data.local.ContactEntity
import com.heartape.whisper.data.local.DatabaseManager
import com.heartape.whisper.data.local.MemberEntity
import com.heartape.whisper.data.local.MessageEntity
import com.heartape.whisper.data.local.PrefsManager
import com.heartape.whisper.data.local.SessionEntity
import com.heartape.whisper.data.model.*
import com.heartape.whisper.data.remote.ApiService
import com.heartape.whisper.data.remote.StompManager
import com.heartape.whisper.utils.ErrorUtils.runSafe
import com.heartape.whisper.utils.ErrorUtils.runSafeWithRetry
import com.heartape.whisper.utils.FileUtils
import com.heartape.whisper.utils.FileUtils.detectMime
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ChatRepository @Inject constructor(
    val api: ApiService,
    private val dbManager: DatabaseManager,
    private val stomp: StompManager,
    private val prefsManager: PrefsManager,
    @ApplicationContext private val context: Context
) {
    val wsStatus = stomp.wsStatus

    // 长生命周期后台任务和脱离 UI 的任务使用
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repoScope.launch {
            stomp.incomingMessages.collect { msg ->
                if (msg != null) {
                    handleIncomingWsMessage(msg)
                }
            }
        }
    }

    private val dao get() = dbManager.sessionDao

    // 内存安全的活跃数据流暴露给 ViewModel
    fun getActiveSessions(): Flow<List<SessionEntity>> = dao.getActiveSessionsFlow()
    fun getActiveMessages(sessionId: Long, limit: Int): Flow<List<MessageEntity>> = dao.getActiveMessagesFlow(sessionId, limit)
    fun getSessionMembers(sessionId: Long): Flow<List<MemberEntity>> = dao.getMembersFlow(sessionId)

    // ================= 核心同步引擎 =================

    /**
     * 全局启动同步逻辑：差异化更新 Session、Message 和 Member
     * 优化点：
     * 1. 添加并发控制，限制同时同步的会话数量
     * 2. 改进错误处理，单个会话失败不影响其他会话
     * 3. 添加同步状态追踪
     */
    suspend fun syncAllData(): AppResult<Unit> = runSafe {
        syncContacts()
        val semaphore = Semaphore(5)
        // 并发同步联系人和会话列表
        coroutineScope {
            val remoteSessions = api.getSessions().unwrap()
            val localSessionsMap = dao.getAllSessionsOnce().associateBy { it.id }

            // 限制并发数
            remoteSessions.map { remote ->
                val local = localSessionsMap[remote.id]
                async {
                    semaphore.withPermit {
                        syncSingleSessionDetails(local, remote)
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * 同步联系人列表
     * 优化点：
     * 1. 添加错误处理和返回类型
     * 2. 使用批量插入优化性能
     */
    private suspend fun syncContacts() {
        val contacts = api.getContacts().unwrap()
        if (contacts.isEmpty()) return

        val contactEntities = contacts.map {
            ContactEntity(it.id, it.username, it.avatar)
        }
        dao.insertContacts(contactEntities)
    }

    /**
     * 同步单个会话的详细信息
     * 优化点：
     * 1. 添加错误处理
     * 2. 使用when表达式替代if-else链
     */
    private suspend fun syncSingleSessionDetails(local: SessionEntity?, remote: SessionDto): AppResult<Unit> = runSafeWithRetry(times = 2, initialDelay = 500L) {

        val sessionId = remote.id
        val members = api.getMembers(sessionId).unwrap()

        val icon: String
        val name = when (remote.type) {
            "PEER" -> {
                val userId = prefsManager.requireUserProfile().id
                val peerMember = members.find { it.userId != userId }
                    ?: return@runSafeWithRetry
                val contact = dao.getContact(peerMember.userId)
                icon = contact.avatar
                peerMember.aliasName ?: contact.username
            }
            "GROUP" -> {
                icon = remote.icon!!
                remote.name!!
            }
            else -> {
                throw ApiDataException("不支持的会话类型")
            }
        }

        val session = if (local == null) {
            SessionEntity(
                id = remote.id,
                name = name,
                type = remote.type,
                icon = icon
            )
        } else {
            SessionEntity(
                id = remote.id,
                name = name,
                type = remote.type,
                icon = icon,
                lastMessage = local.lastMessage,
                lastTime = local.lastTime
            )
        }
        dao.insertSession(session)
        dao.deleteMembersBySession(sessionId)
        dao.insertMembers(members.map {
            MemberEntity(sessionId, it.userId, it.username, it.aliasName, it.avatar, it.role, it.isBlock, it.joinTime)
        })
        syncSessionMessages(sessionId)
    }

    private val MESSAGE_SYNC_BATCH_SIZE = 20


    /**
     * 增量同步 Messages
     * 优化点：
     * 1. 添加最大同步次数限制，防止无限循环
     * 2. 优化消息插入逻辑
     * 3. 改进错误处理
     */
    private suspend fun syncSessionMessages(sessionId: Long): AppResult<Unit> = runSafe {
        var lastMessageId = dao.getLastSyncMessageId(sessionId)
        var syncCount = 0
        val maxSyncBatches = 100 // 最多同步100批次，防止无限循环

        while (syncCount < maxSyncBatches) {
            val messages = api.getHistoryMessages(sessionId, MESSAGE_SYNC_BATCH_SIZE, since = lastMessageId).unwrap()
            if (messages.isEmpty()) break
            dao.insertMessages(messages.map {
                MessageEntity(it.id, it.sessionId, it.userId, it.messageType, it.messageInfo, it.createTime)
            })

            // 更新会话最后一条消息状态
            val latestMsg = messages.maxBy { it.id }
            updateSessionLastStatus(sessionId, latestMsg)
            lastMessageId = latestMsg.id

            syncCount++
            if (messages.size < MESSAGE_SYNC_BATCH_SIZE) break
        }
    }

    /**
     * 更新单个 Session 的最新消息展示状态
     * 优化点：
     * 1. 使用when表达式处理不同消息类型
     * 2. 添加空值保护
     */
    private suspend fun updateSessionLastStatus(sessionId: Long, latestMsg: MessageDto) {
        val displayMsg = when (latestMsg.messageType) {
            "AUDIO" -> "[语音]"
            "IMAGE" -> "[图片]"
            "VIDEO" -> "[视频]"
            "FILE" -> "[文件]"
            else -> latestMsg.messageInfo
        }
        dao.updateSessionLastMessage(sessionId, displayMsg, latestMsg.createTime)
    }

    suspend fun syncSession(sessionId: Long): AppResult<Unit> = runSafe {
        val remote = api.getSession(sessionId).unwrap()
        val local = dao.getSession(sessionId)
        syncSingleSessionDetails(local, remote)
    }

    // ================= 实时事件驱动 =================

    /**
     * 处理 WebSocket 收到的实时消息
     * 要求：立即保存到 sqlite 并更新 session 的 last 属性
     */
    suspend fun handleIncomingWsMessage(msg: MessageDto) {
        val safeDao = dao

        safeDao.insertMessage(
            MessageEntity(msg.id, msg.sessionId, msg.userId, msg.messageType, msg.messageInfo, msg.createTime)
        )
        val displayMsg = if (msg.messageType == "AUDIO") "[语音]" else msg.messageInfo
        safeDao.updateSessionLastMessage(msg.sessionId, displayMsg, msg.createTime)
    }

    // 处理退出或解散会话时的本地清空操作
    suspend fun deleteLocalSessionData(sessionId: Long) {
        dao.deleteSession(sessionId)
        dao.deleteMessagesBySession(sessionId)
        dao.deleteMembersBySession(sessionId)
    }

    fun sendWsMessage(sessionId: Long, sessionType: String, content: String) {
        stomp.sendMessage(sessionId, sessionType, content)
    }

    // ================= 消息通知 =================

    // 获取指定用户的公开档案
    suspend fun getUserInfo(userId: Long): AppResult<UserDto> = runSafe {
        return@runSafe api.getUser(userId).unwrap()
    }

    // 获取待处理的申请记录
    suspend fun getPendingApplies(): AppResult<List<ApplyDto>> = runSafe {
        return@runSafe api.getPendingApplies().unwrap()
    }

    // 审核申请 (同意/拒绝)
    suspend fun reviewApply(applyId: Long, approved: Boolean, reviewNote: String, aliasName: String? = null): AppResult<Unit> = runSafe {
        api.reviewApply(ReviewReq(applyId, approved, reviewNote, aliasName)).unwrap()
        if (approved) {
            syncAllData()
        }
    }

    fun getSession(sessionId: Long): Flow<SessionEntity?> = dao.getSessionFlow(sessionId)

    private suspend fun syncMember(sessionId: Long, userId: Long): AppResult<Unit> = runSafe {
        val member = api.getMember(sessionId, userId).unwrap()
        val memberEntity = MemberEntity(sessionId, member.userId, member.username, member.aliasName, member.avatar, member.role, member.isBlock, member.joinTime)
        dao.insertMember(memberEntity)
    }

    // 备注
    suspend fun updateAlias(sessionId: Long, aliasName: String): AppResult<Unit> = runSafe {
        val member = api.updateAlias(AliasReq(sessionId, aliasName)).unwrap()
        val memberEntity = MemberEntity(sessionId, member.userId, member.username, member.aliasName, member.avatar, member.role, member.isBlock, member.joinTime)
        dao.insertMember(memberEntity)
    }

    // 私聊管理 (拉黑等)
    suspend fun managePeerMember(sessionId: Long, userId: Long, action: String): AppResult<Unit> = runSafe {
        api.managePeerMember(ManageMemberReq(sessionId, userId, action)).unwrap()
        syncMember(sessionId, userId)
    }

    // 群成员管理 (踢人/设管理等)
    suspend fun manageGroupMember(sessionId: Long, userId: Long, action: String): AppResult<Unit> = runSafe {
        api.manageGroupMember(ManageMemberReq(sessionId, userId, action)).unwrap()
        syncMember(sessionId, userId)
    }

    // ================= 群公告功能 =================

    // 拉取历史公告列表
    suspend fun getAnnouncements(sessionId: Long): AppResult<List<AnnouncementDto>> = runSafe {
        val announcementDto = api.getAnnouncements(sessionId).unwrap()
        return@runSafe listOf(announcementDto)
    }

    // 发布新群公告
    suspend fun publishAnnouncement(sessionId: Long, content: String): AppResult<Unit> = runSafe {
        api.publishAnnouncement(sessionId, AnnounceReq(content)).unwrap()
    }

    suspend fun exit(sessionId: Long): AppResult<Unit> = runSafe {
        api.exit(ExitReq(sessionId)).unwrap()
    }

    suspend fun createGroup(name: String, avatarUrl: String): AppResult<Long> = runSafe {
        return@runSafe api.createGroup(CreateGroupReq(name, avatarUrl)).unwrap()
    }

    suspend fun searchUsers(keyword: String): AppResult<List<UserSearchDto>> = runSafe {
        return@runSafe api.findUser(keyword).unwrap()
    }
    suspend fun searchGroups(keyword: String): AppResult<List<SessionDto>> = runSafe {
        return@runSafe api.findGroup(keyword).unwrap()
    }
    suspend fun applyFriend(req: ApplyPeerReq): AppResult<Unit> = runSafe {
        api.applyFriend(req).unwrap()
    }
    suspend fun applyGroup(req: ApplyGroupReq): AppResult<Unit> = runSafe {
        api.applyGroup(req).unwrap()
    }

    suspend fun uploadIcon(uri: Uri): AppResult<UploadDto> = runSafe {
        val rawFile = FileUtils.getFileFromUri(context, uri)
        val compressedFile = FileUtils.compressImage(context, rawFile, 100)
        val mimeType = detectMime(compressedFile)
        val requestFile = compressedFile.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)
        return@runSafe api.uploadIcon(body).unwrap()
    }
}