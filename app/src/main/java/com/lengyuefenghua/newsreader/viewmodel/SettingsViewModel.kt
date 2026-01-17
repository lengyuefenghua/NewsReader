package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = (application as NewsReaderApplication).userPreferencesRepository
    private val articleDao = (application as NewsReaderApplication).database.articleDao()

    val autoUpdate = prefsRepo.autoUpdateFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val cacheLimit = prefsRepo.cacheLimitFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            UserPreferencesRepository.DEFAULT_CACHE_LIMIT
        )

    fun setAutoUpdate(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoUpdate(enabled) }
    }

    fun setCacheLimit(limit: Int) {
        viewModelScope.launch { prefsRepo.setCacheLimit(limit) }
    }

    fun clearCacheNow() {
        viewModelScope.launch {
            val limit = cacheLimit.value
            articleDao.cleanupCache(limit)
            Toast.makeText(getApplication(), "缓存清理完成，保留最新 $limit 条", Toast.LENGTH_SHORT)
                .show()
        }
    }
}