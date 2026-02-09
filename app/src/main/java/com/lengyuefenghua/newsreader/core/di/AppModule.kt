package com.lengyuefenghua.newsreader.core.di

import android.app.Application
import com.lengyuefenghua.newsreader.data.AppDatabase
import com.lengyuefenghua.newsreader.data.NewsRepository
import com.lengyuefenghua.newsreader.data.UserPreferencesRepository
import com.lengyuefenghua.newsreader.util.SettingsManager
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    // Application
    single { androidApplication() }

    // Database
    single { AppDatabase.getDatabase(get()) }

    // DAOs
    single { get<AppDatabase>().articleDao() }
    single { get<AppDatabase>().sourceDao() }

    // Repositories
    single { NewsRepository(get()) }
    single { UserPreferencesRepository(get()) }

    // Settings
    single { SettingsManager(get()) }

    // OkHttpClient
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
