package com.lengyuefenghua.newsreader.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.ui.components.ArticleCard
import com.lengyuefenghua.newsreader.viewmodel.FilterType
import com.lengyuefenghua.newsreader.viewmodel.TimelineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = viewModel(),
    onArticleClick: (String) -> Unit
) {
    val articles by viewModel.articles.collectAsState()
    val syncState by viewModel.syncState.collectAsState() // [新增] 监听同步状态
    val currentFilter by viewModel.filterState.collectAsState()
    val context = LocalContext.current

    // [新增] 监听一次性 Toast 事件
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("时间线") },
                actions = {
                    FilterChipGroup(
                        currentFilter = currentFilter,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )
                }
            )
        },
        floatingActionButton = {
            // 刷新按钮，如果在同步中可以禁用或者保持可点（重复点击逻辑在ViewModel已处理）
            FloatingActionButton(onClick = { viewModel.fetchArticles() }) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // [新增] 顶部进度条区域
            if (syncState.isSyncing) {
                // 计算进度 0.0 ~ 1.0
                val progress = if (syncState.total == 0) 0f else syncState.current.toFloat() / syncState.total

                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp), // [要求] 高度 8dp
                    )
                    Text(
                        // [要求] 格式：已更新订阅源数/订阅源总数，正在更新 订阅源名称
                        text = "正在更新 ${syncState.currentSource}     ${syncState.current}/${syncState.total}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 文章列表区域
            Box(modifier = Modifier.weight(1f)) {
                if (articles.isEmpty() && !syncState.isSyncing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无文章", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(articles, key = { it.id }) { article ->
                            ArticleCard(
                                article = article,
                                onClick = { onArticleClick(article.url) }
                            )
                        }
                    }
                }

                // [修改] 移除了之前的 CircularProgressIndicator，
                // 因为现在有了顶部的 LinearProgressIndicator。
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipGroup(
    currentFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = currentFilter == FilterType.ALL,
            onClick = { onFilterSelected(FilterType.ALL) },
            label = { Text("全部") },
            modifier = Modifier.padding(end = 8.dp)
        )
        FilterChip(
            selected = currentFilter == FilterType.UNREAD,
            onClick = { onFilterSelected(FilterType.UNREAD) },
            label = { Text("未读") },
            modifier = Modifier.padding(end = 8.dp)
        )
        FilterChip(
            selected = currentFilter == FilterType.READ,
            onClick = { onFilterSelected(FilterType.READ) },
            label = { Text("已读") },
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}