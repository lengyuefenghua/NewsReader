package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.Source
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 这里继承 AndroidViewModel 是为了能拿到 Application，从而获取数据库实例
class SourceViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as NewsReaderApplication).database.sourceDao()

    // 将数据库的 Flow 转换为 Compose 可以直接使用的 StateFlow
    // 这样当数据库变化时，UI 会自动刷新
    val sources: StateFlow<List<Source>> = dao.getAllSources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 添加订阅源
    fun addSource(source: Source) {
        viewModelScope.launch {
            dao.insert(source)
        }
    }

    // 删除订阅源
    fun deleteSource(source: Source) {
        viewModelScope.launch {
            dao.delete(source)
        }
    }
}