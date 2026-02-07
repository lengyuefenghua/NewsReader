package com.lengyuefenghua.newsreader.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.util.Patterns
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.R
import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.utils.WebViewContentExtractor
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    article: Article?,
    onBack: () -> Unit,
    onMarkRead: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditSource: (String) -> Unit,
    onUpdateReadDuration: (String, Long) -> Unit // [新增] 更新时长回调
) {
    if (article == null) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasMarkedRead by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf<Source?>(null) }
    // viewMode 需要根据 source 动态计算
    var viewMode by remember { mutableStateOf(
        // 初始值：如果有正文使用内容模式，否则网页模式
        if (article.content != null) 0 else 1
    ) }
    var isTranslationEnabled by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    // [新增] 加载进度
    var loadingProgress by remember { mutableStateOf(0f) }
    // [新增] 正在加载标志（用于控制遮罩层显示）
    var isLoading by remember { mutableStateOf(false) }
    // [新增] 记录上次的 viewMode，用于检测模式切换
    var lastViewMode by remember { mutableStateOf(-1) }

    // Pre-load content description strings
    val descMenuMore = context.getString(R.string.desc_menu_more)
    val descEdit = context.getString(R.string.desc_edit)
    val descShare = context.getString(R.string.desc_share)
    val descButtonOpenBrowser = context.getString(R.string.desc_button_open_browser)
    val descWebview = context.getString(R.string.desc_webview)
    val descLoading = context.getString(R.string.desc_loading)

    // [新增] 阅读计时器
    DisposableEffect(article.id) {
        val startTime = System.currentTimeMillis()
        onDispose {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            // 只有停留超过 2 秒才计入有效阅读时间，避免误触
            if (duration > 2000) {
                onUpdateReadDuration(article.id, duration)
            }
        }
    }

    // [新增] 获取订阅源信息（用于判断是否启用正文提取）
    LaunchedEffect(article.sourceName) {
        try {
            val application = context.applicationContext as com.lengyuefenghua.newsreader.NewsReaderApplication
            val sourceDao = application.database.sourceDao()
            val sources = sourceDao.getAllSources().first()
            source = sources.find { it.name == article.sourceName }

            // [统一逻辑] 所有文章默认都是网页模式（viewMode = 1）
            // 用户可以手动切换到内容模式查看已存储的正文
            viewMode = 1
            android.util.Log.d("ArticleScreen", ">>> 初始化 viewMode = 1（网页模式）")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 页面内容保持不变，仅省略以节省空间...
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            article.title,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            article.sourceName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.desc_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            descMenuMore
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("订阅设置") },
                            onClick = { showMenu = false; onEditSource(article.sourceName) },
                            modifier = Modifier.semantics { contentDescription = descEdit }
                        )
                        DropdownMenuItem(
                            text = { Text("分享链接") },
                            onClick = {
                                showMenu = false
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.url}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "分享到"))
                            },
                            modifier = Modifier.semantics { contentDescription = descShare }
                        )
                        DropdownMenuItem(
                            text = { Text("复制链接") },
                            onClick = {
                                showMenu = false
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("文章链接", article.url)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("浏览器打开") },
                            onClick = {
                                showMenu = false
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                                context.startActivity(browserIntent)
                            },
                            modifier = Modifier.semantics { contentDescription = descButtonOpenBrowser }
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(48.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = NavigationBarDefaults.Elevation,
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewMode = if (viewMode == 0) 1 else 0 },
                        enabled = article.content != null ||
                                  source?.useAutoExtract == true ||
                                  (!source?.ruleContent.isNullOrEmpty())
                    ) {
                        val icon =
                            if (viewMode == 0) Icons.Default.Description else Icons.Default.Public
                        Icon(icon, contentDescription = stringResource(R.string.desc_toggle_mode))
                    }
                    IconButton(onClick = onMarkRead) {
                        Icon(
                            imageVector = if (article.isRead) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = if (article.isRead) stringResource(R.string.desc_mark_read) else stringResource(R.string.desc_mark_unread),
                            tint = if (article.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        if (!isTranslationEnabled) {
                            isTranslationEnabled = true; injectTranslationScript(
                                context,
                                webViewRef
                            ); Toast.makeText(context, "正在启动沉浸式翻译...", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = stringResource(R.string.desc_translate),
                            tint = if (isTranslationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (article.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (article.isFavorite) stringResource(R.string.desc_unfavorite) else stringResource(R.string.desc_favorite),
                            tint = if (article.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        // 设置白色背景
                        setBackgroundColor(android.graphics.Color.WHITE)

                        // [关键] 启用硬件加速图层，防止闪烁
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                        // [新增] 注册 JavaScript 接口（仅用于处理特殊情况）
                        addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun onPageLoadStatus(status: String, data: String) {
                                // PageLoader 的状态现在仅用于日志记录和特殊情况处理
                                when (status) {
                                    "ready" -> {
                                        android.util.Log.d("ArticleScreen", "PageLoader: ready (已忽略，直接提取中)")
                                    }
                                    "captcha_required" -> {
                                        // 需要人机验证
                                        android.util.Log.w("ArticleScreen", "⚠ 需要人机验证")
                                        isLoading = false
                                        Toast.makeText(ctx, "需要完成人机验证", Toast.LENGTH_LONG).show()
                                    }
                                    "waiting" -> {
                                        android.util.Log.d("ArticleScreen", "PageLoader: waiting")
                                    }
                                    "redirecting" -> {
                                        android.util.Log.d("ArticleScreen", "PageLoader: redirecting")
                                    }
                                    "timeout" -> {
                                        android.util.Log.e("ArticleScreen", "PageLoader: timeout (但应该已经直接提取了)")
                                    }
                                    "error" -> {
                                        android.util.Log.e("ArticleScreen", "PageLoader: error: $data")
                                    }
                                }
                            }

                            @android.webkit.JavascriptInterface
                            fun onExtractionStatus(status: String, data: String) {
                                when (status) {
                                    "extraction_complete" -> {
                                        // 提取完成
                                        val completeTime = System.currentTimeMillis()
                                        val totalElapsed = completeTime - com.lengyuefenghua.newsreader.utils.WebViewContentExtractor.startTime
                                        val extractionElapsed = completeTime - com.lengyuefenghua.newsreader.utils.WebViewContentExtractor.extractionStartedTime
                                        android.util.Log.d("ArticleScreen", "========================================")
                                        android.util.Log.d("ArticleScreen", "✓✓✓ 正文提取完成！")
                                        android.util.Log.d("ArticleScreen", "    总耗时: ${totalElapsed}ms")
                                        android.util.Log.d("ArticleScreen", "    提取阶段耗时: ${extractionElapsed}ms")
                                        android.util.Log.d("ArticleScreen", "========================================")

                                        // 延迟隐藏遮罩层，等待 DOM 更新完成
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(300) // 延迟 300ms
                                            isLoading = false
                                            loadingProgress = 1f
                                            android.util.Log.d("ArticleScreen", ">>> 遮罩层已隐藏")
                                        }
                                    }
                                    "extraction_error" -> {
                                        // 提取失败
                                        android.util.Log.e("ArticleScreen", "✗✗✗ 正文提取失败: $data")
                                        isLoading = false
                                        Toast.makeText(ctx, "正文提取失败", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }, "AndroidInterface")

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress / 100f
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                android.util.Log.d("ArticleScreen", ">>> onPageStarted: $url")

                                // 立即显示遮罩层，防止原始网页闪现
                                if (source?.useAutoExtract == true && viewMode == 1) {
                                    isLoading = true
                                    loadingProgress = 0f
                                }

                                // 重置计时器并注入 PageLoader 脚本
                                WebViewContentExtractor.resetTimer()
                                coroutineScope.launch {
                                    WebViewContentExtractor.injectLoaderScript(ctx, view)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val pageLoadTime = System.currentTimeMillis() - WebViewContentExtractor.startTime
                                android.util.Log.d("ArticleScreen", ">>> onPageFinished: $url (页面加载耗时: ${pageLoadTime}ms)")

                                // 直接开始提取，不等待 PageLoader 的 ready 回调
                                if (source?.useAutoExtract == true && viewMode == 1) {
                                    android.util.Log.d("ArticleScreen", ">>> 立即开始提取（跳过PageLoader等待）")
                                    val algorithm = source?.extractionAlgorithm ?: "readability"
                                    val selector = source?.ruleContent ?: ""
                                    coroutineScope.launch {
                                        WebViewContentExtractor.extractContent(
                                            view = webViewRef,
                                            algorithm = algorithm,
                                            selector = selector
                                        )
                                    }
                                } else {
                                    // 不需要提取，直接隐藏遮罩层
                                    isLoading = false
                                    loadingProgress = 1f
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                                // 只在提取模式下拦截资源
                                if (!(source?.useAutoExtract == true && viewMode == 1)) {
                                    return super.shouldInterceptRequest(view, request)
                                }

                                val lowerUrl = url.lowercase()

                                // 1. 完全不拦截图片 - 所有图片都允许加载
                                if (lowerUrl.endsWith(".png") || lowerUrl.endsWith(".jpg") ||
                                    lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".gif") ||
                                    lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".svg")) {
                                    return super.shouldInterceptRequest(view, request)
                                }

                                // 2. 拦截明确的广告脚本
                                val adDomains = listOf(
                                    "doubleclick.net",
                                    "googleadservices.com",
                                    "googlesyndication.com",
                                    "facebook.com/tr/",
                                    "amazon-adsystem.com",
                                    "adserver",
                                    "advertising"
                                )

                                if (adDomains.any { lowerUrl.contains(it) }) {
                                    android.util.Log.d("ArticleScreen", "🚫 拦截广告: ${url.take(100)}")
                                    return createEmptyResponse()
                                }

                                // 3. 拦截弹窗脚本
                                if (lowerUrl.contains("popup") || lowerUrl.contains("modal")) {
                                    android.util.Log.d("ArticleScreen", "🚫 拦截弹窗: ${url.take(100)}")
                                    return createEmptyResponse()
                                }

                                // 4. 拦截字体文件（加速加载）
                                if (lowerUrl.endsWith(".woff") || lowerUrl.endsWith(".woff2") ||
                                    lowerUrl.endsWith(".ttf") || lowerUrl.endsWith(".eot")) {
                                    android.util.Log.d("ArticleScreen", "🔤 拦截字体: ${url.take(100)}")
                                    return createEmptyResponse()
                                }

                                // 5. 拦截视频文件
                                if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".webm") ||
                                    lowerUrl.endsWith(".avi") || lowerUrl.endsWith(".mov")) {
                                    android.util.Log.d("ArticleScreen", "🎬 拦截视频: ${url.take(100)}")
                                    return createEmptyResponse()
                                }

                                // 其他资源全部允许加载
                                return super.shouldInterceptRequest(view, request)
                            }
                        }
                        setOnScrollChangeListener { _, _, t, _, _ ->
                            if (t > 50 && !hasMarkedRead && !article.isRead) {
                                hasMarkedRead = true
                                onMarkRead()
                            }
                        }
                        webViewRef = this
                    }
                },
                update = { webView ->
                    // 检测模式切换
                    val viewModeChanged = lastViewMode != viewMode
                    if (viewModeChanged) {
                        lastViewMode = viewMode
                        android.util.Log.d("ArticleScreen", ">>> viewMode 切换: $lastViewMode -> $viewMode")
                    }

                    if (viewMode == 0 && article.content != null) {
                        // 内容模式：不显示遮罩层
                        isLoading = false
                        val htmlData = """
                            <html><head><meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>body { font-family: sans-serif; line-height: 1.6; padding: 16px; color: #333; word-wrap: break-word; } img { max-width: 100%; height: auto; border-radius: 8px; margin: 10px 0; } p { margin-bottom: 16px; } a { color: #007AFF; text-decoration: none; }</style>
                            </head><body><h3>${article.title}</h3>${article.content}<div style="height: 200px;"></div></body></html>
                        """.trimIndent()
                        if (webView.url != "about:blank") webView.loadDataWithBaseURL(
                            null,
                            htmlData,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    } else {
                        // 网页模式：加载原始网页并提取
                        // 如果 URL 不匹配，或者模式刚切换到网页模式，则重新加载
                        val needReload = webView.url != article.url || viewModeChanged

                        if (needReload) {
                            // 在加载 URL 之前立即显示遮罩层，防止原始网页闪现
                            if (source?.useAutoExtract == true && viewMode == 1) {
                                isLoading = true
                                loadingProgress = 0f
                                android.util.Log.d("ArticleScreen", ">>> 网页模式：开始加载并提取")
                            } else {
                                isLoading = false
                                android.util.Log.d("ArticleScreen", ">>> 网页模式：加载原始网页（不提取）")
                            }
                            webView.loadUrl(article.url)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = descWebview }
            )

            // 加载遮罩层（白色背景 + 顶部进度条）
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.White)
                ) {
                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .semantics { contentDescription = descLoading }
                    )
                }
            }
        }
    }
}

