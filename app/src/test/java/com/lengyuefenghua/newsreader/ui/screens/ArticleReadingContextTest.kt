package com.lengyuefenghua.newsreader.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleReadingContextTest {

    @Test
    fun `current article in middle should resolve previous and next urls`() {
        val context = ArticleReadingContext(
            articles = listOf(
                readingItem("u1", "t1"),
                readingItem("u2", "t2"),
                readingItem("u3", "t3"),
            ),
            currentUrl = "u2",
        )

        assertEquals("u1", context.previousUrl)
        assertEquals("u3", context.nextUrl)
        assertEquals("t1", context.previousTitle)
        assertEquals("t3", context.nextTitle)
    }

    @Test
    fun `first article should have no previous url`() {
        val context = ArticleReadingContext(
            articles = listOf(
                readingItem("u1", "t1"),
                readingItem("u2", "t2"),
                readingItem("u3", "t3"),
            ),
            currentUrl = "u1",
        )

        assertNull(context.previousUrl)
        assertEquals("u2", context.nextUrl)
    }

    @Test
    fun `last article should have no next url`() {
        val context = ArticleReadingContext(
            articles = listOf(
                readingItem("u1", "t1"),
                readingItem("u2", "t2"),
                readingItem("u3", "t3"),
            ),
            currentUrl = "u3",
        )

        assertEquals("u2", context.previousUrl)
        assertNull(context.nextUrl)
    }

    @Test
    fun `missing current article should expose no neighbors`() {
        val context = ArticleReadingContext(
            articles = listOf(
                readingItem("u1", "t1"),
                readingItem("u2", "t2"),
                readingItem("u3", "t3"),
            ),
            currentUrl = "u4",
        )

        assertNull(context.previousUrl)
        assertNull(context.nextUrl)
    }

    private fun readingItem(url: String, title: String) = ArticleReadingItem(
        url = url,
        title = title,
    )
}
