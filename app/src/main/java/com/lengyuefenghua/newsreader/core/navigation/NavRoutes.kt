package com.lengyuefenghua.newsreader.core.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object NavRoutes {
    const val TIMELINE = "timeline"
    const val SOURCES = "sources"
    const val DISCOVER = "discover"
    const val PROFILE = "profile"
    const val FEED_PREVIEW = "feed_preview"
    const val PLINK_MARKET = "plink_market"
    const val PLINK_FEED_PREVIEW = "plink_feed_preview?name={name}&url={url}"
    const val PLINK_FEED_PREVIEW_NAME_ARG = "name"
    const val PLINK_FEED_PREVIEW_URL_ARG = "url"

    const val ARTICLE = "article/{url}"
    const val ARTICLE_ARG = "url"

    const val SOURCE_FEED = "source_feed/{sourceId}"
    const val SOURCE_FEED_ARG = "sourceId"

    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val SOURCE_EDIT = "source_edit?id={id}&name={name}&url={url}"
    const val SOURCE_EDIT_ID_ARG = "id"
    const val SOURCE_EDIT_NAME_ARG = "name"
    const val SOURCE_EDIT_URL_ARG = "url"

    // 构建带参数的路由
    fun article(url: String): String {
        val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
        return "article/$encoded"
    }

    fun sourceFeed(sourceId: Int): String = "source_feed/$sourceId"

    fun plinkFeedPreview(name: String, url: String): String {
        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
        return "plink_feed_preview?name=$encodedName&url=$encodedUrl"
    }

    fun sourceEdit(id: Int = -1, name: String = "", url: String = ""): String {
        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
        return "source_edit?id=$id&name=$encodedName&url=$encodedUrl"
    }
}
