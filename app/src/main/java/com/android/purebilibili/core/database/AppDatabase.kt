package com.android.purebilibili.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.android.purebilibili.core.database.dao.SearchHistoryDao
import com.android.purebilibili.core.database.entity.SearchHistory

@Database(entities = [SearchHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    // 🔥 性能优化：移除 allowMainThreadQueries，强制使用协程  
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}