package com.lengyuefenghua.newsreader.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.lengyuefenghua.newsreader.data.Article

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    article: Article?,
    onBack: () -> Unit
) {
    if (article == null) { /* 错误处理 */ return }

    // ...

    // 提示信息，告诉用户当前看的是缓存还是网页
    val loadModeInfo = if (article.content != null) "已加载 RSS 正文" else "正在加载原文网页..."

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(article.title, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                        Text(loadModeInfo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()

                        // [逻辑判断]
                        if (article.content != null) {
                            // 1. 有本地正文 (因为 rawBody 包含 HTML 标签) -> 直接渲染
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
                                ${article.content}
                                <br><hr><br>
                                <a href="${article.url}" style="display:block; text-align:center; padding:12px; background:#f0f0f0; border-radius:8px;">
                                    查看原文网页
                                </a>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
                        } else {
                            // 2. 无本地正文 (纯文本摘要) -> 加载 URL
                            loadUrl(article.url)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // ... 进度条逻辑 ...
        }
    }
}