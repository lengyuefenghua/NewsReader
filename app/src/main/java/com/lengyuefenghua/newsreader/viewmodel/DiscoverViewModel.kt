package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.NewsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val isLoading: Boolean = false,
    val groups: List<NewsRepository.FeedCatalogGroup> = emptyList(),
    val error: String? = null
)

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NewsReaderApplication
    private val repository = NewsRepository(app.database, app.settingsManager)

    private val _plinkState = MutableStateFlow(DiscoverUiState())
    val plinkState: StateFlow<DiscoverUiState> = _plinkState.asStateFlow()

    private val _awesomeRssHubState = MutableStateFlow(DiscoverUiState())
    val awesomeRssHubState: StateFlow<DiscoverUiState> = _awesomeRssHubState.asStateFlow()

    private val _topRssListState = MutableStateFlow(DiscoverUiState())
    val topRssListState: StateFlow<DiscoverUiState> = _topRssListState.asStateFlow()

    private val _wechat2RssState = MutableStateFlow(DiscoverUiState())
    val wechat2RssState: StateFlow<DiscoverUiState> = _wechat2RssState.asStateFlow()

    fun loadPlinkFeeds(forceRefresh: Boolean = false) {
        loadCatalog(
            stateFlow = _plinkState,
            forceRefresh = forceRefresh,
            loader = repository::fetchPlinkFeedGroups,
            defaultErrorMessage = "加载 Plink 订阅市场失败"
        )
    }

    fun loadAwesomeRssHubFeeds(forceRefresh: Boolean = false) {
        loadCatalog(
            stateFlow = _awesomeRssHubState,
            forceRefresh = forceRefresh,
            loader = repository::fetchAwesomeRssHubFeedGroups,
            defaultErrorMessage = "加载 Awesome RSSHub Routes 订阅市场失败"
        )
    }

    fun loadTopRssListFeeds(forceRefresh: Boolean = false) {
        loadCatalog(
            stateFlow = _topRssListState,
            forceRefresh = forceRefresh,
            loader = repository::fetchTopRssListFeedGroups,
            defaultErrorMessage = "加载 Top RSS List 订阅市场失败"
        )
    }

    fun loadWechat2RssFeeds(forceRefresh: Boolean = false) {
        loadCatalog(
            stateFlow = _wechat2RssState,
            forceRefresh = forceRefresh,
            loader = repository::fetchWechat2RssFeedGroups,
            defaultErrorMessage = "加载 Wechat2RSS 订阅市场失败"
        )
    }

    private fun loadCatalog(
        stateFlow: MutableStateFlow<DiscoverUiState>,
        forceRefresh: Boolean,
        loader: suspend () -> List<NewsRepository.FeedCatalogGroup>,
        defaultErrorMessage: String
    ) {
        val currentState = stateFlow.value
        if (currentState.isLoading) return
        if (!forceRefresh && currentState.groups.isNotEmpty() && currentState.error == null) return

        viewModelScope.launch {
            stateFlow.value = currentState.copy(isLoading = true, error = null)
            try {
                val groups = loader()
                stateFlow.value = DiscoverUiState(groups = groups)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                stateFlow.value = currentState.copy(
                    isLoading = false,
                    error = e.message ?: defaultErrorMessage
                )
            }
        }
    }
}