// ========== 资源拦截相关 ==========

/**
 * 资源类型枚举
 */
private enum class ResourceType {
    HTML,          // HTML 文档
    SCRIPT,        // JavaScript
    STYLESHEET,    // CSS 样式
    IMAGE,         // 图片
    VIDEO,         // 视频
    FONT,          // 字体
    AD,            // 广告
    OTHER          // 其他
}

/**
 * 判断资源类型
 */
private fun getResourceType(url: String): ResourceType {
    val lowerUrl = url.lowercase()

    // 广告域名和关键词
    val adPatterns = listOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "facebook.com/tr/", "facebook.com/*/ads", "amazon-adsystem.com",
        "adserver", "advertising", "banner", "popup", "popunder"
    )

    // 检查是否为广告
    if (adPatterns.any { lowerUrl.contains(it) }) {
        return ResourceType.AD
    }

    // 根据文件扩展名判断
    return when {
        lowerUrl.endsWith(".html") || lowerUrl.endsWith(".htm") || lowerUrl.endsWith("/") -> ResourceType.HTML
        lowerUrl.endsWith(".js") -> ResourceType.SCRIPT
        lowerUrl.endsWith(".css") -> ResourceType.STYLESHEET
        lowerUrl.endsWith(".png") || lowerUrl.endsWith(".jpg") ||
        lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".gif") ||
        lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".svg") -> ResourceType.IMAGE
        lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".webm") ||
        lowerUrl.endsWith(".avi") || lowerUrl.endsWith(".mov") -> ResourceType.VIDEO
        lowerUrl.endsWith(".woff") || lowerUrl.endsWith(".woff2") ||
        lowerUrl.endsWith(".ttf") || lowerUrl.endsWith(".eot") -> ResourceType.FONT
        lowerUrl.contains("image") || lowerUrl.contains("img") ||
        lowerUrl.contains("photo") || lowerUrl.contains("pic") -> ResourceType.IMAGE
        lowerUrl.contains("video") || lowerUrl.contains("media") -> ResourceType.VIDEO
        lowerUrl.contains("font") -> ResourceType.FONT
        else -> ResourceType.OTHER
    }
}

/**
 * 判断是否为广告相关资源
 */
private fun isAdRelated(url: String): Boolean {
    val lowerUrl = url.lowercase()
    val adKeywords = listOf(
        "ad", "ads", "advertisement", "banner", "sponsor",
        "tracking", "analytics", "telemetry", "pixel"
    )
    return adKeywords.any { lowerUrl.contains(it) }
}

/**
 * 创建空的资源响应（用于拦截）
 */
private fun createEmptyResponse(): WebResourceResponse {
    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
}

private fun injectTranslationScript(context: Context, webView: WebView?) {
    if (webView == null) return
    try {
        val inputStream: InputStream = context.assets.open("immersive_translate.js")
        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        inputStream.close()
        val script = String(buffer)
        webView.evaluateJavascript("(function() { $script })(); void(0);", null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
