package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.Article
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val articleDao = (application as NewsReaderApplication).database.articleDao()

    // [新增] 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteArticles: StateFlow<List<Article>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                articleDao.getFavoriteArticlesFlow()
            } else {
                articleDao.searchFavoriteArticles(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // [新增] 从收藏中移除（实际上是取消收藏）
    fun removeFavorites(ids: List<String>) {
        viewModelScope.launch {
            articleDao.removeFavorites(ids)
        }
    }
}