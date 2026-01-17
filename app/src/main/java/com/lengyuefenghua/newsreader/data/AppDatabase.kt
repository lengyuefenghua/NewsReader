package com.lengyuefenghua.newsreader.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Source::class, Article::class], exportSchema = false, version = 8)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun articleDao(): ArticleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "news_reader_db"
                )
                    .fallbackToDestructiveMigration() // 版本升级时会清空数据
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}