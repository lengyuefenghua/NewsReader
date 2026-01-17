package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.NewsRepository
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.data.SourceStat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// [新增] 包装类，包含源信息和统计信息
data class SourceWithStat(
    val source: Source,
    val total: Int = 0,
    val read: Int = 0
)

class SourceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as NewsReaderApplication).database
    private val dao = db.sourceDao()
    private val articleDao = db.articleDao() // 需要操作文章表
    private val repository = NewsRepository(db)

    // [修改] 开启 PrettyPrinting
    private val gson = GsonBuilder().setPrettyPrinting().create()

    // [修改] 合并 Source 和 Stat 流
    val sourcesWithStats: StateFlow<List<SourceWithStat>> = combine(
        dao.getAllSources(),
        articleDao.getSourceStatsFlow()
    ) { sources, stats ->
        // 将统计列表转为 Map 方便查找
        val statMap = stats.associateBy { it.sourceName }
        sources.map { source ->
            val stat = statMap[source.name] ?: SourceStat(source.name, 0, 0)
            SourceWithStat(source, stat.totalCount, stat.readCount)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addSource(source: Source) {
        viewModelScope.launch { dao.insert(source) }
    }

    fun deleteSource(source: Source) {
        viewModelScope.launch { dao.delete(source) }
    }

    fun deleteSources(sourcesToDelete: List<Source>) {
        viewModelScope.launch { dao.deleteSources(sourcesToDelete) }
    }

    fun syncSource(sourceId: Int, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            repository.syncSource(sourceId)
            onFinished()
        }
    }

    // [新增] 批量标记已读
    fun markAllAsRead(sourceName: String) {
        viewModelScope.launch {
            articleDao.markSourceAsRead(sourceName)
        }
    }

    // [新增] 批量标记未读
    fun markAllAsUnread(sourceName: String) {
        viewModelScope.launch {
            articleDao.markSourceAsUnread(sourceName)
        }
    }

    fun exportSourceToJson(source: Source): String = gson.toJson(source)

    fun exportSourcesToJson(sources: List<Source>): String = gson.toJson(sources)

    // [核心修复] 在解析前清洗 JSON 字符串
    fun importFromJson(json: String): Int {
        // 1. 替换不换行空格 (\u00a0) 为标准空格
        // 2. 去除首尾空白
        val cleanedJson = json.replace('\u00a0', ' ').trim().trimIndent()
        Log.e("user", cleanedJson)
        return try {
            val validSources = parseSources(cleanedJson)
            if (validSources.isNotEmpty()) {
                viewModelScope.launch {
                    // 导入时重置 ID 为 0，让数据库自动生成新 ID
                    val newSources = validSources.map { it.copy(id = 0) }
                    dao.insertAll(newSources)
                }
                validSources.size
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun parseSources(json: String): List<Source> {
        return try {
            // 尝试解析为列表
            val listType = object : TypeToken<List<Source>>() {}.type
            gson.fromJson<List<Source>>(json, listType) ?: emptyList()
        } catch (_: Exception) {
            Log.d("user", "单条解析")
            try {
                // 尝试解析为单个对象
                val source = gson.fromJson(json, Source::class.java)
                if (source != null) listOf(source) else emptyList()
            } catch (e2: Exception) {
                Log.w("user", "JSON 解析失败", e2)
                emptyList()
            }
        }
    }
}