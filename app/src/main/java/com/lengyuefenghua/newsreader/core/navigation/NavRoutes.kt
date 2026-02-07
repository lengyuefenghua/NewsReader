package com.lengyuefenghua.newsreader.core.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object NavRoutes {
    const val TIMELINE = "timeline"
    const val SOURCES = "sources"
    const val PROFILE = "profile"

    const val ARTICLE = "article/{url}"
    const val ARTICLE_ARG = "url"

    const val SOURCE_FEED = "source_feed/{sourceId}"
    const val SOURCE_FEED_ARG = "sourceId"

    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val STATS = "stats"

    // 构建带参数的路由
    fun article(url: String): String {
        val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
        return "article/$encoded"
    }

    fun sourceFeed(sourceId: Int): String = "source_feed/$sourceId"
}
