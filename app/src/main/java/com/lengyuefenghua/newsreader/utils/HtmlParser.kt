package com.lengyuefenghua.newsreader.utils

import android.util.Log
import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.utils.LogUtils
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
            
            LogUtils.log("┍解析文章列表<$listSelector>")
            val elements = doc.select(listSelector)
            LogUtils.log("┕${elements.size}篇文章")

            elements.forEachIndexed { index, element ->
                // 1. 标题
                val title: String
                val titleRule = if (source.ruleTitle.isNotBlank()) source.ruleTitle else "text()"
                if (source.ruleTitle.isNotBlank()) {
                    title = element.select(source.ruleTitle).text()
                } else {
                    title = element.text().take(20)
                }

                // 2. 链接
                var linkUrl = ""
                val linkRule = if (source.ruleLink.isNotBlank()) source.ruleLink else "a[href]"
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
                var imageUrl: String? = null
                val imageRule = if (source.ruleImage.isNotBlank()) source.ruleImage else "img[src]"
                if (source.ruleImage.isNotBlank()) {
                    imageUrl = element.select(source.ruleImage).attr("abs:src")
                }
                if (imageUrl.isNullOrBlank()) {
                    imageUrl = element.select("img").first()?.attr("abs:src")
                }
                
                // [日志] 仅记录第一篇文章的详细解析
                if (index == 0) {
                    LogUtils.log("┍解析文章标题<$titleRule>")
                    LogUtils.log("┕$title")
                    
                    LogUtils.log("┍解析文章链接<$linkRule>")
                    LogUtils.log("┕$linkUrl")
                    
                    LogUtils.log("┍解析文章图片<$imageRule>")
                    LogUtils.log("┕${imageUrl ?: "无"}")
                    
                    LogUtils.log("┍解析文章摘要<${source.ruleSummary}>")
                    LogUtils.log("┕${summaryText.take(50)}")
                    
                    LogUtils.log("┍解析文章正文<无>")
                    LogUtils.log("┕列表模式不解析正文")
                }

                if (title.isNotBlank() && linkUrl.isNotBlank()) {
                    articles.add(
                        Article(
                            id = linkUrl,
                            title = title,
                            summary = summaryText, // 列表显示的文字
                            content = null,        // [关键修改] 设为 null，ArticleScreen 就会直接加载 URL
                            sourceName = source.name,
                            pubDate = SimpleDateFormat(
                                "yyyy/MM/dd HH:mm",
                                Locale.getDefault()
                            ).format(Date()),
                            url = linkUrl,
                            imageUrl = imageUrl // [新增]
                        )
                    )
                }
            }
        } catch (e: Exception) {
            LogUtils.log("Parse Error: ${e.message}")
            e.printStackTrace()
        }
        return articles
    }
}