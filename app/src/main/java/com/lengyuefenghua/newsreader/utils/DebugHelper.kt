package com.lengyuefenghua.newsreader.utils

import android.content.Context
import com.lengyuefenghua.newsreader.data.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DebugResult(
    val log: String,
    val rawSource: String?
)

object DebugHelper {

    private const val UA_ANDROID =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    private const val UA_PC =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private fun log(sb: StringBuilder, msg: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        sb.append("[$time] $msg\n")
    }

    suspend fun runNewTest(context: Context, source: Source): DebugResult = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        
        log(sb, "开始调试订阅源")
        log(sb, "订阅源名称：${source.name}")
        log(sb, "订阅源地址：${source.url}")
        log(sb, "订阅源模式：${if (source.requestMethod == true) "WebView (全局单例)" else "HTTP-GET"}")
        log(sb, "订阅源UA：${if (source.enablePcUserAgent) "PC" else "Android"}")
        log(sb, "初始化调试环境")
        log(sb, "加载url")

        var html = ""

        try {
            val userAgent = if (source.enablePcUserAgent) UA_PC else UA_ANDROID

            if (source.requestMethod == true) {
                // === 模式 B: 使用全局 WebViewManager ===
                val result = WebViewManager.fetchHtml(source.url, userAgent)

                if (result.startsWith("ERROR:")) {
                    log(sb, "❌ $result")
                } else {
                    html = result
                    log(sb, "加载完成：WebView 模式，HTML长度：${html.length}")
                }
            } else {
                // === 模式 A: OkHttp 抓取 ===
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(source.url)
                    .header("User-Agent", userAgent)
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .build()

                val response = client.newCall(request).execute()
                html = response.body?.string() ?: ""
                log(sb, "加载完成：HTTP状态：${response.code}，内容长度：${html.length / 1024}KB")
            }

            if (html.isNotBlank()) {
                if (source.isCustom) {
                    testCustomListParsing(sb, html, source)
                } else {
                    testRssParsing(sb, html, source)
                }
            }

        } catch (e: Exception) {
            log(sb, "❌ 发生严重错误: ${e.message}")
            e.printStackTrace()
        }

        return@withContext DebugResult(sb.toString(), html)
    }

    private fun testRssParsing(
        sb: StringBuilder,
        xml: String,
        source: Source
    ) {
        val startTime = System.currentTimeMillis()
        try {
            log(sb, "┍解析文章列表<RSS/Atom Standard>")
            val rssParser = RssParser()
            val result = rssParser.parse(xml.byteInputStream(), source.name)
            val items = result.articles
            val iconUrl = result.iconUrl

            log(sb, "┕${items.size}篇文章")

            if (iconUrl != null) {
                log(sb, "┍解析订阅源图标")
                log(sb, "┕$iconUrl")
            }
            
            val first = items.firstOrNull()
            if (first != null) {
                log(sb, "┍解析文章标题<tag:title>")
                log(sb, "┕${first.title}")
                
                log(sb, "┍解析文章链接<tag:link>")
                log(sb, "┕${first.url}")
                
                log(sb, "┍解析文章图片<tag:enclosure/media/img>")
                log(sb, "┕${first.imageUrl ?: "无"}")
                
                log(sb, "┍解析文章摘要<tag:description/summary>")
                log(sb, "┕${first.summary.take(50)}...")
                
                log(sb, "┍解析文章正文<tag:content/encoded>")
                val content = first.content
                if (content != null) {
                    log(sb, "┕${content.length} 字符")
                } else {
                    log(sb, "┕无")
                }
            }
            
            log(sb, "解析完成：${System.currentTimeMillis() - startTime}毫秒")
            
            if (first?.content != null) {
                sb.append("\n=== 文章正文预览 ===\n")
                sb.append(first.content)
                sb.append("\n")
            }
            
        } catch (e: Exception) {
            log(sb, "❌ RSS 解析失败: ${e.message}")
        }
    }

    private fun testCustomListParsing(sb: StringBuilder, html: String, source: Source) {
        val startTime = System.currentTimeMillis()
        try {
            val doc = Jsoup.parse(html, source.url)
            val listSelector = if (source.ruleList.isNotBlank()) source.ruleList else "body"
            
            log(sb, "┍解析文章列表<$listSelector>")
            val elements = doc.select(listSelector)
            log(sb, "┕${elements.size}篇文章")

            if (elements.isNotEmpty()) {
                val element = elements.first()
                
                // Title
                val titleRule = if (source.ruleTitle.isNotBlank()) source.ruleTitle else "text()"
                val title = if (source.ruleTitle.isNotBlank()) element.select(source.ruleTitle).text() else element.text().take(20)
                log(sb, "┍解析文章标题<$titleRule>")
                log(sb, "┕$title")

                // Link
                val linkRule = if (source.ruleLink.isNotBlank()) source.ruleLink else "a[href]"
                var linkUrl = ""
                if (source.ruleLink.isNotBlank()) linkUrl = element.select(source.ruleLink).attr("abs:href")
                if (linkUrl.isBlank()) linkUrl = element.select("a").first()?.attr("abs:href") ?: ""
                log(sb, "┍解析文章链接<$linkRule>")
                log(sb, "┕$linkUrl")
                
                // Image
                val imageRule = if (source.ruleImage.isNotBlank()) source.ruleImage else "img[src]"
                var imageUrl = ""
                if (source.ruleImage.isNotBlank()) imageUrl = element.select(source.ruleImage).attr("abs:src")
                if (imageUrl.isBlank()) imageUrl = element.select("img").first()?.attr("abs:src") ?: ""
                log(sb, "┍解析文章图片<$imageRule>")
                log(sb, "┕${imageUrl.ifBlank { "无" }}")
                
                // Summary
                val summaryRule = if (source.ruleSummary.isNotBlank()) source.ruleSummary else "无"
                var summaryText = "无"
                if (source.ruleSummary.isNotBlank()) summaryText = element.select(source.ruleSummary).text()
                log(sb, "┍解析文章摘要<$summaryRule>")
                log(sb, "┕${summaryText.take(50)}")
                
                log(sb, "┍解析文章正文<无>")
                log(sb, "┕列表模式不解析正文")
            } else {
                log(sb, "❌ 未找到列表项")
            }
            
            log(sb, "解析完成：${System.currentTimeMillis() - startTime}毫秒")
            
        } catch (e: Exception) {
            log(sb, "❌ 自定义解析失败: ${e.message}")
        }
    }
}
