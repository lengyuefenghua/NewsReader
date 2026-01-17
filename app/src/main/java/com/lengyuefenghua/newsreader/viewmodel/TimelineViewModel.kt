package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.data.NewsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    // [新增] 源过滤器 (null 表示显示所有)
    private val _sourceFilter = MutableStateFlow<String?>(null)
    // 公开当前选中的源名称，用于 UI 标题显示
    val currentSourceName = _sourceFilter.asStateFlow()
    // [新增] 记录当前查看的源 ID，用于刷新时区分
    private var currentSourceId: Int? = null
    // [新增] 同步状态流
    private val _syncState = MutableStateFlow(SyncState())
    val syncState = _syncState.asStateFlow()

    // [新增] 单次事件流 (用于 Toast)
    private val _toastEvent = Channel<String>()
    val toastEvent = _toastEvent.receiveAsFlow()

    // [核心修改] 使用 flatMapLatest 动态切换数据源
    @OptIn(ExperimentalCoroutinesApi::class)
    val articles: StateFlow<List<Article>> = _sourceFilter
        .flatMapLatest { sourceName ->
            if (sourceName == null) {
                // 如果过滤器为空，订阅所有文章
                repository.allArticles
            } else {
                // 如果有源名称，只订阅该源的文章 (数据库级过滤)
                repository.getArticlesBySource(sourceName)
            }
        }
        .combine(_filterState) { list, filter ->
            // 这里只处理 已读/未读 过滤
            when (filter) {
                FilterType.ALL -> list
                FilterType.UNREAD -> list.filter { !it.isRead }
                FilterType.READ -> list.filter { it.isRead }
            }
        }
        .stateIn(
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

            val targetSourceId = currentSourceId
            val targetSourceName = _sourceFilter.value

            if (targetSourceId != null && targetSourceName != null) {
                // === 单源刷新模式 ===
                _syncState.value = SyncState(isSyncing = true, current = 0, total = 1, currentSource = targetSourceName)

                // 执行单源更新
                repository.syncSource(targetSourceId)

                _syncState.value = SyncState(isSyncing = false)
                _toastEvent.send("更新完成")

            } else {
                // === 全局刷新模式 ===
                _syncState.value = SyncState(isSyncing = true, current = 0, total = 0, currentSource = "准备中...")
                repository.syncAll { current, total, name ->
                    _syncState.update {
                        it.copy(current = current, total = total, currentSource = name)
                    }
                }
                _syncState.value = SyncState(isSyncing = false)
                _toastEvent.send("全部更新完成")
            }
        }
    }

    fun getArticleByUrl(url: String): Article? {
        return articles.value.find { it.url == url }
    }
    // [新增] 设置只显示特定源的文章
    fun showSource(sourceId: Int) {
        viewModelScope.launch {
            val source = repository.getSourceById(sourceId)
            if (source != null) {
                currentSourceId = sourceId // [新增] 记录 ID
                _sourceFilter.value = source.name
            } else {
                _toastEvent.send("找不到该订阅源")
                _sourceFilter.value = null
            }
        }
    }
    // [新增] 重置源过滤器 (显示所有)
    fun resetSourceFilter() {
        _sourceFilter.value = null
    }
    fun setFilter(type: FilterType) {
        currentSourceId = null // [新增] 清空 ID
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