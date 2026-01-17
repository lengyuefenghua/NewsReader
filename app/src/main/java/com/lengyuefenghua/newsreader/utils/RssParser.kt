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
        // [修改] 预处理步骤：读取流 -> 清洗特殊字符 -> 转回流
        // 1. 读取原始内容
        val rawContent = inputStream.bufferedReader().use { it.readText() }

        // 2. 清洗数据：
        // - 将不换行空格 (\u00a0) 替换为标准空格 (\u0020)
        // - 去除首尾空白 (防止 <?xml 前有空格导致解析错误)
        val sanitizedContent = rawContent.replace("\uFEFF", "")
            .replace('\u00a0', ' ')
            .trim()

        // 3. 将清洗后的字符串转换为新的输入流
        val sanitizedStream = sanitizedContent.byteInputStream()
        sanitizedStream.use { stream ->
            val parser: XmlPullParser = Xml.newPullParser()
            // 禁用命名空间处理，以便混用 rss/atom 标签时更简单（如 content:encoded）
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, null)

            // 3. 智能查找根节点
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    // 找到根节点（rss 或 feed）立即开始处理
                    if (parser.name == "rss" || parser.name == "feed") {
                        return readFeed(parser, sourceName)
                    }
                }
                eventType = parser.next()
            }
            return emptyList()
        }
    }

    // [修改] 兼容 RSS 的 <channel> 结构和 Atom 的直接 <entry> 结构
    private fun readFeed(parser: XmlPullParser, sourceName: String): List<Article> {
        val entries = mutableListOf<Article>()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            // RSS 结构: rss -> channel -> item
            if (parser.name == "channel") {
                entries.addAll(readChannel(parser, sourceName))
            }
            // Atom 结构: feed -> entry (直接在根节点下)
            else if (parser.name == "entry") {
                readEntry(parser, sourceName)?.let { entries.add(it) }
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

    // [修改] 核心解析逻辑：同时兼容 RSS 标签和 Atom 标签
    private fun readEntry(parser: XmlPullParser, sourceName: String): Article? {
        // 移除严格的 require("item")，允许 "entry"

        var title: String? = null
        var pubDate: String? = null
        var link: String? = null

        var description: String? = null
        var descriptionHasCdata = false

        var contentEncoded: String? = null
        var contentEncodedHasCdata = false

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            val tagName = parser.name

            when {
                tagName == "title" -> title = readText(parser)

                // 兼容链接：Atom (<link href="..."/>) 与 RSS (<link>...</link>)
                tagName == "link" -> {
                    val href = parser.getAttributeValue(null, "href")
                    if (!href.isNullOrBlank()) {
                        link = href // Atom 风格
                        parser.nextTag() // 跳过空内容的结束标签 </link>
                    } else {
                        link = readText(parser) // RSS 风格
                    }
                }

                // 兼容时间：Atom (published/updated) 与 RSS (pubDate)
                tagName == "pubDate" || tagName == "published" || tagName == "updated" -> {
                    // 优先取第一次读到的时间，或者是 published
                    if (pubDate == null || tagName == "published") {
                        pubDate = readText(parser)
                    } else {
                        skip(parser)
                    }
                }

                // 兼容摘要：Atom (summary) 与 RSS (description)
                tagName == "description" || tagName == "summary" -> {
                    val result = readTextAndCheckCdata(parser)
                    description = result.first
                    descriptionHasCdata = result.second
                }

                // 兼容正文：Atom (content) 与 RSS (content:encoded)
                tagName == "content:encoded" || tagName == "content" -> {
                    val result = readTextAndCheckCdata(parser)
                    contentEncoded = result.first
                    contentEncodedHasCdata = result.second
                }

                else -> skip(parser)
            }
        }

        if (title == null) return null
        val finalLink = link ?: ""

        // --- 以下正文/摘要判定逻辑保持不变 ---

        val isContentEncodedValid = contentEncoded != null &&
                (contentEncodedHasCdata || containsBlockTags(contentEncoded))
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
            rawBody = description ?: contentEncoded ?: ""
            isFullContent = false
        }

        val summary = rawBody.replace(htmlTagRemoveRegex, "").trim().take(100) + "..."
        val content = if (isFullContent) rawBody else null
        val displayDate = formatDate(pubDate)

        return Article(
            id = finalLink,
            title = title,
            summary = summary,
            content = content,
            sourceName = sourceName,
            pubDate = displayDate,
            url = finalLink
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
        } catch (_: Exception) {
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                val date = isoFormat.parse(rawDate)
                if (date != null) {
                    val outputFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    outputFormat.format(date)
                } else {
                    rawDate
                }
            } catch (_: Exception) {
                try {
                    rawDate.take(16)
                } catch (_: Exception) {
                    rawDate
                }
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