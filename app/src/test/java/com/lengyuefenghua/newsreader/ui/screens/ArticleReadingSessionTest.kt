package com.lengyuefenghua.newsreader.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleReadingSessionTest {

    @Test
    fun `open should store reading context`() {
        val context = ArticleReadingContext(
            articles = listOf(
                readingItem("u1", "t1"),
                readingItem("u2", "t2"),
                readingItem("u3", "t3"),
            ),
            currentUrl = "u2",
        )

        ArticleReadingSession.clear()
        ArticleReadingSession.open(context)

        assertEquals(context, ArticleReadingSession.current)
    }

    @Test
    fun `moveTo should keep list and update current url`() {
        ArticleReadingSession.clear()
        ArticleReadingSession.open(
            ArticleReadingContext(
                articles = listOf(
                    readingItem("u1", "t1"),
                    readingItem("u2", "t2"),
                    readingItem("u3", "t3"),
                ),
                currentUrl = "u2",
            )
        )

        ArticleReadingSession.moveTo("u3")

        assertEquals("u3", ArticleReadingSession.current?.currentUrl)
        assertEquals(listOf("u1", "u2", "u3"), ArticleReadingSession.current?.articleUrls)
    }

    @Test
    fun `clear should remove reading context`() {
        ArticleReadingSession.open(
            ArticleReadingContext(
                articles = listOf(readingItem("u1", "t1")),
                currentUrl = "u1",
            )
        )

        ArticleReadingSession.clear()

        assertNull(ArticleReadingSession.current)
    }

    private fun readingItem(url: String, title: String) = ArticleReadingItem(
        url = url,
        title = title,
    )
}
