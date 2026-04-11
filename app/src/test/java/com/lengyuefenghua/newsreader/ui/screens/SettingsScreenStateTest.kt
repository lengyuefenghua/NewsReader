package com.lengyuefenghua.newsreader.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsScreenStateTest {

    @Test
    fun `stale cache limit draft should sync to persisted value`() {
        assertEquals("200", syncCacheLimitDraft(currentDraft = "1000", persistedLimit = 200))
    }

    @Test
    fun `ime done should not be used for clearing cache limit focus`() {
        assertFalse(shouldClearCacheLimitFocusOnImeDone())
    }

    @Test
    fun `cache limit should snap to nearest slider anchor`() {
        assertEquals(100, snapCacheLimitToAnchor(120))
        assertEquals(1000, snapCacheLimitToAnchor(999))
    }

    @Test
    fun `concurrent refresh should snap to nearest slider anchor`() {
        assertEquals(1, snapConcurrentCountToAnchor(0))
        assertEquals(5, snapConcurrentCountToAnchor(7))
    }

    @Test
    fun `concurrent refresh slider index should map back to anchor value`() {
        assertEquals(4, concurrentCountFromSliderIndex(3f))
    }

    @Test
    fun `source timeout should snap to nearest slider anchor`() {
        assertEquals(5, snapSourceTimeoutSecondsToAnchor(7))
        assertEquals(60, snapSourceTimeoutSecondsToAnchor(58))
    }

    @Test
    fun `source timeout slider index should map back to anchor value`() {
        assertEquals(15, sourceTimeoutSecondsFromSliderIndex(2f))
    }

    @Test
    fun `discrete slider value columns should use the same width`() {
        assertEquals(discreteSliderValueWidthDp(), concurrentCountValueWidthDp())
        assertEquals(discreteSliderValueWidthDp(), cacheLimitValueWidthDp())
        assertEquals(discreteSliderValueWidthDp(), sourceTimeoutValueWidthDp())
    }

    @Test
    fun `article retention field should use slider instead of outlined text field`() {
        assertFalse(shouldUseOutlinedCacheLimitField())
    }
}
