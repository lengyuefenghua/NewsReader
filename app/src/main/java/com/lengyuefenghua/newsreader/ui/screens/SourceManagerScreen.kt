package com.lengyuefenghua.newsreader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.viewmodel.SourceViewModel

@Composable
fun SourceManagerScreen(
    viewModel: SourceViewModel = viewModel(),
    onOpenAdvanced: () -> Unit, // 点击“高级模式”时的回调
    onEditSource: (Int) -> Unit // 点击“编辑”时的回调
) {
    val sources by viewModel.sources.collectAsState()

    // 控制弹窗显示的状态
    var showSimpleDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showSimpleDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { innerPadding ->
        if (sources.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("暂无订阅源，点击右下角添加")
            }
        } else {
            LazyColumn(contentPadding = innerPadding, modifier = Modifier.fillMaxSize()) {
                items(sources) { source ->
                    SourceItem(
                        source = source,
                        onDelete = { viewModel.deleteSource(source) },
                        onEdit = { onEditSource(source.id) } // 点击列表项的编辑，直接进全屏
                    )
                }
            }
        }

        // 简易添加弹窗
        if (showSimpleDialog) {
            SimpleAddDialog(
                onDismiss = { showSimpleDialog = false },
                onConfirm = { name, url ->
                    // 简单模式直接添加为标准 RSS
                    viewModel.addSource(Source(name = name, url = url))
                    showSimpleDialog = false
                },
                onSwitchToAdvanced = {
                    showSimpleDialog = false
                    onOpenAdvanced() // 关闭弹窗，跳转全屏
                }
            )
        }
    }
}

@Composable
fun SourceItem(source: Source, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = source.name, style = MaterialTheme.typography.titleMedium)
                Text(text = source.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                // 标记一下模式
                val modeText = if (source.isCustom) "自定义爬虫" else if (source.useAutoExtract || source.ruleContent.isNotBlank()) "RSS+正文" else "标准 RSS"
                Text(text = modeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
            // 编辑按钮
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
            }
            // 删除按钮
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// 简易模式弹窗
@Composable
fun SimpleAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onSwitchToAdvanced: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加 RSS 订阅") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("RSS 地址") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onConfirm(name, url)
                    }
                }
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                // 左侧显示高级模式按钮
                TextButton(onClick = onSwitchToAdvanced) {
                    Text("高级模式", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}