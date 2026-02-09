package com.lengyuefenghua.newsreader

import android.app.Application
import com.lengyuefenghua.newsreader.core.di.appModule
import com.lengyuefenghua.newsreader.core.di.viewModelModule
import com.lengyuefenghua.newsreader.core.error.GlobalExceptionHandler
import com.lengyuefenghua.newsreader.data.AppDatabase
import com.lengyuefenghua.newsreader.data.UserPreferencesRepository
import com.lengyuefenghua.newsreader.utils.WebViewManager
import com.lengyuefenghua.newsreader.util.SettingsManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NewsReaderApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    // [新增]
    lateinit var userPreferencesRepository: UserPreferencesRepository
    lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()

        // 注册全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(this)
        )

        // 初始化 Koin
        startKoin {
            androidContext(this@NewsReaderApplication)
            modules(appModule + viewModelModule)
        }

        WebViewManager.init(this)
        // [新增]
        userPreferencesRepository = UserPreferencesRepository(this)
        settingsManager = SettingsManager(this)
    }
}