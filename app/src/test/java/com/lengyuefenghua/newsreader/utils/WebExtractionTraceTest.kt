package com.lengyuefenghua.newsreader.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class WebExtractionTraceTest {

    @Test
    fun `scripts elapsed should be based on extraction start`() {
        val started = startWebExtractionTrace(nowMs = 1_000L)
        val extracting = started.markExtractionStarted(nowMs = 1_200L)
        val scriptsInjected = extracting.markScriptsInjected(nowMs = 1_260L)

        assertEquals(60L, scriptsInjected.scriptsElapsedMs())
    }

    @Test
    fun `total and extraction elapsed should remain stable for one trace`() {
        val trace = startWebExtractionTrace(nowMs = 10_000L)
            .markLoaderInjected(nowMs = 10_010L)
            .markExtractionStarted(nowMs = 10_500L)

        assertEquals(700L, trace.totalElapsedMs(nowMs = 10_700L))
        assertEquals(200L, trace.extractionElapsedMs(nowMs = 10_700L))
    }
}
