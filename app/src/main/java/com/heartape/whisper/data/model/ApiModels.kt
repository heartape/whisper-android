package com.heartape.whisper.data.model

data class ApiResponse<T>(val code: Int, val data: T)

data class LoginResponse(val Authorization: String)
data class UserDto(val id: Long, val phone: String, val username: String, val avatar: String, val bio: String, val role: String)
data class UserSearchDto(val id: Long, val username: String, val avatar: String, val bio: String)
data class SessionDto(val id: Long, val name: String?, val type: String, val icon: String?)
data class ApplyDto(val id: Long, val type: String, val sessionId: Long, val applicantId: Long, val applyInfo: String, val status: String)
data class MessageDto(val id: Long, val sessionId: Long, val userId: Long, val messageType: String, val messageInfo: String, val createTime: Long)
data class AnnouncementDto(val content: String, val userId: Long, val publishTime: Long)
data class MemberDto(val userId: Long, val username: String, val aliasName: String?, val avatar: String, val role: String, val isBlock: Boolean, val joinTime: Long)
data class ContactDto(val id: Long, val username: String, val avatar: String)

data class UploadDto(val path: String, val url: String)
data class VersionDto(
    val versionCode: Int,         // 用于对比本地版本号，例如 2
    val versionName: String,      // 用于展示，例如 "V1.0.1"
    val releaseNotes: String,     // 更新日志
    val downloadUrl: String,      // APK 下载地址
    val isForceUpdate: Boolean    // 是否强制更新
)

// 请求体定义
data class LoginReq(val phone: String, val code: String)
data class RegisterReq(val phone: String, val code: String, val username: String, val password: String, val avatar: String)
data class CreateGroupReq(val name: String, val icon: String)
data class AliasReq(val sessionId: Long, val aliasName: String)
data class ApplyPeerReq(val reviewerId: Long, val applyInfo: String, val aliasName: String? = null)
data class ApplyGroupReq(val sessionId: Long, val applyInfo: String)
data class ReviewReq(val applyId: Long, val approved: Boolean, val reviewNote: String, val aliasName: String? = null)
data class AnnounceReq(val content: String)
data class ExitReq(val sessionId: Long)
data class ManageMemberReq(
    val sessionId: Long,
    val userId: Long, // 私聊或群聊踢人时传 userId；群聊修改自己的 alias 时传 null
    val action: String,
)
data class UpdateUsernameReq(val username: String)
data class UpdateAvatarReq(val avatar: String)
data class UpdateBioReq(val bio: String)
data class UpdatePhoneReq(val phone: String)
data class UpdatePasswordReq(val password: String)