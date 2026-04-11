package com.lengyuefenghua.newsreader.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.util.Patterns
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.R
import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.utils.WebExtractionTrace
import com.lengyuefenghua.newsreader.utils.WebViewContentExtractor
import com.lengyuefenghua.newsreader.utils.extractionElapsedMs
import com.lengyuefenghua.newsreader.utils.totalElapsedMs
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    article: Article?,
    previousTitle: String?,
    nextTitle: String?,
    previousUrl: String?,
    nextUrl: String?,
    onBack: () -> Unit,
    onMarkRead: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditSource: (String) -> Unit,
    onUpdateReadDuration: (String, Long) -> Unit, // [新增] 更新时长回调
    onOpenAdjacentArticle: (String) -> Unit,
) {
    if (article == null) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasMarkedRead by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf<Source?>(null) }
    // viewMode 需要根据 source 动态计算
    var viewMode by remember { mutableStateOf(
        if (shouldDefaultToWebMode(article, null)) 1 else 0
    ) }
    var isTranslationEnabled by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var articleLoadState by remember(article.id) { mutableStateOf(ArticleLoadState()) }
    var extractionTrace by remember(article.id) { mutableStateOf<WebExtractionTrace?>(null) }
    // [新增] 加载进度
    var loadingProgress by remember { mutableStateOf(0f) }
    // [新增] 正在加载标志（用于控制遮罩层显示）
    var isLoading by remember { mutableStateOf(false) }
    // [新增] 记录上次的 viewMode，用于检测模式切换
    var lastViewMode by remember { mutableStateOf(-1) }
    var isAtTop by remember { mutableStateOf(true) }
    var isAtBottom by remember { mutableStateOf(false) }
    var activePullEdge by remember { mutableStateOf<PullEdge?>(null) }
    var pullOffsetPx by remember { mutableFloatStateOf(0f) }
    var touchStartY by remember { mutableFloatStateOf(0f) }

    val triggerThresholdPx = 120f
    val maxPullPx = 220f

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

            viewMode = if (shouldDefaultToWebMode(article, source)) 1 else 0
        } catch (e: Exception) {
        }
    }

    LaunchedEffect(article.id) {
        articleLoadState = resetForNewArticleSession(articleLoadState)
        activePullEdge = null
        pullOffsetPx = 0f
        isAtTop = true
        isAtBottom = false
    }

    val pullState = evaluatePullNavigationState(
        edge = activePullEdge ?: PullEdge.Top,
        dragOffset = pullOffsetPx,
        triggerThreshold = triggerThresholdPx,
        hasTarget = when (activePullEdge) {
            PullEdge.Top -> previousUrl != null
            PullEdge.Bottom -> nextUrl != null
            null -> false
        }
    )

    val contentOffsetPx = when (activePullEdge) {
        PullEdge.Top -> pullOffsetPx
        PullEdge.Bottom -> -pullOffsetPx
        null -> 0f
    }

    fun updateBoundaryState(webView: WebView) {
        isAtTop = webView.scrollY <= 0
        val remainingScroll = webView.contentHeight * webView.scale - webView.height - webView.scrollY
        isAtBottom = remainingScroll <= 2f
    }

    fun finishPullGesture() {
        val releaseState = resolvePullReleaseState(
            edge = activePullEdge,
            dragOffset = pullOffsetPx,
            triggerThreshold = triggerThresholdPx,
            previousUrl = previousUrl,
            nextUrl = nextUrl,
        )
        when {
            activePullEdge == PullEdge.Top && releaseState == PullNavigationState.ReadyPrevious && previousUrl != null -> {
                onOpenAdjacentArticle(previousUrl)
            }

            activePullEdge == PullEdge.Bottom && releaseState == PullNavigationState.ReadyNext && nextUrl != null -> {
                onOpenAdjacentArticle(nextUrl)
            }
        }

        activePullEdge = null
        pullOffsetPx = 0f
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
                    val toggleTargetViewMode = getToggleTargetViewMode(viewMode)
                    IconButton(
                        onClick = { viewMode = toggleTargetViewMode },
                        enabled = article.content != null ||
                                  source?.useAutoExtract == true ||
                                  (!source?.ruleContent.isNullOrEmpty())
                    ) {
                        val icon =
                            if (toggleTargetViewMode == 1) Icons.Default.Public else Icons.Default.Description
                        val contentDescription = if (toggleTargetViewMode == 1) {
                            stringResource(R.string.desc_switch_to_web_mode)
                        } else {
                            stringResource(R.string.desc_switch_to_content_mode)
                        }
                        Icon(icon, contentDescription = contentDescription)
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
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .clipToBounds()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, contentOffsetPx.roundToInt()) }
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true

                            setBackgroundColor(android.graphics.Color.WHITE)
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                            addJavascriptInterface(object {
                                @android.webkit.JavascriptInterface
                                fun onPageLoadStatus(status: String, data: String) {
                                    when (status) {
                                        "ready" -> Unit
                                        "captcha_required" -> {
                                            isLoading = false
                                            Toast.makeText(ctx, "需要完成人机验证", Toast.LENGTH_LONG).show()
                                        }

                                        "waiting" -> Unit
                                        "redirecting" -> Unit
                                        "timeout" -> Unit
                                        "error" -> Unit
                                    }
                                }

                                @android.webkit.JavascriptInterface
                                fun onExtractionStatus(status: String, data: String) {
                                    when (status) {
                                        "extraction_complete" -> {
                                            val completeTime = System.currentTimeMillis()
                                            val currentTrace = extractionTrace
                                            val totalElapsed = currentTrace?.totalElapsedMs(completeTime) ?: 0L
                                            val extractionElapsed = currentTrace?.extractionElapsedMs(completeTime) ?: 0L
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(300)
                                                isLoading = false
                                                loadingProgress = 1f
                                            }
                                        }

                                        "extraction_error" -> {
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
                                    articleLoadState = markPageStarted(articleLoadState)

                                    if (source?.useAutoExtract == true && viewMode == 1) {
                                        isLoading = true
                                        loadingProgress = 0f
                                    }

                                    extractionTrace = WebViewContentExtractor.resetTimer()
                                    coroutineScope.launch {
                                        val currentTrace = extractionTrace ?: return@launch
                                        extractionTrace = WebViewContentExtractor.injectLoaderScript(ctx, view, currentTrace)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (shouldIgnorePageFinished(articleLoadState)) {
                                        return
                                    }

                                    val currentTrace = extractionTrace ?: WebViewContentExtractor.resetTimer()
                                    extractionTrace = currentTrace

                                    if (source?.useAutoExtract == true && viewMode == 1) {
                                        val algorithm = source?.extractionAlgorithm ?: "readability"
                                        val selector = source?.ruleContent ?: ""
                                        coroutineScope.launch {
                                            extractionTrace = WebViewContentExtractor.extractContent(
                                                view = webViewRef,
                                                trace = extractionTrace ?: currentTrace,
                                                algorithm = algorithm,
                                                selector = selector
                                            )
                                        }
                                    } else {
                                        isLoading = false
                                        loadingProgress = 1f
                                    }

                                    if (view != null) {
                                        updateBoundaryState(view)
                                    }
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                                    if (!(source?.useAutoExtract == true && viewMode == 1)) {
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    val lowerUrl = url.lowercase()
                                    if (lowerUrl.endsWith(".png") || lowerUrl.endsWith(".jpg") ||
                                        lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".gif") ||
                                        lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".svg")) {
                                        return super.shouldInterceptRequest(view, request)
                                    }

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
                                        return createEmptyResponse()
                                    }

                                    if (lowerUrl.contains("popup") || lowerUrl.contains("modal")) {
                                        return createEmptyResponse()
                                    }

                                    if (lowerUrl.endsWith(".woff") || lowerUrl.endsWith(".woff2") ||
                                        lowerUrl.endsWith(".ttf") || lowerUrl.endsWith(".eot")) {
                                        return createEmptyResponse()
                                    }

                                    if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".webm") ||
                                        lowerUrl.endsWith(".avi") || lowerUrl.endsWith(".mov")) {
                                        return createEmptyResponse()
                                    }

                                    return super.shouldInterceptRequest(view, request)
                                }
                            }
                            setOnScrollChangeListener { view, _, t, _, _ ->
                                if (t > 50 && !hasMarkedRead && !article.isRead) {
                                    hasMarkedRead = true
                                    onMarkRead()
                                }
                                updateBoundaryState(view as WebView)
                            }
                            setOnTouchListener { view, event ->
                                val webView = view as WebView
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        touchStartY = event.rawY
                                        activePullEdge = null
                                        pullOffsetPx = 0f
                                    }

                                    MotionEvent.ACTION_MOVE -> {
                                        updateBoundaryState(webView)
                                        val delta = event.rawY - touchStartY
                                        when {
                                            activePullEdge == null && delta > 0f && isAtTop -> {
                                                activePullEdge = PullEdge.Top
                                                touchStartY = event.rawY
                                                pullOffsetPx = 0f
                                            }

                                            activePullEdge == null && delta < 0f && isAtBottom -> {
                                                activePullEdge = PullEdge.Bottom
                                                touchStartY = event.rawY
                                                pullOffsetPx = 0f
                                            }

                                            activePullEdge == PullEdge.Top -> {
                                                if (delta >= 0f) {
                                                    pullOffsetPx = (delta * 0.45f).coerceIn(0f, maxPullPx)
                                                } else {
                                                    activePullEdge = null
                                                    pullOffsetPx = 0f
                                                }
                                            }

                                            activePullEdge == PullEdge.Bottom -> {
                                                if (delta <= 0f) {
                                                    pullOffsetPx = (abs(delta) * 0.45f).coerceIn(0f, maxPullPx)
                                                } else {
                                                    activePullEdge = null
                                                    pullOffsetPx = 0f
                                                }
                                            }

                                            activePullEdge != null -> {
                                                activePullEdge = null
                                                pullOffsetPx = 0f
                                            }
                                        }
                                    }

                                    MotionEvent.ACTION_UP,
                                    MotionEvent.ACTION_CANCEL -> finishPullGesture()
                                }
                                false
                            }
                            post { updateBoundaryState(this) }
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        val viewModeChanged = lastViewMode != viewMode
                        if (viewModeChanged) {
                            val previousViewMode = lastViewMode
                            lastViewMode = viewMode
                            previousViewMode
                        }

                        if (viewMode == 0 && article.content != null) {
                            isLoading = false
                            val htmlData = """
                                <html><head><meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>body { font-family: sans-serif; line-height: 1.6; padding: 16px; color: #333; word-wrap: break-word; } img { max-width: 100%; height: auto; border-radius: 8px; margin: 10px 0; } p { margin-bottom: 16px; } a { color: #007AFF; text-decoration: none; }</style>
                                </head><body><h3>${article.title}</h3>${article.content}<div style="height: 200px;"></div></body></html>
                            """.trimIndent()
                            if (webView.url != "about:blank") {
                                webView.loadDataWithBaseURL(
                                    null,
                                    htmlData,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        } else {
                            val needReload = webView.url != article.url || viewModeChanged

                            if (needReload) {
                                val (nextLoadState, generation) = requestArticleLoad(articleLoadState)
                                articleLoadState = nextLoadState
                                if (source?.useAutoExtract == true && viewMode == 1) {
                                    isLoading = true
                                    loadingProgress = 0f
                                } else {
                                    isLoading = false
                                }
                                generation
                                webView.loadUrl(article.url)
                            }
                        }

                        updateBoundaryState(webView)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = descWebview }
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
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

            if (activePullEdge == PullEdge.Top && pullOffsetPx > 0f) {
                PullNavigationOverlay(
                    edge = PullEdge.Top,
                    pullOffsetPx = pullOffsetPx,
                    state = pullState,
                    title = getPullPreviewTitle(PullEdge.Top, previousTitle),
                )
            }

            if (activePullEdge == PullEdge.Bottom && pullOffsetPx > 0f) {
                PullNavigationOverlay(
                    edge = PullEdge.Bottom,
                    pullOffsetPx = pullOffsetPx,
                    state = pullState,
                    title = getPullPreviewTitle(PullEdge.Bottom, nextTitle),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.PullNavigationOverlay(
    edge: PullEdge,
    pullOffsetPx: Float,
    state: PullNavigationState,
    title: String,
) {
    val overlayHeight = getPullOverlayHeight(pullOffsetPx)
    val backgroundColor = if (state == PullNavigationState.ReadyPrevious || state == PullNavigationState.ReadyNext) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f)
    }
    val contentColor = if (state == PullNavigationState.ReadyPrevious || state == PullNavigationState.ReadyNext) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(overlayHeight.dp)
            .align(if (edge == PullEdge.Top) Alignment.TopCenter else Alignment.BottomCenter)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val arcDepth = getPullArcDepth(width = width, height = height)
            val anchorY = if (edge == PullEdge.Top) 0f else height
            val innerY = if (edge == PullEdge.Top) height else 0f
            val path = Path().apply {
                if (edge == PullEdge.Top) {
                    moveTo(0f, anchorY)
                    lineTo(0f, innerY)
                    quadraticTo(width / 2f, arcDepth, width, innerY)
                    lineTo(width, anchorY)
                    close()
                } else {
                    moveTo(0f, anchorY)
                    lineTo(0f, innerY)
                    quadraticTo(width / 2f, height - arcDepth, width, innerY)
                    lineTo(width, anchorY)
                    close()
                }
            }
            drawPath(path = path, color = backgroundColor)
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.10f),
            )
        }

        Column(
            modifier = Modifier
                .align(if (edge == PullEdge.Top) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                maxLines = 1,
                textAlign = TextAlign.Center,
                color = contentColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = getPullReleaseHint(state),
                textAlign = TextAlign.Center,
                color = contentColor,
                style = MaterialTheme.typography.bodySmall,
            )
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

internal fun shouldDefaultToWebMode(article: Article, source: Source?): Boolean {
    val hasCustomReadingConfig =
        source?.useAutoExtract == true || !source?.ruleContent.isNullOrBlank()

    return hasCustomReadingConfig || article.content == null
}

internal fun getToggleTargetViewMode(currentViewMode: Int): Int {
    return if (currentViewMode == 0) 1 else 0
}

internal data class ArticleLoadState(
    val nextGeneration: Int = 1,
    val pendingGeneration: Int? = null,
    val activeGeneration: Int? = null,
)

internal fun requestArticleLoad(state: ArticleLoadState): Pair<ArticleLoadState, Int> {
    val generation = state.nextGeneration
    return state.copy(
        nextGeneration = generation + 1,
        pendingGeneration = generation,
    ) to generation
}

internal fun resetForNewArticleSession(state: ArticleLoadState): ArticleLoadState {
    return state.copy(activeGeneration = null)
}

internal fun markPageStarted(state: ArticleLoadState): ArticleLoadState {
    val generation = state.pendingGeneration ?: state.activeGeneration
    return state.copy(
        pendingGeneration = null,
        activeGeneration = generation,
    )
}

internal fun shouldIgnorePageFinished(state: ArticleLoadState): Boolean {
    return state.pendingGeneration != null || state.activeGeneration == null
}

internal enum class PullEdge {
    Top,
    Bottom,
}

internal enum class PullNavigationState {
    Idle,
    PullingPrevious,
    PullingNext,
    ReadyPrevious,
    ReadyNext,
    BoundaryOnly,
}

internal fun evaluatePullNavigationState(
    edge: PullEdge,
    dragOffset: Float,
    triggerThreshold: Float,
    hasTarget: Boolean,
): PullNavigationState {
    if (dragOffset <= 0f) return PullNavigationState.Idle
    if (!hasTarget) return PullNavigationState.BoundaryOnly

    return when (edge) {
        PullEdge.Top -> {
            if (dragOffset >= triggerThreshold) PullNavigationState.ReadyPrevious
            else PullNavigationState.PullingPrevious
        }

        PullEdge.Bottom -> {
            if (dragOffset >= triggerThreshold) PullNavigationState.ReadyNext
            else PullNavigationState.PullingNext
        }
    }
}

internal fun getPullPreviewTitle(edge: PullEdge, articleTitle: String?): String {
    return when (edge) {
        PullEdge.Top -> when {
            articleTitle == null -> "已经是第一条"
            articleTitle.isBlank() -> "上一篇"
            else -> "上一篇：$articleTitle"
        }

        PullEdge.Bottom -> when {
            articleTitle == null -> "已经是最后一条"
            articleTitle.isBlank() -> "下一篇"
            else -> "下一篇：$articleTitle"
        }
    }
}

internal fun getPullOverlayHeight(pullOffsetPx: Float): Float {
    return pullOffsetPx.coerceIn(40f, 72f)
}

internal fun getPullArcDepth(width: Float, height: Float): Float {
    return (width * 0.2f).coerceAtMost(height * 0.72f)
}

internal fun resolvePullReleaseState(
    edge: PullEdge?,
    dragOffset: Float,
    triggerThreshold: Float,
    previousUrl: String?,
    nextUrl: String?,
): PullNavigationState {
    val resolvedEdge = edge ?: return PullNavigationState.Idle
    return evaluatePullNavigationState(
        edge = resolvedEdge,
        dragOffset = dragOffset,
        triggerThreshold = triggerThreshold,
        hasTarget = when (resolvedEdge) {
            PullEdge.Top -> previousUrl != null
            PullEdge.Bottom -> nextUrl != null
        }
    )
}

internal fun getPullReleaseHint(state: PullNavigationState): String {
    return when (state) {
        PullNavigationState.ReadyPrevious -> "释放查看上一篇"
        PullNavigationState.ReadyNext -> "释放查看下一篇"
        PullNavigationState.BoundaryOnly -> "松手后回弹"
        PullNavigationState.PullingPrevious -> "继续下拉"
        PullNavigationState.PullingNext -> "继续上拉"
        PullNavigationState.Idle -> ""
    }
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
