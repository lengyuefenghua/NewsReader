package com.lengyuefenghua.newsreader.viewmodel

import com.lengyuefenghua.newsreader.data.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedPreviewViewModelTest {

    @Test
    fun `setPreview should store preview data and keep explicit title`() {
        val viewModel = FeedPreviewViewModel()
        val articles = listOf(sampleArticle())

        viewModel.setPreview(
            title = "少数派",
            url = "https://sspai.com/feed",
            articles = articles,
            iconUrl = "https://sspai.com/icon.png"
        )

        val preview = viewModel.preview.value
        assertEquals("少数派", preview?.displayTitle)
        assertEquals("https://sspai.com/feed", preview?.url)
        assertEquals(1, preview?.articles?.size)
        assertEquals("https://sspai.com/icon.png", preview?.iconUrl)
        assertTrue(viewModel.hasPreview())
    }

    @Test
    fun `setPreview should fallback to host when title is blank`() {
        val viewModel = FeedPreviewViewModel()

        viewModel.setPreview(
            title = "   ",
            url = "https://example.com/rss.xml",
            articles = listOf(sampleArticle()),
            iconUrl = null
        )

        assertEquals("example.com", viewModel.preview.value?.displayTitle)
    }

    @Test
    fun `clearPreview should remove stored preview`() {
        val viewModel = FeedPreviewViewModel()

        viewModel.setPreview(
            title = "测试源",
            url = "https://example.com/rss.xml",
            articles = listOf(sampleArticle()),
            iconUrl = null
        )
        viewModel.clearPreview()

        assertNull(viewModel.preview.value)
        assertFalse(viewModel.hasPreview())
    }

    private fun sampleArticle() = Article(
        id = "1",
        title = "标题",
        summary = "摘要",
        content = null,
        sourceName = "测试源",
        pubDate = "2026-04-11",
        url = "https://example.com/article"
    )
}
