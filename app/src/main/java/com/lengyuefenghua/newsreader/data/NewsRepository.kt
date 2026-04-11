package com.lengyuefenghua.newsreader.data

import android.util.Log
import com.lengyuefenghua.newsreader.core.domain.model.NetworkError
import com.lengyuefenghua.newsreader.core.domain.model.Result
import com.lengyuefenghua.newsreader.utils.HtmlParser
import com.lengyuefenghua.newsreader.utils.LogUtils
import com.lengyuefenghua.newsreader.utils.RssParser
import com.lengyuefenghua.newsreader.util.SettingsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.IOException
import java.net.URI
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NewsRepository(
    private val database: AppDatabase,
    private val settingsManager: SettingsManager
) {

    private val sourceDao = database.sourceDao()
    private val articleDao = database.articleDao()

    private val client = OkHttpClient()
    private val rssParser = RssParser()
    private val htmlParser = HtmlParser()

    private data class HttpResponsePayload(
        val code: Int,
        val isSuccessful: Boolean,
        val body: String?
    )

    data class FeedPreviewResult(
        val title: String?,
        val articles: List<Article>,
        val iconUrl: String?
    )

    data class FeedCatalogSource(
        val name: String,
        val url: String
    )

    data class FeedCatalogGroup(
        val name: String,
        val feeds: List<FeedCatalogSource>
    )

    val allArticles: Flow<List<Article>> = articleDao.getAllArticlesFlow()

    // [新增] 暴露所有 Source 用于获取图标
    fun getAllSources(): Flow<List<Source>> = sourceDao.getAllSources()

    companion object {
        const val PLINK_URL = "https://plink.anyfeeder.com/"
        const val AWESOME_RSSHUB_ROUTES_URL = "https://jackyst0.github.io/awesome-rsshub-routes/"
        const val AWESOME_RSSHUB_ROUTES_OPML_URL = "${AWESOME_RSSHUB_ROUTES_URL}feeds.opml"
        const val TOP_RSS_LIST_URL = "https://github.com/weekend-project-space/top-rss-list/blob/main/README.md"
        const val TOP_RSS_LIST_RAW_URL = "https://raw.githubusercontent.com/weekend-project-space/top-rss-list/main/README.md"
        const val WECHAT2RSS_URL = "https://wechat2rss.xlab.app/list/all.html"
        const val QIREADER_DISCOVER_URL = "https://www.qireader.com.cn/discover?lastUpdateDays=7&language=zh"
        const val UA_ANDROID =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        const val UA_PC =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    // [新增] 获取特定源的文章流
    fun getArticlesBySource(sourceName: String): Flow<List<Article>> {
        return articleDao.getArticlesBySourceFlow(sourceName)
    }

    fun getUnreadCount(): Flow<Int> {
        return articleDao.getUnreadCountFlow()
    }

    fun getUnreadCountBySource(sourceName: String): Flow<Int> {
        return articleDao.getUnreadCountBySourceFlow(sourceName)
    }

    // [新增] 获取单篇文章流，修复类型不匹配问题
    fun getArticleFlow(url: String): Flow<Article?> {
        return articleDao.getArticleFlow(url)
    }

    // [重构] syncAll 改为并发刷新,返回 RefreshSummary
    // concurrentLimit: 并发数（1-5，默认 3）
    // onProgress: 每个源刷新完成后回调
    suspend fun syncAll(
        concurrentLimit: Int = 3,
        onProgress: (RefreshProgress) -> Unit
    ): Result<RefreshSummary> = withContext(Dispatchers.IO) {
        try {
            val sources = sourceDao.getAllSources().first()
            if (sources.isEmpty()) {
                return@withContext Result.Success(RefreshSummary(0, 0, 0))
            }

            val semaphore = Semaphore(concurrentLimit)
            val total = sources.size
            var failedSources = 0
            val completedCount = java.util.concurrent.atomic.AtomicInteger(0)

            val deferredResults = sources.map { source ->
                async {
                    semaphore.acquire()
                    try {
                        val newCount = fetchAndSave(source)
                        val current = completedCount.incrementAndGet()
                        onProgress(
                            RefreshProgress(
                                sourceName = source.name,
                                success = true,
                                newArticleCount = newCount,
                                current = current,
                                total = total
                            )
                        )
                        newCount
                    } catch (e: Exception) {
                        Log.e("NewsRepository", "刷新失败: ${source.name}", e)
                        failedSources++
                        val current = completedCount.incrementAndGet()
                        onProgress(
                            RefreshProgress(
                                sourceName = source.name,
                                success = false,
                                newArticleCount = 0,
                                current = current,
                                total = total
                            )
                        )
                        0
                    } finally {
                        semaphore.release()
                    }
                }
            }

            val totalNewArticles = deferredResults.awaitAll().sum()

            Result.Success(
                RefreshSummary(
                    totalNewArticles = totalNewArticles,
                    totalSources = total,
                    failedSources = failedSources
                )
            )
        } catch (e: Exception) {
            when (e) {
                is java.net.UnknownHostException -> {
                    Result.Error(NetworkError.NetworkException("网络连接失败", e))
                }
                is java.net.SocketTimeoutException -> {
                    Result.Error(NetworkError.NetworkException("连接超时", e))
                }
                else -> {
                    Result.Error(NetworkError.UnknownError("同步失败: ${e.message}", e))
                }
            }
        }
    }

    suspend fun syncAllTwoPhase(
        fastBatchSize: Int = 1,
        onEvent: (TwoPhaseRefreshEvent) -> Unit
    ): Result<TwoPhaseRefreshSummary> = withContext(Dispatchers.IO) {
        try {
            val sources = sourceDao.getAllSources().first()
            val summary = runTwoPhaseRefresh(
                sources = sources,
                fastBatchSize = fastBatchSize,
                fetcher = { source -> fetchAndSave(source) },
                onEvent = onEvent
            )
            Result.Success(summary)
        } catch (e: Exception) {
            when (e) {
                is java.net.UnknownHostException -> {
                    Result.Error(NetworkError.NetworkException("网络连接失败", e))
                }
                is java.net.SocketTimeoutException -> {
                    Result.Error(NetworkError.NetworkException("连接超时", e))
                }
                else -> {
                    Result.Error(NetworkError.UnknownError("同步失败: ${e.message}", e))
                }
            }
        }
    }

    suspend fun syncSource(sourceId: Int): Int? = withContext(Dispatchers.IO) {
        val source = sourceDao.getSourceById(sourceId)
        if (source != null) {
            fetchAndSave(source)
        } else {
            null
        }
    }

    suspend fun markArticleAsRead(id: String) = withContext(Dispatchers.IO) {
        articleDao.markAsRead(id)
    }

    // [新增] 更新阅读时长
    suspend fun updateReadDuration(id: String, duration: Long) = withContext(Dispatchers.IO) {
        articleDao.updateReadDuration(id, duration)
    }

    suspend fun toggleFavorite(id: String, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        articleDao.updateFavorite(id, !currentStatus)
    }

    suspend fun getSourceById(sourceId: Int): Source? = withContext(Dispatchers.IO) {
        sourceDao.getSourceById(sourceId)
    }

    suspend fun getSourceIdByName(name: String): Int? = withContext(Dispatchers.IO) {
        sourceDao.getSourceIdByName(name)
    }

    /**
     * 获取订阅源的文章列表（不保存到数据库）
     * 用于检测订阅源是否包含完整正文
     */
    suspend fun fetchArticles(source: Source): List<Article> = withContext(Dispatchers.IO) {
        try {
            val currentUserAgent = if (source.enablePcUserAgent) UA_PC else UA_ANDROID

            val request = Request.Builder()
                .url(source.url)
                .header("User-Agent", currentUserAgent)
                .build()

            val response = executeRequest(request)
            val responseString = response.body

            if (response.isSuccessful && responseString != null) {
                val items: List<Article> = if (source.isCustom) {
                    htmlParser.parse(responseString, source)
                } else {
                    val result = rssParser.parse(responseString.byteInputStream(), source.name)
                    result.articles
                }
                items
            } else {
                emptyList()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("NewsRepository", "获取文章失败", e)
            emptyList()
        }
    }

    suspend fun fetchFeedPreview(url: String): FeedPreviewResult = withContext(Dispatchers.IO) {
        val fallbackTitle = runCatching { URI(url).host.orEmpty().removePrefix("www.") }
            .getOrDefault("")
            .ifBlank { url }
        val source = Source(name = fallbackTitle, url = url)

        try {
            val request = Request.Builder()
                .url(source.url)
                .header("User-Agent", UA_ANDROID)
                .build()

            val response = executeRequest(request)
            val responseString = response.body

            if (!response.isSuccessful || responseString.isNullOrBlank()) {
                return@withContext FeedPreviewResult(
                    title = fallbackTitle,
                    articles = emptyList(),
                    iconUrl = null
                )
            }

            val result = rssParser.parse(responseString.byteInputStream(), source.name)
            FeedPreviewResult(
                title = result.title?.ifBlank { fallbackTitle } ?: fallbackTitle,
                articles = result.articles,
                iconUrl = result.iconUrl
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("NewsRepository", "预览抓取失败", e)
            FeedPreviewResult(
                title = fallbackTitle,
                articles = emptyList(),
                iconUrl = null
            )
        }
    }

    suspend fun fetchPlinkFeedGroups(): List<FeedCatalogGroup> = fetchFeedCatalogGroups(
        requestUrl = PLINK_URL,
        sourceName = "Plink",
        parser = ::parsePlinkFeedGroups
    )

    suspend fun fetchAwesomeRssHubFeedGroups(): List<FeedCatalogGroup> = fetchFeedCatalogGroups(
        requestUrl = AWESOME_RSSHUB_ROUTES_OPML_URL,
        sourceName = "Awesome RSSHub Routes",
        parser = ::parseAwesomeRssHubFeedGroups
    )

    suspend fun fetchTopRssListFeedGroups(): List<FeedCatalogGroup> = fetchFeedCatalogGroups(
        requestUrl = TOP_RSS_LIST_RAW_URL,
        sourceName = "Top RSS List",
        parser = ::parseTopRssListFeedGroups
    )

    suspend fun fetchWechat2RssFeedGroups(): List<FeedCatalogGroup> = fetchFeedCatalogGroups(
        requestUrl = WECHAT2RSS_URL,
        sourceName = "Wechat2RSS",
        parser = ::parseWechat2RssFeedGroups
    )

    private suspend fun fetchAndSave(source: Source): Int {
        val startTime = System.currentTimeMillis()
        try {
            val currentUserAgent = if (source.enablePcUserAgent) UA_PC else UA_ANDROID

            LogUtils.log("开始调试订阅源")
            LogUtils.log("订阅源名称：${source.name}")
            LogUtils.log("订阅源地址：${source.url}")
            LogUtils.log("订阅源模式：${if (source.isCustom) "HTML" else "RSS"}")
            LogUtils.log("订阅源UA：$currentUserAgent")
            LogUtils.log("初始化调试环境")
            LogUtils.log("加载url")

            val request = Request.Builder()
                .url(source.url)
                .header("User-Agent", currentUserAgent)
                .build()

            val response = executeRequest(request)
            val responseString = response.body

            if (response.isSuccessful && responseString != null) {
                val kbSize = responseString.length / 1024
                LogUtils.log("加载完成：HTTP状态：${response.code}，内容长度：${kbSize}KB")

                var items: List<Article> = emptyList()

                if (source.isCustom) {
                    items = htmlParser.parse(responseString, source)
                } else {
                    val result = rssParser.parse(responseString.byteInputStream(), source.name)
                    items = result.articles
                    val iconUrl = result.iconUrl

                    if (!iconUrl.isNullOrBlank()) {
                         LogUtils.log("┍解析订阅源图标")
                         LogUtils.log("┕$iconUrl")
                    }

                    // 如果解析到了图标，且与当前不同，则更新 Source
                    if (!iconUrl.isNullOrBlank() && source.iconUrl != iconUrl) {
                        sourceDao.update(source.copy(iconUrl = iconUrl))
                    }
                }

                if (items.isNotEmpty()) {
                    // 获取插入前的文章数量
                    val beforeCount = articleDao.getArticleCountBySource(source.name)

                    // 插入文章
                    articleDao.insertArticles(items)

                    // 获取插入后的文章数量
                    val afterCount = articleDao.getArticleCountBySource(source.name)

                    val newCount = afterCount - beforeCount
                    LogUtils.log("新增文章：$newCount 篇")
                    return newCount
                }
            } else {
                 LogUtils.log("加载失败：HTTP状态：${response.code}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogUtils.log("同步失败 [${source.name}]: ${e.message}")
            Log.e("NewsReader", "同步失败", e)
            throw e  // 抛出异常,让上层处理
        }
        val endTime = System.currentTimeMillis()
        LogUtils.log("解析完成：${endTime - startTime}毫秒")
        return 0
    }

    private fun requestClient(): OkHttpClient {
        val timeoutSeconds = settingsManager.getSourceTimeoutSeconds().toLong()
        return client.newBuilder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    private fun parsePlinkFeedGroups(html: String): List<FeedCatalogGroup> {
        val doc = Jsoup.parse(html, PLINK_URL)
        val feedsContainer = doc.selectFirst(".feeds") ?: return emptyList()

        return feedsContainer.children()
            .filter { it.hasClass("feed-group") }
            .mapNotNull { groupElement ->
                val groupName = groupElement.selectFirst("h3")?.text()?.trim().orEmpty()
                val feeds = groupElement.select(".feed-list a[href]")
                    .mapNotNull { link ->
                        val name = link.text().trim()
                        val url = link.absUrl("href").ifBlank { link.attr("href").trim() }
                        if (name.isBlank() || url.isBlank()) {
                            null
                        } else {
                            FeedCatalogSource(name = name, url = url)
                        }
                    }

                if (groupName.isBlank() || feeds.isEmpty()) {
                    null
                } else {
                    FeedCatalogGroup(name = groupName, feeds = feeds)
                }
            }
    }

    private fun parseAwesomeRssHubFeedGroups(opml: String): List<FeedCatalogGroup> {
        val doc = Jsoup.parse(opml, AWESOME_RSSHUB_ROUTES_OPML_URL, Parser.xmlParser())
        val body = doc.selectFirst("body") ?: return emptyList()

        return body.children()
            .filter { it.tagName().equals("outline", ignoreCase = true) }
            .mapNotNull { groupElement ->
                val groupName = readOutlineTitle(groupElement)
                val feeds = groupElement.children()
                    .filter { it.tagName().equals("outline", ignoreCase = true) }
                    .mapNotNull { feedElement ->
                        val name = readOutlineTitle(feedElement)
                        val url = feedElement.attr("xmlUrl").ifBlank { feedElement.attr("xmlurl") }.trim()
                        if (name.isBlank() || url.isBlank()) {
                            null
                        } else {
                            FeedCatalogSource(name = name, url = url)
                        }
                    }

                if (groupName.isBlank() || feeds.isEmpty()) {
                    null
                } else {
                    FeedCatalogGroup(name = groupName, feeds = feeds)
                }
            }
    }

    private fun parseTopRssListFeedGroups(markdown: String): List<FeedCatalogGroup> {
        val groups = mutableListOf<FeedCatalogGroup>()
        var currentGroupName: String? = null
        val currentFeeds = mutableListOf<FeedCatalogSource>()
        var inTable = false

        fun flushGroup() {
            val groupName = currentGroupName.orEmpty().trim()
            if (groupName.isNotBlank() && currentFeeds.isNotEmpty()) {
                groups += FeedCatalogGroup(name = groupName, feeds = currentFeeds.toList())
            }
            currentFeeds.clear()
            inTable = false
        }

        markdown.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("## ") || line.startsWith("### ") -> {
                    flushGroup()
                    currentGroupName = line.removePrefix("## ").removePrefix("### ").trim()
                }

                currentGroupName != null && line.startsWith("|") -> {
                    val cells = parseMarkdownTableRow(line)
                    if (cells.size < 2) {
                        return@forEach
                    }

                    if (isMarkdownTableSeparator(cells)) {
                        return@forEach
                    }

                    if (cells[0] == "名称" && cells[1] == "源") {
                        inTable = true
                        return@forEach
                    }

                    if (!inTable) {
                        return@forEach
                    }

                    val url = extractMarkdownLinkTarget(cells[1])
                    if (url.isBlank()) {
                        return@forEach
                    }

                    val title = normalizeCatalogSourceName(cells[0], url)
                    currentFeeds += FeedCatalogSource(name = title, url = url)
                }
            }
        }

        flushGroup()
        return groups
    }

    private fun parseWechat2RssFeedGroups(html: String): List<FeedCatalogGroup> {
        val doc = Jsoup.parse(html, WECHAT2RSS_URL)
        val contentRoot = doc.selectFirst(".vp-doc._list_all > div") ?: return emptyList()
        val groups = mutableListOf<FeedCatalogGroup>()
        var currentGroupName: String? = null
        val currentFeeds = mutableListOf<FeedCatalogSource>()

        fun flushGroup() {
            val groupName = currentGroupName.orEmpty().trim()
            if (groupName.isNotBlank() && currentFeeds.isNotEmpty()) {
                groups += FeedCatalogGroup(name = groupName, feeds = currentFeeds.toList())
            }
            currentFeeds.clear()
        }

        contentRoot.children().forEach { element ->
            if (element.tagName().equals("h2", ignoreCase = true)) {
                flushGroup()
                currentGroupName = element.ownText()
                    .ifBlank { element.text() }
                    .replace("\u200B", "")
                    .trim()
                return@forEach
            }

            if (currentGroupName.isNullOrBlank()) {
                return@forEach
            }

            element.select("a[href^=https://wechat2rss.xlab.app/feed/][href$=.xml]")
                .mapNotNull { link ->
                    val url = link.absUrl("href").ifBlank { link.attr("href").trim() }
                    if (url.isBlank()) {
                        null
                    } else {
                        FeedCatalogSource(
                            name = normalizeCatalogSourceName(link.text(), url),
                            url = url
                        )
                    }
                }
                .forEach(currentFeeds::add)
        }

        flushGroup()
        return groups
    }

    private fun readOutlineTitle(element: org.jsoup.nodes.Element): String {
        return element.attr("title")
            .ifBlank { element.attr("text") }
            .trim()
    }

    private fun parseMarkdownTableRow(line: String): List<String> {
        return line.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split("|")
            .map { it.trim() }
    }

    private fun isMarkdownTableSeparator(cells: List<String>): Boolean {
        val separatorRegex = Regex(":?-{3,}:?")
        return cells.all { cell ->
            val normalized = cell.replace(" ", "")
            normalized.isNotBlank() && separatorRegex.matches(normalized)
        }
    }

    private fun extractMarkdownLinkTarget(cell: String): String {
        val markdownLink = Regex("\\[[^\\]]*]\\((https?://[^)]+)\\)")
            .find(cell)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()

        if (markdownLink.isNotBlank()) {
            return markdownLink.trim()
        }

        return Regex("https?://[^\\s)]+")
            .find(cell)
            ?.value
            .orEmpty()
            .trim()
    }

    private fun normalizeCatalogSourceName(rawName: String, url: String): String {
        val name = rawName
            .replace(Regex("\\[([^\\]]*)]\\(([^)]+)\\)"), "$1")
            .trim()

        if (name.isNotBlank()) {
            return name
        }

        return runCatching { URI(url).host.orEmpty().removePrefix("www.") }
            .getOrDefault("")
            .ifBlank { url }
    }

    private suspend fun fetchFeedCatalogGroups(
        requestUrl: String,
        sourceName: String,
        parser: (String) -> List<FeedCatalogGroup>
    ): List<FeedCatalogGroup> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(requestUrl)
                .header("User-Agent", UA_ANDROID)
                .build()

            val response = executeRequest(request)
            val responseString = response.body

            if (!response.isSuccessful || responseString.isNullOrBlank()) {
                throw IOException("$sourceName 页面加载失败：HTTP ${response.code}")
            }

            val groups = parser(responseString)
            if (groups.isEmpty()) {
                throw IOException("$sourceName 中未解析到订阅源")
            }

            groups
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("NewsRepository", "加载 $sourceName 订阅市场失败", e)
            throw e
        }
    }

    private suspend fun executeRequest(request: Request): HttpResponsePayload {
        return suspendCancellableCoroutine { continuation ->
            val call = requestClient().newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        continuation.resume(
                            HttpResponsePayload(
                                code = response.code,
                                isSuccessful = response.isSuccessful,
                                body = response.body?.string()
                            )
                        )
                    }
                }
            })
        }
    }
}
