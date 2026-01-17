package com.lengyuefenghua.newsreader.utils

import android.content.Context
import com.lengyuefenghua.newsreader.data.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

object DebugHelper {

    private const val UA_ANDROID = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    private const val UA_PC = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun runNewTest(context: Context, source: Source): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("=== 调试日志 ===\n")
        sb.append("目标: ${source.name}\n")
        sb.append("URL: ${source.url}\n")

        val userAgent = if (source.enablePcUserAgent) UA_PC else UA_ANDROID
        sb.append("模式: ${if (source.requestMethod == true) "WebView (全局单例)" else "HTTP-GET"}\n")
        sb.append("UA: ${if (source.enablePcUserAgent) "PC" else "Android"}\n")
        sb.append("----------------------------\n")

        try {
            var html = ""

            if (source.requestMethod == true) {
                // === 模式 B: 使用全局 WebViewManager ===
                sb.append(">> 正在排队使用全局 WebView...\n")

                // 直接调用我们刚写的 Manager，它会自动处理排队和主线程切换
                val result = WebViewManager.fetchHtml(source.url, userAgent)

                if (result.startsWith("ERROR:")) {
                    sb.append("❌ $result\n")
                } else {
                    html = result
                    sb.append("✅ WebView 加载完成，HTML长度: ${html.length}\n")
                }
            } else {
                // === 模式 A: OkHttp 抓取 (保持不变) ===
                sb.append(">> 正在进行 HTTP 请求...\n")
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(source.url)
                    .header("User-Agent", userAgent)
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .build()

                val response = client.newCall(request).execute()
                html = response.body?.string() ?: ""
                sb.append("HTTP状态: ${response.code}, 内容长度: ${html.length} 字符\n")
            }
            sb.append("\n")

            // 解析逻辑 (保持不变)
            if (html.isNotBlank()) {
                if (source.isCustom) {
                    testCustomListParsing(sb, html, source)
                } else {
                    testRssParsing(sb, html, source, OkHttpClient())
                }
            }

        } catch (e: Exception) {
            sb.append("\n❌ 发生严重错误: ${e.message}\n")
            e.printStackTrace()
        }

        return@withContext sb.toString()
    }

    // 这里保留 testRssParsing 和 testCustomListParsing 方法，
    // 请直接复制之前文件中的这两个方法，不需要修改。
    private fun testRssParsing(sb: StringBuilder, xml: String, source: Source, client: OkHttpClient) {
        try {
            val rssParser = RssParser()
            val items = rssParser.parse(xml.byteInputStream(), source.name)
            sb.append("✅ RSS 解析成功，找到 ${items.size} 篇文章。\n")
            sb.append("首篇文章预览\n")
            var elements = items.first()
            sb.append("文章标题：${elements.title}\n")
            sb.append("文章日期：${elements.pubDate}\n")
            sb.append("文章描述：${elements.summary}\n")
            sb.append("文章链接：${elements.url}\n")
            sb.append("文章内容：${elements.content}\n")

        } catch (e: Exception) {
            sb.append("❌ RSS 解析失败: ${e.message}\n")
        }
    }

    private fun testCustomListParsing(sb: StringBuilder, html: String, source: Source) {
        try {
            val doc = Jsoup.parse(html, source.url)
            val listSelector = if (source.ruleList.isNotBlank()) source.ruleList else "body"
            sb.append("列表规则: $listSelector\n")

            val elements = doc.select(listSelector)

            if (elements.isNotEmpty()) {
                sb.append("✅ 找到 ${elements.size} 个列表项\n")
                val first = elements.first()
                val tRule = source.ruleTitle
                val title = if (tRule.isNotBlank()) first.select(tRule).text() else first.text().take(50)
                sb.append("标题示例: $title\n")
                val lRule = source.ruleLink
                val link = if (lRule.isNotBlank()) first.select(lRule).attr("abs:href") else first.select("a").attr("abs:href")
                sb.append("链接示例: $link\n")
            } else {
                sb.append("❌ 未找到列表项。\n")
                sb.append("=== 源码预览 (前 500 字) ===\n")
                sb.append(html.take(500))
            }
        } catch (e: Exception) {
            sb.append("❌ 自定义解析失败: ${e.message}\n")
        }
    }
}