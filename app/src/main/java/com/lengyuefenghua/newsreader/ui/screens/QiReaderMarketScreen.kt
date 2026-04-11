package com.lengyuefenghua.newsreader.ui.screens

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.lengyuefenghua.newsreader.data.NewsRepository

private const val QIREADER_BRIDGE_NAME = "QiReaderBridge"

private const val QIREADER_BRIDGE_SCRIPT = """
(function() {
  if (window.__qiReaderBridgeInstalled) {
    return;
  }
  window.__qiReaderBridgeInstalled = true;

  function normalizeText(value) {
    return (value || '').replace(/\s+/g, ' ').trim();
  }

  function isSubscribeButton(button) {
    return !!button && button.tagName === 'BUTTON' && normalizeText(button.innerText || button.textContent) === '订阅';
  }

  function findCardRoot(startNode) {
    let node = startNode;
    while (node && node !== document.body) {
      const titleLink = node.querySelector('h4 a[href]');
      const reportButton = node.querySelector('button[title*="举报"]');
      if (titleLink && reportButton) {
        return node;
      }
      node = node.parentElement;
    }
    return null;
  }

  function extractFeedInfo(card) {
    const titleLink = card.querySelector('h4 a[href]');
    if (!titleLink) {
      return null;
    }

    const titleContainer = titleLink.closest('h4');
    const detailContainer = titleContainer && titleContainer.parentElement ? titleContainer.parentElement : card;
    const links = Array.from(detailContainer.querySelectorAll('a[href]'));
    const feedLink = links.find(function(link) {
      return link !== titleLink && !!link.href && link.href !== titleLink.href;
    });

    if (!feedLink) {
      return null;
    }

    return {
      title: normalizeText(titleLink.textContent),
      feedUrl: feedLink.href
    };
  }

  document.addEventListener('click', function(event) {
    const target = event.target;
    if (!(target instanceof Element)) {
      return;
    }

    const button = target.closest('button');
    if (!isSubscribeButton(button)) {
      return;
    }

    const card = findCardRoot(button);
    const info = card ? extractFeedInfo(card) : null;
    if (!info || !info.title || !info.feedUrl || !window.QiReaderBridge) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    if (typeof event.stopImmediatePropagation === 'function') {
      event.stopImmediatePropagation();
    }

    window.QiReaderBridge.openFeedPreview(info.title, info.feedUrl);
  }, true);
})();
"""

private class QiReaderJavascriptBridge(
    private val mainHandler: Handler,
    private val onOpenFeedPreview: (String, String) -> Unit
) {
    @JavascriptInterface
    fun openFeedPreview(name: String?, url: String?) {
        val trimmedName = name?.trim().orEmpty()
        val trimmedUrl = url?.trim().orEmpty()
        if (trimmedName.isBlank() || trimmedUrl.isBlank()) {
            return
        }

        mainHandler.post {
            onOpenFeedPreview(trimmedName, trimmedUrl)
        }
    }
}

private fun injectQiReaderBridge(webView: WebView?) {
    webView?.evaluateJavascript(QIREADER_BRIDGE_SCRIPT, null)
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiReaderMarketScreen(
    onBack: () -> Unit,
    onOpenFeedPreview: (String, String) -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val cookieManager = remember { CookieManager.getInstance() }
    val webViewState = rememberSaveable { Bundle() }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.let { view ->
                webViewState.clear()
                view.saveState(webViewState)
                view.removeJavascriptInterface(QIREADER_BRIDGE_NAME)
                view.stopLoading()
                view.destroy()
            }
            webView = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QiReader") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { webView?.reload() }) {
                        Text("刷新")
                    }
                    TextButton(onClick = {
                        cookieManager.removeAllCookies {
                            cookieManager.flush()
                            WebStorage.getInstance().deleteAllData()
                            webViewState.clear()
                            webView?.apply {
                                clearCache(true)
                                clearHistory()
                                loadUrl(NewsRepository.QIREADER_DISCOVER_URL)
                            }
                            Toast.makeText(context, "已清除登录状态", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("清登录")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.userAgentString = NewsRepository.UA_ANDROID

                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        addJavascriptInterface(
                            QiReaderJavascriptBridge(
                                mainHandler = mainHandler,
                                onOpenFeedPreview = onOpenFeedPreview
                            ),
                            QIREADER_BRIDGE_NAME
                        )

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                cookieManager.flush()
                                injectQiReaderBridge(view)
                            }
                        }

                        val restored = if (!webViewState.isEmpty) {
                            restoreState(webViewState) != null
                        } else {
                            false
                        }
                        if (!restored) {
                            loadUrl(NewsRepository.QIREADER_DISCOVER_URL)
                        } else {
                            post { injectQiReaderBridge(this) }
                            isLoading = false
                        }

                        webView = this
                    }
                },
                update = { view ->
                    webView = view
                }
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}
