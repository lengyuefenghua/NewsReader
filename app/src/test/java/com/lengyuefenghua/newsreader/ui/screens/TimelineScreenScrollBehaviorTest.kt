package com.lengyuefenghua.newsreader.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineScreenScrollBehaviorTest {

    @Test
    fun `should reveal new top article when list was anchored on previous first item during sync`() {
        val shouldReveal = shouldRevealNewTopArticle(
            previousFirstArticleId = "article-1",
            currentFirstArticleId = "article-2",
            visibleArticleKey = "article-1",
            firstVisibleItemScrollOffset = 0,
            isSyncing = true
        )

        assertTrue(shouldReveal)
    }

    @Test
    fun `should not reveal when user is reading further down the list`() {
        val shouldReveal = shouldRevealNewTopArticle(
            previousFirstArticleId = "article-1",
            currentFirstArticleId = "article-2",
            visibleArticleKey = "article-9",
            firstVisibleItemScrollOffset = 0,
            isSyncing = true
        )

        assertFalse(shouldReveal)
    }

    @Test
    fun `should not reveal when refresh is not running`() {
        val shouldReveal = shouldRevealNewTopArticle(
            previousFirstArticleId = "article-1",
            currentFirstArticleId = "article-2",
            visibleArticleKey = "article-1",
            firstVisibleItemScrollOffset = 0,
            isSyncing = false
        )

        assertFalse(shouldReveal)
    }
}
