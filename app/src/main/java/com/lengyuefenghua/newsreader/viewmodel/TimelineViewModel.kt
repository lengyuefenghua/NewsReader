package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.utils.HtmlParser
import com.lengyuefenghua.newsreader.utils.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class TimelineViewModel(application: Application) : AndroidViewModel(application) {

    private val sourceDao = (application as NewsReaderApplication).database.sourceDao()
    private val client = OkHttpClient()
    private val rssParser = RssParser()
    private val htmlParser = HtmlParser()

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 定义 UA 常量
    companion object {
        const val UA_ANDROID = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        const val UA_PC = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    init {
        fetchArticles()
    }

    fun fetchArticles() {
        viewModelScope.launch {
            _isLoading.value = true
            sourceDao.getAllSources().collect { sources ->
                val allArticles = mutableListOf<Article>()

                withContext(Dispatchers.IO) {
                    sources.forEach { source ->
                        try {
                            Log.d("NewsReader", "准备抓取: ${source.name} | URL: ${source.url}")

                            // [动态 User-Agent]
                            val currentUserAgent = if (source.enablePcUserAgent) UA_PC else UA_ANDROID

                            val request = Request.Builder()
                                .url(source.url)
                                .header("User-Agent", currentUserAgent)
                                .build()

                            val response = client.newCall(request).execute()
                            val responseString = response.body?.string()

                            Log.d("NewsReader", "请求结束: 状态码=${response.code}")

                            if (response.isSuccessful && responseString != null) {
                                if (source.isCustom) {
                                    val items = htmlParser.parse(responseString, source)
                                    allArticles.addAll(items)
                                } else {
                                    val items = rssParser.parse(responseString.byteInputStream(), source.name)
                                    allArticles.addAll(items)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("NewsReader", "错误: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
                _articles.value = allArticles.shuffled()
                _isLoading.value = false
            }
        }
    }

    fun getArticleByUrl(url: String): Article? {
        return _articles.value.find { it.url == url }
    }
}