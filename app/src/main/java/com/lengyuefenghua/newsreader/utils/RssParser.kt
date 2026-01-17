package com.lengyuefenghua.newsreader.utils

import android.util.Xml
import com.lengyuefenghua.newsreader.data.Article
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

class RssParser {
    // 简单的正则，用来检测是否包含 HTML 标签
    private val htmlTagRegex = Regex("<[a-zA-Z/].*?>")

    fun parse(inputStream: InputStream, sourceName: String): List<Article> {
        inputStream.use { stream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, null)
            parser.nextTag()
            return readFeed(parser, sourceName)
        }
    }

    private fun readFeed(parser: XmlPullParser, sourceName: String): List<Article> {
        val entries = mutableListOf<Article>()
        parser.require(XmlPullParser.START_TAG, null, "rss")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "channel") {
                entries.addAll(readChannel(parser, sourceName))
            } else {
                skip(parser)
            }
        }
        return entries
    }

    private fun readChannel(parser: XmlPullParser, sourceName: String): List<Article> {
        val entries = mutableListOf<Article>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "item") {
                readEntry(parser, sourceName)?.let { entries.add(it) }
            } else {
                skip(parser)
            }
        }
        return entries
    }

    private fun readEntry(parser: XmlPullParser, sourceName: String): Article? {
        parser.require(XmlPullParser.START_TAG, null, "item")
        var title: String? = null
        var description: String? = null
        var contentEncoded: String? = null
        var pubDate: String? = null
        var link: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "link" -> link = readText(parser)
                "pubDate" -> pubDate = readText(parser)
                "description" -> description = readText(parser)
                // 很多 RSS 使用 content:encoded 存放完整的 CDATA 正文
                "content:encoded" -> contentEncoded = readText(parser)
                else -> skip(parser)
            }
        }

        if (title == null || link == null) return null

        // --- 核心逻辑：判断是否有正文 ---

        // 1. 优先取 content:encoded，没有则取 description
        val rawBody = contentEncoded ?: description ?: ""

        // 2. 检测是否包含 HTML 标签 (判断是否曾被 CDATA 包裹)
        val hasHtml = htmlTagRegex.containsMatchIn(rawBody)

        val summary: String
        val content: String?

        if (hasHtml) {
            // 情况 A: 包含 HTML -> 说明 RSS 提供了正文/富文本摘要
            // 列表页：清洗标签，只取前 100 字作为简介
            summary = rawBody.replace(htmlTagRegex, "").trim().take(100) + "..."
            // 详情页：直接使用原始 HTML (缓存起来，点击即看)
            content = rawBody
        } else {
            // 情况 B: 纯文本 -> 说明 RSS 只给了简介，或者没给正文
            // 列表页：直接显示
            summary = rawBody.trim()
            // 详情页：标记为 null，告诉 UI 需要加载 URL
            content = null
        }

        // 简单处理时间
        val displayDate = formatDate(pubDate)

        return Article(
            id = link,
            title = title,
            summary = summary,
            content = content, // 只要这里不是 null，就说明有本地缓存的正文
            sourceName = sourceName,
            pubDate = displayDate,
            url = link
        )
    }
    // 日期格式化辅助函数
    private fun formatDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return ""
        return try {
            // RSS 标准日期格式 (RFC 1123)，例如: Wed, 14 Jan 2026 15:30:00 GMT
            // 使用 Locale.US 解析英文月份/星期
            val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
            val date = inputFormat.parse(rawDate)

            if (date != null) {
                // 目标格式: 2026/01/17 11:56
                val outputFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                outputFormat.format(date)
            } else {
                rawDate // 解析失败返回原值
            }
        } catch (e: Exception) {
            // 如果格式不匹配（比如是 ISO 8601），尝试 ISO 格式
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                val date = isoFormat.parse(rawDate)
                if (date != null) {
                    val outputFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    outputFormat.format(date)
                } else {
                    rawDate
                }
            } catch (e2: Exception) {
                // 依然解析失败，尝试去掉星期几再解析，或者直接返回原始值
                // 之前代码是取前16位，作为最后的兜底
                try { rawDate.take(16) } catch (e3: Exception) { rawDate }
            }
        }
    }
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) throw IllegalStateException()
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}