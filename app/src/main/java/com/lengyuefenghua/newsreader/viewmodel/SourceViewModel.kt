package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.Source
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SourceViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as NewsReaderApplication).database.sourceDao()
    private val gson = Gson()

    val sources: StateFlow<List<Source>> = dao.getAllSources()
        .stateIn(
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

    // [新增] 批量删除
    fun deleteSources(sourcesToDelete: List<Source>) {
        viewModelScope.launch { dao.deleteSources(sourcesToDelete) }
    }

    // [新增] 导出单个源为 JSON
    fun exportSourceToJson(source: Source): String {
        return gson.toJson(source)
    }

    // [新增] 导出多个源为 JSON
    fun exportSourcesToJson(sources: List<Source>): String {
        return gson.toJson(sources)
    }

    /**
     * [新增] 从 JSON 导入源
     * 返回导入成功的数量，如果失败返回 -1
     */
    fun importFromJson(json: String): Int {
        return try {
            val validSources = parseSources(json)
            if (validSources.isNotEmpty()) {
                viewModelScope.launch {
                    // 重置 ID 为 0，确保是新增而不是覆盖
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

    // 智能解析：尝试解析为 List<Source> 或 单个 Source
    private fun parseSources(json: String): List<Source> {
        return try {
            // 尝试解析为列表
            val listType = object : TypeToken<List<Source>>() {}.type
            gson.fromJson<List<Source>>(json, listType) ?: emptyList()
        } catch (e: Exception) {
            try {
                // 尝试解析为单个对象
                val source = gson.fromJson(json, Source::class.java)
                if (source != null) listOf(source) else emptyList()
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }
}