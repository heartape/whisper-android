package com.heartape.whisper.repository

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
import com.heartape.whisper.utils.FileUtils.detectMime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URLConnection
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ChatRepository @Inject constructor(
    val api: ApiService,
    private val dbManager: DatabaseManager,
    private val stomp: StompManager,
    private val prefsManager: PrefsManager
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
     */
    suspend fun syncAllData(): AppResult<Unit> = runSafeWithRetry(times = 3) {
        syncContacts()
        val remoteSessions = api.getSessions().unwrap()
        val localSessions = dao.getAllSessionsOnce()
        val localSessionsMap = localSessions.associateBy { it.id }

        // 利用协程并发
        coroutineScope {
            remoteSessions.map { remote ->
                val local = localSessionsMap[remote.id]
                async {
                    // todo:收集错误用于重试
                    syncSingleSessionDetails(local, remote)
                }
            }.awaitAll()
        }
    }

    suspend fun syncContacts() {
        val contacts = api.getContacts().unwrap()
        val contactEntities = contacts.map {
            ContactEntity(it.id, it.username, it.avatar)
        }
        dao.insertContacts(contactEntities)
    }

    private suspend fun syncSingleSessionDetails(local: SessionEntity?, remote: SessionDto) {
        if (remote.type == "PEER") {
            syncPeerSessionDetails(local, remote)
        } else if (remote.type == "GROUP") {
            syncGroupSessionDetails(local, remote)
        }
    }

    private val count = 20

    private suspend fun syncPeerSessionDetails(local: SessionEntity?, remote: SessionDto): AppResult<Unit> = runSafeWithRetry(times = 2, initialDelay = 500L)  {
        val sessionId = remote.id
        val members = api.getMembers(sessionId).unwrap()
        val userId = prefsManager.currentUserProfile.id
        dao.deleteMembersBySession(sessionId)
        val peerMember = members.find { it.userId != userId }
        if (peerMember == null) {
            return@runSafeWithRetry
        }

        val contact = dao.getContact(peerMember.userId)
        val name = peerMember.aliasName ?: contact.username
        val session = if (local == null) {
            SessionEntity(
                id = remote.id,
                name = name,
                type = remote.type,
                icon = contact.avatar
            )
        } else {
            SessionEntity(
                id = remote.id,
                name = name,
                type = remote.type,
                icon = contact.avatar,
                lastMessage = local.lastMessage,
                lastTime = local.lastTime
            )
        }
        dao.insertSession(session)
        // 全量刷新 Members (覆盖同步)
        dao.insertMembers(members.map {
            MemberEntity(sessionId, it.userId, it.username, it.aliasName, it.avatar, it.role, it.isBlock, it.joinTime)
        })
        syncSessionMessages(sessionId)
    }

    /**
     * 增量同步 Messages
     */
    private suspend fun syncSessionMessages(sessionId: Long) {
        while (true) {
            val lastMessageId= dao.getLastSyncMessageId(sessionId)
            val messages = api.getHistoryMessages(sessionId, count, since = lastMessageId).unwrap()
            if (messages.isEmpty()) {
                break
            }
            dao.insertMessages(messages.map {
                MessageEntity(it.id, it.sessionId, it.userId, it.messageType, it.messageInfo, it.createTime)
            })
            val latestMsg = messages.maxBy { it.id }
            updateSessionLastStatus(sessionId, latestMsg)
            if (messages.size < count) {
                break
            }
        }
    }

    /**
     * 单个会话的数据同步 (用于建群成功、通过好友申请、通过入群申请后触发)
     */
    private suspend fun syncGroupSessionDetails(local: SessionEntity?, remote: SessionDto): AppResult<Unit> = runSafeWithRetry(times = 2, initialDelay = 500L)  {
        if (remote.name == null || remote.icon == null || remote.name.isEmpty() || remote.icon.isEmpty()) {
            return@runSafeWithRetry
        }
        val session = if (local == null) {
            SessionEntity(
                id = remote.id,
                name = remote.name,
                type = remote.type,
                icon = remote.icon
            )
        } else {
            SessionEntity(
                id = remote.id,
                name = remote.name,
                type = remote.type,
                icon = remote.icon,
                lastMessage = local.lastMessage,
                lastTime = local.lastTime
            )
        }
        dao.insertSession(session)
        val sessionId = session.id
        syncSessionMessages(sessionId)
        // 全量刷新 Members (覆盖同步)
        val members = api.getMembers(sessionId).unwrap()
        dao.deleteMembersBySession(sessionId)
        dao.insertMembers(members.map {
            MemberEntity(sessionId, it.userId, it.username, it.aliasName, it.avatar, it.role, it.isBlock, it.joinTime)
        })
    }

    // 更新单个 Session 的最新消息展示状态
    private suspend fun updateSessionLastStatus(sessionId: Long, latestMsg: MessageDto) {
        val displayMsg = if (latestMsg.messageType == "AUDIO") "[语音]" else latestMsg.messageInfo
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

    suspend fun uploadIcon(file: File): AppResult<UploadDto> = runSafe {
        val mimeType = detectMime(file)
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        return@runSafe api.uploadIcon(body).unwrap()
    }
}