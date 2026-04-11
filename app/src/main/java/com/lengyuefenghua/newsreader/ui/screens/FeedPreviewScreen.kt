package com.lengyuefenghua.newsreader.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.lengyuefenghua.newsreader.viewmodel.FeedPreviewViewModel
import com.lengyuefenghua.newsreader.viewmodel.SourceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPreviewScreen(
    previewViewModel: FeedPreviewViewModel,
    sourceViewModel: SourceViewModel,
    onBack: () -> Unit,
    onArticleClick: (String, List<ArticleReadingItem>) -> Unit,
    onOpenAdvanced: (String, String) -> Unit,
    onSubscribed: () -> Unit,
) {
    val preview by previewViewModel.preview.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    if (preview == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无预览内容")
        }
        return
    }

    val currentPreview = preview ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = currentPreview.displayTitle, maxLines = 1)
                        Text(text = currentPreview.url)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("添加订阅")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Text(
                    text = "共找到 ${currentPreview.articles.size} 篇文章",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(currentPreview.articles, key = { it.id }) { article ->
                ArticleCard(
                    article = article,
                    sourceIconUrl = currentPreview.iconUrl,
                    onClick = {
                        onArticleClick(
                            article.url,
                            currentPreview.articles.map {
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

    if (showAddDialog) {
        SimpleAddDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url ->
                coroutineScope.launch {
                    if (sourceViewModel.isUrlExists(url)) {
                        Toast.makeText(context, "该订阅源地址已存在", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    sourceViewModel.addSource(
                        Source(
                            name = name,
                            url = url,
                            iconUrl = currentPreview.iconUrl,
                        )
                    )
                    showAddDialog = false
                    Toast.makeText(context, "订阅源已添加", Toast.LENGTH_SHORT).show()
                    onSubscribed()
                }
            },
            onSwitchToAdvanced = { name, url ->
                showAddDialog = false
                onOpenAdvanced(name, url)
            },
            onImportFromClipboard = {},
            onImportFromFile = {},
            showNameInput = true,
            showImportButton = false,
            confirmText = "保存",
            initialName = currentPreview.displayTitle,
            initialUrl = currentPreview.url
        )
    }
}
