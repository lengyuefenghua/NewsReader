package com.lengyuefenghua.newsreader.utils

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WebView 内容提取工具（新方案）
 * 使用 PageLoader + WebExtractor 统一接口
 */
object WebViewContentExtractor {
    // 记录各个阶段的时间戳
    var startTime: Long = 0
    private var loaderInjectedTime: Long = 0
    private var pageLoaderStartedTime: Long = 0
    var readyReceivedTime: Long = 0
    private var scriptsInjectedTime: Long = 0
    var extractionStartedTime: Long = 0

    /**
     * 重置计时器
     */
    fun resetTimer() {
        startTime = System.currentTimeMillis()
        loaderInjectedTime = 0
        pageLoaderStartedTime = 0
        readyReceivedTime = 0
        scriptsInjectedTime = 0
        extractionStartedTime = 0
        android.util.Log.d("WebViewContentExtractor", "=== 性能计时开始 ===")
    }

    /**
     * 打印耗时统计
     */
    fun printTimingStats(stage: String) {
        val now = System.currentTimeMillis()
        val total = now - startTime
        android.util.Log.d("WebViewContentExtractor", "[$stage] 耗时: ${total}ms")
    }

    /**
     * 注入 PageLoader 脚本
     * 在 onPageStarted 时调用
     */
    suspend fun injectLoaderScript(context: Context, webView: WebView?) {
        webView ?: return
        try {
            val stepStart = System.currentTimeMillis()
            val script = withContext(Dispatchers.IO) {
                context.assets.open("algorithm/loader.js").bufferedReader().use { it.readText() }
            }
            webView.evaluateJavascript(script, null)
            loaderInjectedTime = System.currentTimeMillis()
            val elapsed = loaderInjectedTime - startTime
            android.util.Log.d("WebViewContentExtractor", "✓ PageLoader injected (耗时: ${elapsed}ms, 从开始: ${elapsed}ms)")
        } catch (e: Exception) {
            android.util.Log.e("WebViewContentExtractor", "Failed to inject loader.js", e)
        }
    }

    /**
     * 启动 PageLoader 监听
     * 在 onPageFinished 时调用
     */
    suspend fun startPageLoader(webView: WebView?) {
        webView ?: return
        try {
            pageLoaderStartedTime = System.currentTimeMillis()
            val elapsedFromStart = pageLoaderStartedTime - startTime
            val elapsedFromLoaderInject = pageLoaderStartedTime - loaderInjectedTime
            android.util.Log.d("WebViewContentExtractor", "✓ PageLoader starting (从onPageStarted: ${elapsedFromLoaderInject}ms, 总耗时: ${elapsedFromStart}ms)")

            val command = "PageLoader.start({timeout: 30000});"
            webView.evaluateJavascript(command, null)
        } catch (e: Exception) {
            android.util.Log.e("WebViewContentExtractor", "Failed to start PageLoader", e)
        }
    }

    /**
     * 注入算法脚本并调用 WebExtractor 提取
     * 在 onPageFinished 后立即调用（跳过 PageLoader 等待）
     *
     * @param view WebView 实例
     * @param algorithm 提取算法 ("readability", "gne", "custom")
     * @param selector CSS 选择器（仅当 algorithm="custom" 时使用）
     */
    suspend fun extractContent(
        view: WebView?,
        algorithm: String = "readability",
        selector: String = ""
    ) = withContext(Dispatchers.Main) {
        view ?: return@withContext

        try {
            extractionStartedTime = System.currentTimeMillis()
            val elapsedFromStart = extractionStartedTime - startTime
            android.util.Log.d("WebViewContentExtractor", ">>> 开始提取内容 (从onPageStarted: ${elapsedFromStart}ms)")
            android.util.Log.d("WebViewContentExtractor", "    使用算法: $algorithm, 选择器: '$selector'")

            val context = view.context ?: throw IllegalStateException("WebView context is null")

            // 1. 注入 Readability-android.js
            injectScript(context, view, "algorithm/Readability-android.js")

            // 2. 注入 gne-custom.js
            injectScript(context, view, "algorithm/gne-custom.js")

            // 3. 注入 main.js (WebExtractor)
            injectScript(context, view, "algorithm/main.js")

            scriptsInjectedTime = System.currentTimeMillis()
            val scriptsElapsed = scriptsInjectedTime - extractionStartedTime
            android.util.Log.d("WebViewContentExtractor", "✓ 所有脚本注入完成 (耗时: ${scriptsElapsed}ms)")

            // 4. 调用 WebExtractor.extractAndNotify()
            val command = when {
                algorithm == "custom" && selector.isNotEmpty() -> {
                    // CSS 选择器模式
                    """WebExtractor.keepOnly({selector: '$selector'});"""
                }
                algorithm == "readability" -> {
                    // Readability 算法
                    """WebExtractor.extractAndNotify({algorithm: 'readability', addResponsiveStyle: true});"""
                }
                algorithm == "gne" -> {
                    // GNE 算法
                    """WebExtractor.extractAndNotify({algorithm: 'gne', addResponsiveStyle: true});"""
                }
                else -> {
                    // 默认使用 Readability
                    """WebExtractor.extractAndNotify({algorithm: 'readability', addResponsiveStyle: true});"""
                }
            }

            android.util.Log.d("WebViewContentExtractor", ">>> 执行提取命令: $command")
            view.evaluateJavascript(command, null)

        } catch (e: Exception) {
            android.util.Log.e("WebViewContentExtractor", "Failed to extract content", e)
        }
    }

    /**
     * 注入单个脚本文件
     */
    private suspend fun injectScript(context: Context, webView: WebView, scriptPath: String) {
        try {
            val injectStart = System.currentTimeMillis()
            val script = withContext(Dispatchers.IO) {
                context.assets.open(scriptPath).bufferedReader().use { it.readText() }
            }
            webView.evaluateJavascript(script, null)
            val elapsed = System.currentTimeMillis() - injectStart
            android.util.Log.d("WebViewContentExtractor", "  ✓ 注入 $scriptPath (耗时: ${elapsed}ms)")
            // 移除延迟，加快速度
            // kotlinx.coroutines.delay(100)
        } catch (e: Exception) {
            android.util.Log.e("WebViewContentExtractor", "Failed to inject: $scriptPath", e)
        }
    }
}
