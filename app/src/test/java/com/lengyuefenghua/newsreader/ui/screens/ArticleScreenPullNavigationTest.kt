package com.lengyuefenghua.newsreader.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleScreenPullNavigationTest {

    @Test
    fun `top edge should become ready when drag exceeds threshold`() {
        val state = evaluatePullNavigationState(
            edge = PullEdge.Top,
            dragOffset = 140f,
            triggerThreshold = 120f,
            hasTarget = true,
        )

        assertEquals(PullNavigationState.ReadyPrevious, state)
    }

    @Test
    fun `bottom edge should stay pulling when drag below threshold`() {
        val state = evaluatePullNavigationState(
            edge = PullEdge.Bottom,
            dragOffset = 80f,
            triggerThreshold = 120f,
            hasTarget = true,
        )

        assertEquals(PullNavigationState.PullingNext, state)
    }

    @Test
    fun `missing previous article should stay boundary only`() {
        val state = evaluatePullNavigationState(
            edge = PullEdge.Top,
            dragOffset = 180f,
            triggerThreshold = 120f,
            hasTarget = false,
        )

        assertEquals(PullNavigationState.BoundaryOnly, state)
    }

    @Test
    fun `top preview should show first boundary text when previous title missing`() {
        assertEquals("已经是第一条", getPullPreviewTitle(PullEdge.Top, null))
    }

    @Test
    fun `bottom preview should show next article title when present`() {
        assertEquals("下一篇：后续文章", getPullPreviewTitle(PullEdge.Bottom, "后续文章"))
    }

    @Test
    fun `top preview should fall back to generic previous label when title blank`() {
        assertEquals("上一篇", getPullPreviewTitle(PullEdge.Top, ""))
    }

    @Test
    fun `bottom preview should fall back to generic next label when title blank`() {
        assertEquals("下一篇", getPullPreviewTitle(PullEdge.Bottom, ""))
    }

    @Test
    fun `ready top state should expose release hint`() {
        assertEquals("释放查看上一篇", getPullReleaseHint(PullNavigationState.ReadyPrevious))
    }

    @Test
    fun `overlay height should stay compact even when pull grows`() {
        assertEquals(72f, getPullOverlayHeight(220f), 0.01f)
    }

    @Test
    fun `arc depth should use one fifth of width at most`() {
        assertEquals(60f, getPullArcDepth(width = 300f, height = 120f), 0.01f)
    }

    @Test
    fun `release evaluation should use current pull values instead of stale composed state`() {
        val state = resolvePullReleaseState(
            edge = PullEdge.Top,
            dragOffset = 220f,
            triggerThreshold = 120f,
            previousUrl = "prev",
            nextUrl = "next",
        )

        assertEquals(PullNavigationState.ReadyPrevious, state)
    }
}
