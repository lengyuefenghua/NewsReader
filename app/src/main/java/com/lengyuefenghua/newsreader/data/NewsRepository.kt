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

class NewsRepository(private val database: AppDatabase) {

    private val sourceDao = database.sourceDao()
    private val articleDao = database.articleDao()

    private val client = OkHttpClient()
    private val rssParser = RssParser()
    private val htmlParser = HtmlParser()

    // UI 层直接观察这个 Flow，数据库有变动会自动通知
    val allArticles: Flow<List<Article>> = articleDao.getAllArticlesFlow()

    companion object {
        const val UA_ANDROID = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        const val UA_PC = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    /**
     * 触发一次全量同步：
     * 1. 获取所有源
     * 2. 并发抓取
     * 3. 存入数据库
     */
    suspend fun syncAll() = withContext(Dispatchers.IO) {
        // 获取当前订阅源快照 (使用 first() 取一次流的当前值)
        val sources = sourceDao.getAllSources().first()

        // 并发执行抓取任务
        val deferredResults = sources.map { source ->
            async {
                fetchAndSave(source)
            }
        }
        deferredResults.awaitAll()
    }

    /**
     * 同步单个源（可用于添加新源时立即刷新）
     */
    suspend fun syncSource(sourceId: Int) = withContext(Dispatchers.IO) {
        val source = sourceDao.getSourceById(sourceId) // 假设 SourceDao 有这个方法，如果没有可以略过，或者用 getAllSources 过滤
        if (source != null) {
            fetchAndSave(source)
        }
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
                    // 保存到数据库 (Insert OnConflictStrategy.IGNORE 会自动处理重复文章)
                    articleDao.insertArticles(items)
                    Log.d("NewsReader", "入库成功: ${source.name} 更新 ${items.size} 条")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsReader", "同步失败 [${source.name}]: ${e.message}")
        }
    }
}