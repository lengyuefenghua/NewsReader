package com.lengyuefenghua.newsreader.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtils {
    private const val TAG = "ParserDebug"

    fun log(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        Log.d(TAG, "[$timestamp] $message")
    }
}
