package com.lengyuefenghua.newsreader.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.ui.components.ArticleCard
import com.lengyuefenghua.newsreader.viewmodel.PlinkFeedPreviewViewModel
import com.lengyuefenghua.newsreader.viewmodel.SourceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlinkFeedPreviewScreen(
    title: String,
    url: String,
    viewModel: PlinkFeedPreviewViewModel,
    sourceViewModel: SourceViewModel,
    onBack: () -> Unit,
    onArticleClick: (String, List<ArticleReadingItem>) -> Unit,
    onOpenAdvanced: (String, String) -> Unit,
    onSubscribed: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(title, url) {
        viewModel.loadPreview(title = title, url = url)
    }

    val preview = state.preview

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = preview?.displayTitle ?: title, maxLines = 1)
                        Text(text = url)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (preview != null) {
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("添加订阅")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading && preview == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null && preview == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.error ?: "获取订阅源预览失败")
                        TextButton(onClick = { viewModel.loadPreview(title = title, url = url, forceRefresh = true) }) {
                            Text("重试")
                        }
                    }
                }
            }

            preview != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    item {
                        Text(
                            text = "共找到 ${preview.articles.size} 篇文章",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(preview.articles, key = { it.id }) { article ->
                        ArticleCard(
                            article = article,
                            sourceIconUrl = preview.iconUrl,
                            onClick = {
                                onArticleClick(
                                    article.url,
                                    preview.articles.map {
                                        ArticleReadingItem(
                                            url = it.url,
                                            title = it.title,
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无预览内容")
                }
            }
        }
    }

    if (showAddDialog && preview != null) {
        SimpleAddDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, targetUrl ->
                coroutineScope.launch {
                    if (sourceViewModel.isUrlExists(targetUrl)) {
                        Toast.makeText(context, "该订阅源地址已存在", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    sourceViewModel.addSource(
                        Source(
                            name = name,
                            url = targetUrl,
                            iconUrl = preview.iconUrl,
                        )
                    )
                    showAddDialog = false
                    Toast.makeText(context, "订阅源已添加", Toast.LENGTH_SHORT).show()
                    onSubscribed()
                }
            },
            onSwitchToAdvanced = { name, targetUrl ->
                showAddDialog = false
                onOpenAdvanced(name, targetUrl)
            },
            onImportFromClipboard = {},
            onImportFromFile = {},
            showNameInput = true,
            showImportButton = false,
            confirmText = "保存",
            initialName = preview.displayTitle,
            initialUrl = preview.url
        )
    }
}
