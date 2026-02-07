package com.lengyuefenghua.newsreader.core.error

import android.content.Context
import android.util.Log
import kotlin.system.exitProcess

class GlobalExceptionHandler(private val context: Context) :
    Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e("GlobalExceptionHandler", "Uncaught exception", throwable)

        // 可选：上传崩溃报告到 Firebase Crashlytics

        // 调用默认处理器
        defaultHandler?.uncaughtException(thread, throwable) ?: run {
            exitProcess(1)
        }
    }
}
