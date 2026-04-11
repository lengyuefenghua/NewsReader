package com.lengyuefenghua.newsreader.utils

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WebViewScriptCacheTest {

    @Before
    fun setUp() {
        WebViewScriptCache.clear()
    }

    @Test
    fun `same script path should only load once`() {
        var loadCount = 0

        val (first, firstFromCache) = WebViewScriptCache.getOrPut("algorithm/loader.js") {
            loadCount += 1
            "loader-script"
        }
        val (second, secondFromCache) = WebViewScriptCache.getOrPut("algorithm/loader.js") {
            loadCount += 1
            "loader-script-new"
        }

        assertEquals("loader-script", first)
        assertEquals("loader-script", second)
        assertEquals(false, firstFromCache)
        assertEquals(true, secondFromCache)
        assertEquals(1, loadCount)
    }

    @Test
    fun `different script paths should maintain separate cache entries`() {
        var loadCount = 0

        val (loader, loaderFromCache) = WebViewScriptCache.getOrPut("algorithm/loader.js") {
            loadCount += 1
            "loader-script"
        }
        val (main, mainFromCache) = WebViewScriptCache.getOrPut("algorithm/main.js") {
            loadCount += 1
            "main-script"
        }

        assertEquals("loader-script", loader)
        assertEquals("main-script", main)
        assertEquals(false, loaderFromCache)
        assertEquals(false, mainFromCache)
        assertEquals(2, loadCount)
    }
}
