package com.lengyuefenghua.newsreader.data

import android.util.Log
import com.lengyuefenghua.newsreader.utils.HtmlParser
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

    companion object {
        const val UA_ANDROID = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        const val UA_PC = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    // [新增] 获取特定源的文章流
    fun getArticlesBySource(sourceName: String): Flow<List<Article>> {
        return articleDao.getArticlesBySourceFlow(sourceName)
    }
    // [修改] syncAll 现在接受进度回调
    // onProgress: (finishedCount, totalCount, currentSourceName)
    suspend fun syncAll(onProgress: (Int, Int, String) -> Unit) = withContext(Dispatchers.IO) {
        val sources = sourceDao.getAllSources().first()
        if (sources.isEmpty()) return@withContext

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

    suspend fun toggleFavorite(id: String, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        articleDao.updateFavorite(id, !currentStatus)
    }
    suspend fun getSourceById(sourceId: Int): Source? = withContext(Dispatchers.IO) {
        sourceDao.getSourceById(sourceId)
    }
    suspend fun getSourceIdByName(name: String): Int? = withContext(Dispatchers.IO) {
        sourceDao.getSourceIdByName(name)
    }

    private suspend fun fetchAndSave(source: Source) {
        try {
            Log.d("NewsReader", "后台同步: ${source.name} | URL: ${source.url}")

            val currentUserAgent = if (source.enablePcUserAgent) UA_PC else UA_ANDROID
            val request = Request.Builder()
                .url(source.url)
                .header("User-Agent", currentUserAgent)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string()

            if (response.isSuccessful && responseString != null) {
                val items = if (source.isCustom) {
                    htmlParser.parse(responseString, source)
                } else {
                    rssParser.parse(responseString.byteInputStream(), source.name)
                }

                if (items.isNotEmpty()) {
                    articleDao.insertArticles(items)
                    Log.d("NewsReader", "入库成功: ${source.name} 更新 ${items.size} 条")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsReader", "同步失败 [${source.name}]: ${e.message}")
        }
    }
}