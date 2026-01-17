package com.lengyuefenghua.newsreader

import android.app.Application
import com.lengyuefenghua.newsreader.data.AppDatabase
import com.lengyuefenghua.newsreader.utils.WebViewManager

class NewsReaderApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // [新增] 初始化全局 WebView
        WebViewManager.init(this)
    }
}