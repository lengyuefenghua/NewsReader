package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.*
import com.lengyuefenghua.newsreader.data.UserPreferencesRepository
import com.lengyuefenghua.newsreader.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = (application as NewsReaderApplication).userPreferencesRepository
    private val settingsManager = (application as NewsReaderApplication).settingsManager
    private val articleDao = (application as NewsReaderApplication).database.articleDao()
    private val sourceDao = (application as NewsReaderApplication).database.sourceDao()
    private val gson = Gson()

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

    // ========== 数据备份与恢复功能 ==========

    /**
     * 创建完整数据备份
     */
    suspend fun createBackup(): BackupData = withContext(Dispatchers.IO) {
        val sources = sourceDao.getAllSourcesList()
        val articles = articleDao.getAllArticles()
        val autoUpdate = prefsRepo.autoUpdateFlow.first()
        val cacheLimit = prefsRepo.cacheLimitFlow.first()
        val defaultFilterType = settingsManager.getDefaultFilterType().name
        val concurrentCount = settingsManager.getConcurrentCount()
        val sourceTimeoutSeconds = settingsManager.getSourceTimeoutSeconds()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val backupDate = dateFormat.format(Date())

        BackupData(
            version = "2.2", // 更新版本号以支持新字段
            backupDate = backupDate,
            databaseVersion = 11, // 当前数据库版本
            sources = sources,
            articles = articles,
            settings = BackupSettings(
                autoUpdate = autoUpdate,
                cacheLimit = cacheLimit,
                defaultFilterType = defaultFilterType,
                concurrentCount = concurrentCount,
                sourceTimeoutSeconds = sourceTimeoutSeconds
            )
        )
    }

    /**
     * 保存备份到文件
     */
    suspend fun saveBackupToFile(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
        try {
            val backupData = createBackup()
            val json = gson.toJson(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: return@withContext "无法写入文件"

            val sourceCount = backupData.sources.size
            val articleCount = backupData.articles.size

            "备份成功：$sourceCount 个订阅源，$articleCount 篇文章"
        } catch (e: Exception) {
            "备份失败：${e.message}"
        }
    }

    /**
     * 从文件加载备份数据
     */
    suspend fun loadBackupFromFile(uri: Uri, context: Context): BackupData? = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                ?: return@withContext null

            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 恢复备份数据（全部覆盖策略）
     */
    suspend fun restoreBackup(backupData: BackupData): String = withContext(Dispatchers.IO) {
        try {
            // 1. 清除现有数据
            sourceDao.deleteAll()
            articleDao.deleteAllArticles()

            // 2. 插入订阅源（需要重置 ID 以避免冲突）
            backupData.sources.forEach { source ->
                sourceDao.insert(source.copy(id = 0))
            }

            // 3. 插入文章
            backupData.articles.forEach { article ->
                articleDao.insert(article)
            }

            // 4. 恢复用户设置
            prefsRepo.setAutoUpdate(backupData.settings.autoUpdate)
            prefsRepo.setCacheLimit(backupData.settings.cacheLimit)

            // 5. 恢复 v0.0.3 新增的设置（兼容旧版本备份）
            try {
                val filterType = when (backupData.settings.defaultFilterType) {
                    "ALL" -> com.lengyuefenghua.newsreader.ui.common.FilterType.ALL
                    "READ" -> com.lengyuefenghua.newsreader.ui.common.FilterType.READ
                    else -> com.lengyuefenghua.newsreader.ui.common.FilterType.UNREAD
                }
                settingsManager.setDefaultFilterType(filterType)
            } catch (e: Exception) {
                // 忽略错误，使用默认值
            }

            // 6. 恢复 v0.0.4 新增的设置（兼容旧版本备份）
            try {
                val count = backupData.settings.concurrentCount
                if (count in 1..5) {
                    settingsManager.setConcurrentCount(count)
                }
            } catch (e: Exception) {
                // 忽略错误，使用默认值
            }

            try {
                settingsManager.setSourceTimeoutSeconds(backupData.settings.sourceTimeoutSeconds)
            } catch (e: Exception) {
                // 忽略错误，使用默认值
            }

            val sourceCount = backupData.sources.size
            val articleCount = backupData.articles.size

            "恢复成功：$sourceCount 个订阅源，$articleCount 篇文章"
        } catch (e: Exception) {
            "恢复失败：${e.message}"
        }
    }
}
