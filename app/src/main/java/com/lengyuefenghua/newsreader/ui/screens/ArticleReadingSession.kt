package com.lengyuefenghua.newsreader.ui.screens

object ArticleReadingSession {
    var current: ArticleReadingContext? = null
        private set

    fun open(context: ArticleReadingContext) {
        current = context
    }

    fun moveTo(url: String) {
        val existing = current ?: return
        current = existing.copy(currentUrl = url)
    }

    fun clear() {
        current = null
    }
}
