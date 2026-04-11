package com.lengyuefenghua.newsreader.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleLoadStateTest {

    @Test
    fun `page finished should be ignored when a newer load is pending`() {
        val (requestedState, generation) = requestArticleLoad(ArticleLoadState())

        assertEquals(1, generation)
        assertTrue(shouldIgnorePageFinished(requestedState))
    }

    @Test
    fun `page finished should be handled after pending load starts`() {
        val (requestedState, _) = requestArticleLoad(ArticleLoadState())
        val startedState = markPageStarted(requestedState)

        assertFalse(shouldIgnorePageFinished(startedState))
    }

    @Test
    fun `new request should replace previous pending generation`() {
        val (firstState, firstGeneration) = requestArticleLoad(ArticleLoadState())
        val (secondState, secondGeneration) = requestArticleLoad(firstState)

        assertEquals(1, firstGeneration)
        assertEquals(2, secondGeneration)
        assertEquals(2, secondState.pendingGeneration)
    }

    @Test
    fun `new article session should preserve a pending generation`() {
        val (requestedState, generation) = requestArticleLoad(ArticleLoadState())

        val sessionState = resetForNewArticleSession(requestedState)

        assertEquals(generation, sessionState.pendingGeneration)
        assertEquals(requestedState.nextGeneration, sessionState.nextGeneration)
    }
}
