package com.heartape.whisper.data.local

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsManager: PrefsManager
) {
    private var currentUserId: Long = 0L
    private var currentDb: AppDatabase? = null

    @Synchronized
    fun openDatabase(userId: Long) {
        if (userId == 0L) return

        // 如果当前已经打开了该用户的库，直接返回
        if (currentDb != null && currentUserId == userId) return

        // 关闭旧库，开启新库
        currentDb?.close()
        currentUserId = userId
        currentDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "whisper_db_user_$userId"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    val sessionDao: SessionDao get() = getSafeDb().sessionDao()
//    val messageDao: MessageDao? get() = getDatabase()?.messageDao()
//    val memberDao: MemberDao? get() = getDatabase()?.memberDao()

    private fun getSafeDb(): AppDatabase {
        if (currentDb == null) {
            val userId = prefsManager.requireUserId()
            openDatabase(userId)
        }
        return currentDb!!
    }

    @Synchronized
    fun closeDatabase() {
        currentDb?.close()
        currentDb = null
        currentUserId = 0L
    }
}