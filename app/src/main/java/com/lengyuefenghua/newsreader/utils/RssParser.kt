package com.lengyuefenghua.newsreader.utils

import android.util.Xml
import com.lengyuefenghua.newsreader.data.Article
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

class RssParser {
    // 仅用于生成纯文本摘要时清洗标签
    private val htmlTagRemoveRegex = Regex("<[^>]*>")

    // [新增] 结构化/块级标签特征检测
    // 如果没有 CDATA，但包含这些标签，也视为富文本正文
    // 避免只包含 <a> 或 <b> 的简单摘要被误判
    private val blockTagIndicators = listOf(
        "<p", "<div", "<br", "<table", "<ul", "<ol", "<li", "<blockquote",
        "<h1", "<h2", "<h3", "<h4", "<h5", "<h6", "<img", "<iframe"
    )

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
        var pubDate: String? = null
        var link: String? = null

        var description: String? = null
        var descriptionHasCdata = false

        var contentEncoded: String? = null
        var contentEncodedHasCdata = false

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "link" -> link = readText(parser)
                "pubDate" -> pubDate = readText(parser)
                "description" -> {
                    val result = readTextAndCheckCdata(parser)
                    description = result.first
                    descriptionHasCdata = result.second
                }
                "content:encoded" -> {
                    val result = readTextAndCheckCdata(parser)
                    contentEncoded = result.first
                    contentEncodedHasCdata = result.second
                }
                else -> skip(parser)
            }
        }

        if (title == null || link == null) return null

        // --- 核心逻辑调整 ---

        // 判断 contentEncoded 是否可用（有 CDATA 或 包含块级HTML标签）
        val isContentEncodedValid = contentEncoded != null &&
                (contentEncodedHasCdata || containsBlockTags(contentEncoded))

        // 判断 description 是否可用
        val isDescriptionValid = description != null &&
                (descriptionHasCdata || containsBlockTags(description))

        val rawBody: String
        val isFullContent: Boolean

        if (isContentEncodedValid) {
            rawBody = contentEncoded!!
            isFullContent = true
        } else if (isDescriptionValid) {
            rawBody = description!!
            isFullContent = true
        } else {
            // 如果两者都没有“像正文”的特征，则回退到 description 用于生成摘要，但标记为非全文
            rawBody = description ?: contentEncoded ?: ""
            isFullContent = false
        }

        val summary: String
        val content: String?

        // 生成摘要：去除 HTML 标签
        summary = rawBody.replace(htmlTagRemoveRegex, "").trim().take(100) + "..."

        if (isFullContent) {
            content = rawBody
        } else {
            content = null
        }

        val displayDate = formatDate(pubDate)

        return Article(
            id = link,
            title = title,
            summary = summary,
            content = content,
            sourceName = sourceName,
            pubDate = displayDate,
            url = link
        )
    }

    // [新增] 检测是否包含块级 HTML 标签
    private fun containsBlockTags(text: String): Boolean {
        // 为了提高匹配效率，先转换为小写（如果 XML 内容大小写不一致）
        // 或者直接不区分大小写查找
        val lowerText = text.lowercase(Locale.US)
        for (tag in blockTagIndicators) {
            if (lowerText.contains(tag)) {
                return true
            }
        }
        return false
    }

    private fun formatDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return ""
        return try {
            val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
            val date = inputFormat.parse(rawDate)
            if (date != null) {
                val outputFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                outputFormat.format(date)
            } else {
                rawDate
            }
        } catch (e: Exception) {
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

    private fun readTextAndCheckCdata(parser: XmlPullParser): Pair<String, Boolean> {
        val sb = StringBuilder()
        var hasCdata = false

        while (true) {
            val token = parser.nextToken()
            if (token == XmlPullParser.END_TAG) {
                break
            }
            when (token) {
                XmlPullParser.TEXT -> {
                    sb.append(parser.text)
                }
                XmlPullParser.CDSECT -> {
                    hasCdata = true
                    sb.append(parser.text)
                }
                XmlPullParser.ENTITY_REF -> {
                    // 处理常见实体
                    val replacement = when (parser.name) {
                        "lt" -> "<"
                        "gt" -> ">"
                        "amp" -> "&"
                        "apos" -> "'"
                        "quot" -> "\""
                        else -> ""
                    }
                    if (replacement.isNotEmpty()) {
                        sb.append(replacement)
                    } else {
                        sb.append("&").append(parser.name).append(";")
                    }
                }
            }
        }
        return Pair(sb.toString(), hasCdata)
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