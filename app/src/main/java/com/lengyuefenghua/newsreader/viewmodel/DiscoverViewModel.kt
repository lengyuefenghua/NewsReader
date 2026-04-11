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

    fun loadPlinkFeeds(forceRefresh: Boolean = false) {
        val currentState = _plinkState.value
        if (currentState.isLoading) return
        if (!forceRefresh && currentState.groups.isNotEmpty() && currentState.error == null) return

        viewModelScope.launch {
            _plinkState.value = currentState.copy(isLoading = true, error = null)
            try {
                val groups = repository.fetchPlinkFeedGroups()
                _plinkState.value = DiscoverUiState(groups = groups)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _plinkState.value = currentState.copy(
                    isLoading = false,
                    error = e.message ?: "加载 Plink 订阅市场失败"
                )
            }
        }
    }
}
