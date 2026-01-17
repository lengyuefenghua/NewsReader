package com.lengyuefenghua.newsreader.utils

import android.util.Log
import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.data.Source
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HtmlParser {
    fun parse(html: String, source: Source): List<Article> {
        val articles = mutableListOf<Article>()
        try {
            // 设置 Base URL 以支持相对路径
            val doc = Jsoup.parse(html, source.url)
            val listSelector = if (source.ruleList.isNotBlank()) source.ruleList else "body"
            val elements = doc.select(listSelector)

            elements.forEachIndexed { index, element ->
                // 1. 标题
                val title = if (source.ruleTitle.isNotBlank()) {
                    element.select(source.ruleTitle).text()
                } else {
                    element.text().take(20)
                }

                // 2. 链接
                var linkUrl = ""
                if (source.ruleLink.isNotBlank()) {
                    linkUrl = element.select(source.ruleLink).attr("abs:href")
                }
                if (linkUrl.isBlank()) {
                    linkUrl = element.select("a").first()?.attr("abs:href") ?: ""
                }

                // 3. 摘要 (支持自定义规则)
                var summaryText = "点击查看详情"
                if (source.ruleSummary.isNotBlank()) {
                    summaryText = element.select(source.ruleSummary).text()
                }

                // 4. 图片
                // (这里仅作为逻辑保留，实际上 HtmlParser 不往 summary 里拼图了，图可以在 ArticleCard 里单独显示)
                // 如果需要，您可以把图片 URL 存到 article.summary 里，或者扩展 Article 加 imageUrl 字段

                if (title.isNotBlank() && linkUrl.isNotBlank()) {
                    articles.add(
                        Article(
                            id = linkUrl,
                            title = title,
                            summary = summaryText, // 列表显示的文字
                            content = null,        // [关键修改] 设为 null，ArticleScreen 就会直接加载 URL
                            sourceName = source.name,
                            pubDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date()),
                            url = linkUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return articles
    }
}