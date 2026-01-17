package com.lengyuefenghua.newsreader.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONTokener
import kotlin.coroutines.resume

/**
 * 全局 WebView 管理器
 * 用于后台任务（抓取、调试），避免重复创建 WebView 带来的开销。
 */
object WebViewManager {

    private var backgroundWebView: WebView? = null

    // 互斥锁：确保同一时间只有一个任务在使用这个全局 WebView
    private val mutex = Mutex()
    private val mainScope = MainScope()

    /**
     * 在 Application 启动时初始化
     */
    fun init(context: Context) {
        mainScope.launch(Dispatchers.Main) {
            try {
                if (backgroundWebView == null) {
                    // 使用 Application Context 创建，避免内存泄漏
                    backgroundWebView = WebView(context.applicationContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.blockNetworkImage = true // 后台抓取不需要加载图片，提速省流
                        settings.mixedContentMode =
                            android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        // 关键：即使不可见，也要强行设置布局参数，否则某些网页 JS 不会执行
                        layout(0, 0, 1080, 1920)
                    }
                    Log.d("WebViewManager", "全局后台 WebView 初始化成功")
                }
            } catch (e: Exception) {
                Log.e("WebViewManager", "WebView 初始化失败: ${e.message}")
            }
        }
    }

    /**
     * 借用全局 WebView 抓取网页源码
     * 自动处理：线程切换、互斥锁、超时、UserAgent 切换
     */
    suspend fun fetchHtml(url: String, userAgent: String): String {
        // 1. 获取锁 (如果别人正在用，这里会挂起等待)
        return mutex.withLock {
            // 2. 增加超时保护 (30秒)
            val result = withTimeoutOrNull(30_000L) {
                performFetch(url, userAgent)
            }
            // 3. 任务结束后，重置 WebView (加载空页面)，防止残留 JS 影响下一次任务
            cleanup()

            result ?: "ERROR: Timeout (30s)"
        }
    }

    private suspend fun performFetch(url: String, ua: String): String =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val webView = backgroundWebView ?: run {
                    continuation.resume("ERROR: WebView 未初始化")
                    return@suspendCancellableCoroutine
                }

                var isResumed = false
                fun safeResume(result: String) {
                    if (!isResumed && continuation.isActive) {
                        isResumed = true
                        continuation.resume(result)
                    }
                }

                // 配置本次请求的参数
                webView.settings.userAgentString = ua

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // 延迟 2 秒等待动态渲染 (如 36Kr)
                        Handler(Looper.getMainLooper()).postDelayed({
                            view?.evaluateJavascript("(function(){return document.documentElement.outerHTML})();") { value ->
                                try {
                                    val rawHtml = JSONTokener(value).nextValue().toString()
                                    safeResume(rawHtml)
                                } catch (_: Exception) {
                                    safeResume(value ?: "")
                                }
                            }
                        }, 2000)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            Log.e("WebViewManager", "加载出错: ${error?.description}")
                            // 这里不立即 resume error，因为有些网站 404 也会触发 error 但内容是有的
                            // 我们选择等待 onPageFinished 或超时
                        }
                    }
                }

                Log.d("WebViewManager", "开始抓取: $url")
                webView.loadUrl(url)
            }
        }

    private suspend fun cleanup() = withContext(Dispatchers.Main) {
        // 加载空页面，停止之前的 JS 执行，为下一次任务做清理
        backgroundWebView?.loadUrl("about:blank")
    }
}