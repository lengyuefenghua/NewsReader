package com.lengyuefenghua.newsreader.data

import android.util.Log
import com.lengyuefenghua.newsreader.core.domain.model.NetworkError
import com.lengyuefenghua.newsreader.core.domain.model.Result
import com.lengyuefenghua.newsreader.utils.HtmlParser
import com.lengyuefenghua.newsreader.utils.LogUtils
import com.lengyuefenghua.newsreader.utils.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.atomic.AtomicInteger

class NewsRepository(private val database: AppDatabase) {

    private val sourceDao = database.sourceDao()
    private val articleDao = database.articleDao()

    private val client = OkHttpClient()
    private val rssParser = RssParser()
    private val htmlParser = HtmlParser()

    val allArticles: Flow<List<Article>> = articleDao.getAllArticlesFlow()

    // [新增] 暴露所有 Source 用于获取图标
    fun getAllSources(): Flow<List<Source>> = sourceDao.getAllSources()

    companion object {
        const val UA_ANDROID =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        const val UA_PC =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    // [新增] 获取特定源的文章流
    fun getArticlesBySource(sourceName: String): Flow<List<Article>> {
        return articleDao.getArticlesBySourceFlow(sourceName)
    }

    // [新增] 获取单篇文章流，修复类型不匹配问题
    fun getArticleFlow(url: String): Flow<Article?> {
        return articleDao.getArticleFlow(url)
    }

    // [修改] syncAll 现在接受进度回调，返回 Result
    // onProgress: (finishedCount, totalCount, currentSourceName)
    suspend fun syncAll(onProgress: (Int, Int, String) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sources = sourceDao.getAllSources().first()
            if (sources.isEmpty()) {
                return@withContext Result.Success(Unit)
            }

            val total = sources.size
            val counter = AtomicInteger(0)

            // 初始通知
            onProgress(0, total, "准备开始...")

            val deferredResults = sources.map { source ->
                async {
                    // 开始前通知：正在更新 xxx
                    // 注意：由于并行执行，这里可能会快速刷新，UI 层展示其中一个即可
                    onProgress(counter.get(), total, source.name)

                    fetchAndSave(source)

                    // 完成后增加计数
                    val current = counter.incrementAndGet()
                    onProgress(current, total, source.name)
                }
            }
            deferredResults.awaitAll()

            Result.Success(Unit)
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

    suspend fun syncSource(sourceId: Int) = withContext(Dispatchers.IO) {
        val source = sourceDao.getSourceById(sourceId)
        if (source != null) {
            fetchAndSave(source)
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
        val startTime = System.currentTimeMillis()
        try {
            val currentUserAgent = if (source.enablePcUserAgent) UA_PC else UA_ANDROID

            val request = Request.Builder()
                .url(source.url)
                .header("User-Agent", currentUserAgent)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string()

            if (response.isSuccessful && responseString != null) {
                val items: List<Article> = if (source.isCustom) {
                    htmlParser.parse(responseString, source)
                } else {
                    val result = rssParser.parse(responseString.byteInputStream(), source.name)
                    result.first
                }
                items
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "获取文章失败", e)
            emptyList()
        }
    }

    private suspend fun fetchAndSave(source: Source) {
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

            val response = client.newCall(request).execute()
            val responseString = response.body?.string()

            if (response.isSuccessful && responseString != null) {
                val kbSize = responseString.length / 1024
                LogUtils.log("加载完成：HTTP状态：${response.code}，内容长度：${kbSize}KB")

                var items: List<Article> = emptyList()

                if (source.isCustom) {
                    items = htmlParser.parse(responseString, source)
                } else {
                    val result = rssParser.parse(responseString.byteInputStream(), source.name)
                    items = result.first
                    val iconUrl = result.second

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
                    articleDao.insertArticles(items)
                }
            } else {
                 LogUtils.log("加载失败：HTTP状态：${response.code}")
            }
        } catch (e: Exception) {
            LogUtils.log("同步失败 [${source.name}]: ${e.message}")
            Log.e("NewsReader", "同步失败", e)
        }
        val endTime = System.currentTimeMillis()
        LogUtils.log("解析完成：${endTime - startTime}毫秒")
    }
}