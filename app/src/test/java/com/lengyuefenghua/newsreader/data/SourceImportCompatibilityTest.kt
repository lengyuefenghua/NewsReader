package com.lengyuefenghua.newsreader.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceImportCompatibilityTest {

    private val gson = Gson()

    @Test
    fun `legacy source without group name should normalize successfully`() {
        val legacySource = gson.fromJson(
            """
            {
              "name": "少数派",
              "url": "https://sspai.com/feed",
              "iconUrl": "https://cdn.example.com/icon.png",
              "isCustom": false,
              "requestMethod": false,
              "enablePcUserAgent": false,
              "ruleList": "",
              "ruleTitle": "",
              "ruleLink": "",
              "ruleImage": "",
              "ruleSummary": "",
              "ruleContent": "",
              "useAutoExtract": false,
              "extractionAlgorithm": "readability"
            }
            """.trimIndent(),
            Source::class.java,
        )

        val normalizedSource = normalizeImportedSource(legacySource)

        assertEquals("少数派", normalizedSource.name)
        assertEquals("https://sspai.com/feed", normalizedSource.url)
        assertEquals("", normalizedSource.groupName)
        assertTrue(isImportableSource(normalizedSource))
    }

    @Test
    fun `missing extraction algorithm should keep custom mode when rule exists`() {
        val legacySource = gson.fromJson(
            """
            {
              "name": "自定义源",
              "url": "https://example.com/feed",
              "useAutoExtract": true,
              "ruleContent": ".article-content"
            }
            """.trimIndent(),
            Source::class.java,
        )

        val normalizedSource = normalizeImportedSource(legacySource)

        assertEquals("custom", normalizedSource.extractionAlgorithm)
    }
}
