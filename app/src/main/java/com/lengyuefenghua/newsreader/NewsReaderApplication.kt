package com.lengyuefenghua.newsreader

import android.app.Application
import com.lengyuefenghua.newsreader.data.AppDatabase
import com.lengyuefenghua.newsreader.data.UserPreferencesRepository
import com.lengyuefenghua.newsreader.utils.WebViewManager

class NewsReaderApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    // [新增]
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        WebViewManager.init(this)
        // [新增]
        userPreferencesRepository = UserPreferencesRepository(this)
    }
}