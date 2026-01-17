package com.lengyuefenghua.newsreader.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.ui.components.ArticleCard
import com.lengyuefenghua.newsreader.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = viewModel(),
    onBack: () -> Unit,
    onArticleClick: (String) -> Unit
) {
    val articles by viewModel.favoriteArticles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() } // Article ID 是 String

    BackHandler {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedIds.clear()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("已选择 ${selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { isSelectionMode = false; selectedIds.clear() }) { Icon(Icons.Default.Close, "取消") }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedIds.size == articles.size) selectedIds.clear() else {
                                selectedIds.clear()
                                selectedIds.addAll(articles.map { it.id })
                            }
                        }) { Icon(Icons.Default.SelectAll, "全选") }
                        IconButton(onClick = {
                            viewModel.removeFavorites(selectedIds.toList()) // 转换为 List 传入
                            isSelectionMode = false
                            selectedIds.clear()
                        }) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                // 普通模式：显示搜索框
                Column {
                    TopAppBar(
                        title = { Text("我的收藏") },
                        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }
                    )
                    // [新增] 搜索栏
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("搜索标题或内容...") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true
                    )
                }
            }
        }
    ) { innerPadding ->
        if (articles.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isEmpty()) "暂无收藏文章" else "未找到相关文章", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                items(articles, key = { it.id }) { article ->
                    val isSelected = selectedIds.contains(article.id)

                    // [修改] 支持多选的 ArticleItem
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedIds.remove(article.id) else selectedIds.add(article.id)
                                    } else {
                                        onArticleClick(article.url)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedIds.add(article.id)
                                    }
                                }
                            )
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { if (isSelected) selectedIds.remove(article.id) else selectedIds.add(article.id) },
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                        // 复用 ArticleCard，注意：ArticleCard 内部也有 clickable，可能会冲突
                        // 为了简单，这里直接包裹 ArticleCard 并禁用其内部点击 (或者传递 null)
                        // 更好的做法是拆分 Card 内容。这里采用 Box 覆盖点击事件的方式：
                        Box(modifier = Modifier.weight(1f)) {
                            ArticleCard(article = article, onClick = null) // 空点击，由外部 Row 处理
                        }
                    }
                }
            }
        }
    }
}