package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.data.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimelineViewModel(application: Application) : AndroidViewModel(application) {

    // 简单构建 Repository，实际项目中可以用 Hilt/Koin 注入
    private val repository = NewsRepository((application as NewsReaderApplication).database)

    // 直接观察仓库的数据流，转换为 StateFlow 供 Compose 使用
    // SharingStarted.Lazily 确保只有 UI 显示时才订阅数据库
    val articles: StateFlow<List<Article>> = repository.allArticles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // 启动时可以尝试自动同步一次，或者完全依赖手动刷新
        // refresh()
    }

    fun fetchArticles() {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (_isLoading.value) return@launch

            _isLoading.value = true
            // 执行后台同步任务，任务完成后，articles 流会自动更新，UI 也会自动刷新
            repository.syncAll()
            _isLoading.value = false
        }
    }

    // 从当前内存列表中查找文章（如果还需要的话）
    fun getArticleByUrl(url: String): Article? {
        return articles.value.find { it.url == url }
    }
}