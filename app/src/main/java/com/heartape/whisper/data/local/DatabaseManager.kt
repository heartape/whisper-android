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
    fun getDatabase(): AppDatabase {
        val userId = prefsManager.currentUserProfile.id

        // 如果当前已经打开了该用户的数据库，直接返回复用
        if (currentDb != null && currentUserId == userId) {
            return currentDb!!
        }

        // 如果是切换了账号，先关闭旧的数据库连接
        currentDb?.close()

        // 为该用户创建专属的物理隔离数据库文件
        currentUserId = userId
        currentDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "whisper_db_user_$userId" // ★ 核心：根据 userId 动态命名数据库文件
        )
            .fallbackToDestructiveMigration()
            .build()

        return currentDb!!
    }

    val sessionDao: SessionDao
        get() = getDatabase().sessionDao()

    @Synchronized
    fun closeDatabase() {
        currentDb?.close()
        currentDb = null
        currentUserId = 0L
    }
}