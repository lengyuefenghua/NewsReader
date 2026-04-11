package com.lengyuefenghua.newsreader.viewmodel

import androidx.lifecycle.ViewModel
import com.lengyuefenghua.newsreader.data.Article
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FeedPreview(
    val displayTitle: String,
    val url: String,
    val articles: List<Article>,
    val iconUrl: String?
)

class FeedPreviewViewModel : ViewModel() {

    private val _preview = MutableStateFlow<FeedPreview?>(null)
    val preview: StateFlow<FeedPreview?> = _preview.asStateFlow()

    fun setPreview(
        title: String?,
        url: String,
        articles: List<Article>,
        iconUrl: String?
    ) {
        val displayTitle = resolveDisplayTitle(title, url)
        _preview.value = FeedPreview(
            displayTitle = displayTitle,
            url = url,
            articles = articles.map { it.copy(sourceName = displayTitle) },
            iconUrl = iconUrl
        )
    }

    fun clearPreview() {
        _preview.value = null
    }

    fun hasPreview(): Boolean = _preview.value != null

    private fun resolveDisplayTitle(title: String?, url: String): String {
        val trimmedTitle = title?.trim().orEmpty()
        if (trimmedTitle.isNotEmpty()) return trimmedTitle

        return runCatching { URI(url).host.orEmpty().removePrefix("www.") }
            .getOrDefault("")
            .ifBlank { url }
    }
}
