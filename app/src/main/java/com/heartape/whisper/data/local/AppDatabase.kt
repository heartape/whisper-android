package com.heartape.whisper.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val type: String,
    val icon: String,
    val lastMessage: String = "",
    val lastTime: Long = 0L,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: Long,
    val sessionId: Long,
    val userId: Long,
    val messageType: String,
    val messageInfo: String,
    val createTime: Long
)

@Entity(tableName = "members", primaryKeys = ["sessionId", "userId"])
data class MemberEntity(
    val sessionId: Long,
    val userId: Long,
    val username: String,
    val aliasName: String?,
    val avatar: String,
    val role: String,
    val isBlock: Boolean,
    val joinTime: Long
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val userId: Long,
    val username: String,
    val avatar: String
)

/**
 * 为保证离线可读，应该保存session、message和member到本地。
 */
@Dao
interface SessionDao {
    // ================= Session CRUD =================
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionFlow(sessionId: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: Long): SessionEntity?

    // 仅加载最近活跃的 100 个 Session 以控制内存
    @Query("SELECT * FROM sessions ORDER BY lastTime DESC LIMIT 100")
    fun getActiveSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessionsOnce(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Query("UPDATE sessions SET lastMessage = :msg, lastTime = :time WHERE id = :sessionId")
    suspend fun updateSessionLastMessage(sessionId: Long, msg: String, time: Long)

    @Query("DELETE FROM sessions WHERE id IN (:ids)")
    suspend fun deleteSessionsByIds(ids: List<Long>)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    // ================= Message CRUD =================
    // 内存优化：通过子查询仅拉取该会话最新活跃的 50 条消息，并在外层按时间正序排列给 UI
    @Query("SELECT * FROM (SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY id DESC LIMIT :limit) ORDER BY id ASC")
    fun getActiveMessagesFlow(sessionId: Long, limit: Int): Flow<List<MessageEntity>>

    @Query("SELECT MAX(id) FROM messages WHERE sessionId = :sessionId")
    suspend fun getLastSyncMessageId(sessionId: Long): Long?

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY id DESC LIMIT 1")
    suspend fun getLatestMessageOnce(sessionId: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: Long)

    // ================= Member CRUD =================
    @Query("SELECT * FROM members WHERE sessionId = :sessionId ORDER BY role ASC, joinTime ASC")
    fun getMembersFlow(sessionId: Long): Flow<List<MemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Query("DELETE FROM members WHERE sessionId = :sessionId")
    suspend fun deleteMembersBySession(sessionId: Long)

    // ================= Contact CRUD =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("SELECT * FROM contacts WHERE userId = :userId LIMIT 1")
    suspend fun getContact(userId: Long): ContactEntity

    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsOnce(): List<ContactEntity>
}

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
        MemberEntity::class,
        ContactEntity::class
               ],
    version = 1,
    exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}