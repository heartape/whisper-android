package com.heartape.whisper.data.remote

import com.heartape.whisper.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {
    @POST("/login") suspend fun login(@Body req: LoginReq): ApiResponse<LoginResponse>
    @POST("/register") suspend fun register(@Body req: RegisterReq): ApiResponse<Any>
    @GET("/user") suspend fun getCurrentUser(): ApiResponse<UserDto>
    @GET("/user/{id}") suspend fun getUser(@Path("id") id: Long): ApiResponse<UserDto>
    @GET("/user/find") suspend fun findUser(@Query("keyword") keyword: String): ApiResponse<List<UserSearchDto>>
    @GET("/session/contact")
    suspend fun getContacts(): ApiResponse<List<ContactDto>>
    @GET("/session/{sessionId}") suspend fun getSession(@Path("sessionId") sessionId: Long): ApiResponse<SessionDto>
    @GET("/session") suspend fun getSessions(): ApiResponse<List<SessionDto>>
    @POST("/session/group") suspend fun createGroup(@Body req: CreateGroupReq): ApiResponse<Long>
    @PUT("/session/alias") suspend fun updateAlias(@Body req: AliasReq): ApiResponse<MemberDto>
    @POST("/session/peer/apply") suspend fun applyFriend(@Body req: ApplyPeerReq): ApiResponse<Any>
    @POST("/session/group/apply") suspend fun applyGroup(@Body req: ApplyGroupReq): ApiResponse<Any>
    @GET("/session/apply/pending") suspend fun getPendingApplies(): ApiResponse<List<ApplyDto>>
    @POST("/session/apply/review") suspend fun reviewApply(@Body req: ReviewReq): ApiResponse<Any>
    @GET("/session/{sessionId}/announcement") suspend fun getAnnouncements(@Path("sessionId") sessionId: Long): ApiResponse<AnnouncementDto>
    @POST("/session/{sessionId}/announcement") suspend fun publishAnnouncement(@Path("sessionId") sessionId: Long, @Body req: AnnounceReq): ApiResponse<Any>

    @GET("/session/{sessionId}/member/{userId}")
    suspend fun getMember(@Path("sessionId") sessionId: Long, @Path("userId") userId: Long): ApiResponse<MemberDto>
    @GET("/session/{sessionId}/members") suspend fun getMembers(@Path("sessionId") sessionId: Long): ApiResponse<List<MemberDto>>
    @DELETE("/session/exit") suspend fun exit(@Body req: ExitReq): ApiResponse<Any>
    @PUT("/session/peer/member/manage")
    suspend fun managePeerMember(@Body req: ManageMemberReq): ApiResponse<Any>

    @PUT("/session/group/member/manage")
    suspend fun manageGroupMember(@Body req: ManageMemberReq): ApiResponse<Any>

    // 假设提供文件上传接口用于语音和图片发送
    @Multipart @POST("/upload/avatar") suspend fun uploadAvatar(@Part file: MultipartBody.Part): ApiResponse<UploadDto>
    @Multipart @POST("/upload/avatar") suspend fun uploadIcon(@Part file: MultipartBody.Part): ApiResponse<UploadDto>
    @PUT("/user/password") suspend fun updatePassword(@Body req: UpdatePasswordReq): ApiResponse<Any>

    // 历史消息增加时间线参数 since（如果 since 为空，则后端返回所有）
    @GET("/session/{sessionId}/messages")
    suspend fun getHistoryMessages(
        @Path("sessionId") sessionId: Long,
        @Query("count") count: Int = 20,
        @Query("since") since: Long? = null,
        @Query("before") before: Long? = null
    ): ApiResponse<List<MessageDto>>

    // 搜索群聊 (基于 API 文档)
    @GET("/session/find")
    suspend fun findGroup(
        @Query("keyword") keyword: String,
        @Query("type") type: String = "GROUP"
    ): ApiResponse<List<SessionDto>>

    @PUT("/user/username") suspend fun updateUsername(@Body req: UpdateUsernameReq): ApiResponse<Any>
    @PUT("/user/avatar") suspend fun updateAvatar(@Body req: UpdateAvatarReq): ApiResponse<Any>
    @PUT("/user/bio") suspend fun updateBio(@Body req: UpdateBioReq): ApiResponse<Any>
    @PUT("/user/phone") suspend fun updatePhone(@Body req: UpdatePhoneReq): ApiResponse<Any>
    @GET("/system/version/latest")
    suspend fun checkUpdate(@Query("platform") platform: String = "ANDROID"): ApiResponse<VersionDto>
}