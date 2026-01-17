package com.lengyuefenghua.newsreader.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lengyuefenghua.newsreader.data.Article
import java.io.InputStream

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    article: Article?,
    onBack: () -> Unit,
    onMarkRead: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditSource: (String) -> Unit
) {
    if (article == null) return

    val context = LocalContext.current
    var hasMarkedRead by remember { mutableStateOf(false) }

    // Mode 0: 本地渲染 (RSS正文)
    // Mode 1: 原文链接 (浏览器模式)
    var viewMode by remember { mutableStateOf(if (article.content != null) 0 else 1) }

    var isTranslationEnabled by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(article.title, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                        Text(article.sourceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("订阅设置") },
                            onClick = {
                                showMenu = false
                                onEditSource(article.sourceName)
                            }
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
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("浏览器打开") },
                            onClick = {
                                showMenu = false
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                                context.startActivity(browserIntent)
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            // [修改] 高度设置为 48dp，移除内边距，实现紧凑的工具栏效果
            BottomAppBar(
                modifier = Modifier.height(48.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = NavigationBarDefaults.Elevation,
                contentPadding = PaddingValues(0.dp) // 移除默认 padding
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(), // 填满 48dp 高度
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically // 垂直居中
                ) {
                    // 1. 模式切换
                    IconButton(
                        onClick = { viewMode = if (viewMode == 0) 1 else 0 },
                        enabled = article.content != null
                    ) {
                        val icon = if (viewMode == 0) Icons.Default.Public else Icons.Default.Description
                        val tint = if (article.content != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        Icon(icon, contentDescription = "切换模式", tint = tint)
                    }

                    // 2. 标记已读
                    IconButton(onClick = onMarkRead) {
                        Icon(
                            imageVector = if (article.isRead) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "已读",
                            tint = if (article.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 3. 翻译
                    IconButton(onClick = {
                        if (!isTranslationEnabled) {
                            isTranslationEnabled = true
                            injectTranslationScript(context, webViewRef)
                            Toast.makeText(context, "正在启动沉浸式翻译...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "翻译已开启", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = "翻译",
                            tint = if (isTranslationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 4. 收藏
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (article.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
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

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (isTranslationEnabled) {
                                    injectTranslationScript(ctx, view)
                                }
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
                    if (viewMode == 0 && article.content != null) {
                        val htmlData = """
                            <html>
                            <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body { font-family: sans-serif; line-height: 1.6; padding: 16px; color: #333; word-wrap: break-word; }
                                img { max-width: 100%; height: auto; border-radius: 8px; margin: 10px 0; }
                                p { margin-bottom: 16px; }
                                a { color: #007AFF; text-decoration: none; }
                            </style>
                            </head>
                            <body>
                            <h3>${article.title}</h3>
                            ${article.content}
                            <div style="height: 200px;"></div>
                            </body>
                            </html>
                        """.trimIndent()
                        if (webView.url == null || webView.url == "about:blank") {
                            webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
                        }
                    } else {
                        val currentUrl = webView.url
                        if (currentUrl != article.url) {
                            webView.loadUrl(article.url)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
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

        webView.evaluateJavascript(
            "(function() { $script })(); void(0);",
            null
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}