package com.lengyuefenghua.newsreader.utils

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * WebView 内容提取工具（新方案）
 * 使用 PageLoader + WebExtractor 统一接口
 */
object WebViewContentExtractor {
    internal fun resetTimer(nowMs: Long = System.currentTimeMillis()): WebExtractionTrace {
        return startWebExtractionTrace(nowMs)
    }

    /**
     * 打印耗时统计
     */
    internal fun printTimingStats(stage: String, trace: WebExtractionTrace, nowMs: Long = System.currentTimeMillis()) {
        trace.totalElapsedMs(nowMs)
    }

    /**
     * 注入 PageLoader 脚本
     * 在 onPageStarted 时调用
     */
    internal suspend fun injectLoaderScript(
        context: Context,
        webView: WebView?,
        trace: WebExtractionTrace,
    ): WebExtractionTrace {
        webView ?: return trace
        try {
            val (script, fromCache) = loadScript(context, "algorithm/loader.js")
            webView.evaluateJavascript(script, null)
            val updatedTrace = trace.markLoaderInjected(System.currentTimeMillis())
            updatedTrace.loaderElapsedMs()
            fromCache
            return updatedTrace
        } catch (e: Exception) {
            return trace
        }
    }

    /**
     * 启动 PageLoader 监听
     * 在 onPageFinished 时调用
     */
    suspend fun startPageLoader(webView: WebView?) {
        webView ?: return
        try {
            val command = "PageLoader.start({timeout: 30000});"
            webView.evaluateJavascript(command, null)
        } catch (e: Exception) {
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
    internal suspend fun extractContent(
        view: WebView?,
        trace: WebExtractionTrace,
        algorithm: String = "readability",
        selector: String = ""
    ): WebExtractionTrace = withContext(Dispatchers.Main) {
        view ?: return@withContext trace

        try {
            val extractingTrace = trace.markExtractionStarted(System.currentTimeMillis())

            val context = view.context ?: throw IllegalStateException("WebView context is null")

            // 1. 注入 Readability-android.js
            injectScript(context, view, "algorithm/Readability-android.js")

            // 2. 注入 gne-custom.js
            injectScript(context, view, "algorithm/gne-custom.js")

            // 3. 注入 main.js (WebExtractor)
            injectScript(context, view, "algorithm/main.js")

            val updatedTrace = extractingTrace.markScriptsInjected(System.currentTimeMillis())
            updatedTrace.scriptsElapsedMs()

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

            view.evaluateJavascript(command, null)
            return@withContext updatedTrace

        } catch (e: Exception) {
            return@withContext trace
        }
    }

    /**
     * 注入单个脚本文件
     */
    private suspend fun injectScript(context: Context, webView: WebView, scriptPath: String) {
        try {
            val injectStart = System.currentTimeMillis()
            val (script, fromCache) = loadScript(context, scriptPath)
            webView.evaluateJavascript(script, null)
            val elapsed = System.currentTimeMillis() - injectStart
            elapsed
            fromCache
        } catch (e: Exception) {
        }
    }

    private suspend fun loadScript(context: Context, scriptPath: String): Pair<String, Boolean> {
        return withContext(Dispatchers.IO) {
            WebViewScriptCache.getOrPut(scriptPath) {
                context.assets.open(scriptPath).bufferedReader().use { it.readText() }
            }
        }
    }
}

internal data class WebExtractionTrace(
    val startedAtMs: Long,
    val loaderInjectedAtMs: Long? = null,
    val extractionStartedAtMs: Long? = null,
    val scriptsInjectedAtMs: Long? = null,
)

internal fun startWebExtractionTrace(nowMs: Long = System.currentTimeMillis()): WebExtractionTrace {
    return WebExtractionTrace(startedAtMs = nowMs)
}

internal fun WebExtractionTrace.markLoaderInjected(nowMs: Long): WebExtractionTrace {
    return copy(loaderInjectedAtMs = nowMs)
}

internal fun WebExtractionTrace.markExtractionStarted(nowMs: Long): WebExtractionTrace {
    return copy(extractionStartedAtMs = nowMs)
}

internal fun WebExtractionTrace.markScriptsInjected(nowMs: Long): WebExtractionTrace {
    return copy(scriptsInjectedAtMs = nowMs)
}

internal fun WebExtractionTrace.totalElapsedMs(nowMs: Long): Long {
    return (nowMs - startedAtMs).coerceAtLeast(0L)
}

internal fun WebExtractionTrace.loaderElapsedMs(): Long {
    return ((loaderInjectedAtMs ?: startedAtMs) - startedAtMs).coerceAtLeast(0L)
}

internal fun WebExtractionTrace.extractionStartedFromStartMs(): Long {
    return ((extractionStartedAtMs ?: startedAtMs) - startedAtMs).coerceAtLeast(0L)
}

internal fun WebExtractionTrace.scriptsElapsedMs(): Long {
    val start = extractionStartedAtMs ?: startedAtMs
    val end = scriptsInjectedAtMs ?: start
    return (end - start).coerceAtLeast(0L)
}

internal fun WebExtractionTrace.extractionElapsedMs(nowMs: Long): Long {
    val start = extractionStartedAtMs ?: startedAtMs
    return (nowMs - start).coerceAtLeast(0L)
}

internal object WebViewScriptCache {
    private val scripts = ConcurrentHashMap<String, String>()

    fun getOrPut(scriptPath: String, loader: () -> String): Pair<String, Boolean> {
        scripts[scriptPath]?.let { return it to true }

        val loadedScript = loader()
        val cachedScript = scripts.putIfAbsent(scriptPath, loadedScript)
        return if (cachedScript == null) {
            loadedScript to false
        } else {
            cachedScript to true
        }
    }

    fun clear() {
        scripts.clear()
    }
}
