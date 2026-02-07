package com.lengyuefenghua.newsreader.ui.common

import com.lengyuefenghua.newsreader.core.domain.model.NetworkError
import com.lengyuefenghua.newsreader.data.Article

// 筛选类型
enum class FilterType {
    ALL, UNREAD, READ
}

// 同步状态
data class SyncState(
    val isSyncing: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val currentSource: String = ""
)

// Timeline 页面统一状态
data class TimelineUiState(
    val articles: List<Article> = emptyList(),
    val sourceIcons: Map<String, String> = emptyMap(),
    val syncState: SyncState = SyncState(),
    val filterType: FilterType = FilterType.ALL,
    val sourceFilter: String? = null, // null 表示显示所有源
    val isLoading: Boolean = false,
    val error: NetworkError? = null
)
