package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.data.NewsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FilterType {
    ALL, UNREAD, READ
}

// [新增] 同步状态数据类
data class SyncState(
    val isSyncing: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val currentSource: String = ""
)

class TimelineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NewsRepository((application as NewsReaderApplication).database)

    private val _filterState = MutableStateFlow(FilterType.ALL)
    val filterState = _filterState.asStateFlow()

    // [新增] 同步状态流
    private val _syncState = MutableStateFlow(SyncState())
    val syncState = _syncState.asStateFlow()

    // [新增] 单次事件流 (用于 Toast)
    private val _toastEvent = Channel<String>()
    val toastEvent = _toastEvent.receiveAsFlow()

    val articles: StateFlow<List<Article>> = combine(
        repository.allArticles,
        _filterState
    ) { allArticles, filter ->
        when (filter) {
            FilterType.ALL -> allArticles
            FilterType.UNREAD -> allArticles.filter { !it.isRead }
            FilterType.READ -> allArticles.filter { it.isRead }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun fetchArticles() {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (_syncState.value.isSyncing) return@launch

            // 初始化状态
            _syncState.value = SyncState(isSyncing = true, current = 0, total = 0, currentSource = "准备中...")

            repository.syncAll { current, total, name ->
                // 更新进度状态
                _syncState.update {
                    it.copy(current = current, total = total, currentSource = name)
                }
            }

            // 完成
            _syncState.value = SyncState(isSyncing = false)
            _toastEvent.send("更新完成")
        }
    }

    fun getArticleByUrl(url: String): Article? {
        return articles.value.find { it.url == url }
    }

    fun setFilter(type: FilterType) {
        _filterState.value = type
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markArticleAsRead(id)
        }
    }

    fun toggleFavorite(article: Article) {
        viewModelScope.launch {
            repository.toggleFavorite(article.id, article.isFavorite)
        }
    }

    fun findSourceIdAndEdit(sourceName: String, onFound: (Int) -> Unit) {
        viewModelScope.launch {
            val id = repository.getSourceIdByName(sourceName)
            if (id != null) {
                onFound(id)
            }
        }
    }
}