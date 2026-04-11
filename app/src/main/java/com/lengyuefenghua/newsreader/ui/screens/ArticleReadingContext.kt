package com.lengyuefenghua.newsreader.ui.screens

data class ArticleReadingItem(
    val url: String,
    val title: String,
)

data class ArticleReadingContext(
    val articles: List<ArticleReadingItem>,
    val currentUrl: String,
) {
    val articleUrls: List<String>
        get() = articles.map { it.url }

    val currentIndex: Int = articleUrls.indexOf(currentUrl)

    val previousUrl: String?
        get() = previousItem?.url

    val previousTitle: String?
        get() = previousItem?.title

    val nextUrl: String?
        get() = nextItem?.url

    val nextTitle: String?
        get() = nextItem?.title

    private val previousItem: ArticleReadingItem?
        get() = if (currentIndex <= 0) null else articles.getOrNull(currentIndex - 1)

    private val nextItem: ArticleReadingItem?
        get() = if (currentIndex < 0) null else articles.getOrNull(currentIndex + 1)
}
