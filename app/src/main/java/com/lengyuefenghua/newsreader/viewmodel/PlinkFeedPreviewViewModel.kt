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

data class PlinkFeedPreviewUiState(
    val isLoading: Boolean = false,
    val preview: FeedPreview? = null,
    val error: String? = null
)

class PlinkFeedPreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NewsReaderApplication
    private val repository = NewsRepository(app.database, app.settingsManager)

    private val _uiState = MutableStateFlow(PlinkFeedPreviewUiState())
    val uiState: StateFlow<PlinkFeedPreviewUiState> = _uiState.asStateFlow()

    fun loadPreview(title: String, url: String, forceRefresh: Boolean = false) {
        val currentState = _uiState.value
        if (currentState.isLoading) return
        if (!forceRefresh && currentState.preview?.url == url && currentState.error == null) return

        viewModelScope.launch {
            _uiState.value = PlinkFeedPreviewUiState(isLoading = true)
            try {
                val result = repository.fetchFeedPreview(url)
                val displayTitle = resolveDisplayTitle(title, result.title, url)
                _uiState.value = PlinkFeedPreviewUiState(
                    preview = FeedPreview(
                        displayTitle = displayTitle,
                        url = url,
                        articles = result.articles.map { it.copy(sourceName = displayTitle) },
                        iconUrl = result.iconUrl
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = PlinkFeedPreviewUiState(
                    error = e.message ?: "获取订阅源预览失败"
                )
            }
        }
    }

    fun clearPreview() {
        _uiState.value = PlinkFeedPreviewUiState()
    }

    private fun resolveDisplayTitle(routeTitle: String, previewTitle: String?, url: String): String {
        val candidates = listOf(routeTitle, previewTitle.orEmpty())
        return candidates.firstOrNull { it.isNotBlank() }
            ?: runCatching { java.net.URI(url).host.orEmpty().removePrefix("www.") }
                .getOrDefault("")
                .ifBlank { url }
    }
}
