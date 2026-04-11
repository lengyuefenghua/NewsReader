package com.lengyuefenghua.newsreader.ui.screens

import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.data.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleScreenModeTest {

    @Test
    fun `no custom config and article has content should default to content mode`() {
        val shouldOpenInWebMode = shouldDefaultToWebMode(
            article = article(content = "<p>content</p>"),
            source = source(),
        )

        assertFalse(shouldOpenInWebMode)
    }

    @Test
    fun `no custom config and article has no content should default to web mode`() {
        val shouldOpenInWebMode = shouldDefaultToWebMode(
            article = article(content = null),
            source = source(),
        )

        assertTrue(shouldOpenInWebMode)
    }

    @Test
    fun `auto extract enabled should default to web mode even when article has content`() {
        val shouldOpenInWebMode = shouldDefaultToWebMode(
            article = article(content = "<p>content</p>"),
            source = source(useAutoExtract = true),
        )

        assertTrue(shouldOpenInWebMode)
    }

    @Test
    fun `custom rule content should default to web mode even when article has content`() {
        val shouldOpenInWebMode = shouldDefaultToWebMode(
            article = article(content = "<p>content</p>"),
            source = source(ruleContent = ".article-content"),
        )

        assertTrue(shouldOpenInWebMode)
    }

    @Test
    fun `content mode toggle should target web mode`() {
        assertEquals(1, getToggleTargetViewMode(currentViewMode = 0))
    }

    @Test
    fun `web mode toggle should target content mode`() {
        assertEquals(0, getToggleTargetViewMode(currentViewMode = 1))
    }

    private fun article(content: String?) = Article(
        id = "article-1",
        title = "Title",
        summary = "Summary",
        content = content,
        sourceName = "Source",
        pubDate = "2026/04/04 20:00",
        url = "https://example.com/article-1",
    )

    private fun source(
        useAutoExtract: Boolean = false,
        ruleContent: String = "",
    ) = Source(
        id = 1,
        name = "Source",
        url = "https://example.com/feed.xml",
        useAutoExtract = useAutoExtract,
        ruleContent = ruleContent,
    )
}
