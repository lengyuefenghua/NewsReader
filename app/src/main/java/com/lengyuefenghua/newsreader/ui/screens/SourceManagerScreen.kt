package com.lengyuefenghua.newsreader.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.viewmodel.SourceViewModel
import com.lengyuefenghua.newsreader.viewmodel.SourceWithStat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceManagerScreen(
    viewModel: SourceViewModel = viewModel(),
    onOpenAdvanced: () -> Unit = {},
    onEditSource: (Int) -> Unit = {},
    onSourceClick: (Int) -> Unit = {}
) {
    val sourceItems by viewModel.sourcesWithStats.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Int>() }
    var showSimpleDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds.clear()
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
                            if (selectedIds.size == sourceItems.size) selectedIds.clear() else {
                                selectedIds.clear()
                                selectedIds.addAll(sourceItems.map { it.source.id })
                            }
                        }) { Icon(Icons.Default.SelectAll, "全选") }
                        IconButton(onClick = {
                            val selectedSources = sourceItems.filter { it.source.id in selectedIds }.map { it.source }
                            val json = viewModel.exportSourcesToJson(selectedSources)
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "已复制 ${selectedSources.size} 个源到剪贴板", Toast.LENGTH_SHORT).show()
                            isSelectionMode = false
                            selectedIds.clear()
                        }) { Icon(Icons.Default.Share, "导出") }
                        IconButton(onClick = {
                            val selectedSources = sourceItems.filter { it.source.id in selectedIds }.map { it.source }
                            viewModel.deleteSources(selectedSources)
                            isSelectionMode = false
                            selectedIds.clear()
                        }) { Icon(Icons.Default.Delete, "删除") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { showSimpleDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }
        }
    ) { innerPadding ->
        if (sourceItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("暂无订阅源，点击右下角添加")
            }
        } else {
            LazyColumn(
                // [修改点] 将 left 改为 start，将 right 改为 end
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = 16.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp // 留出 FAB 空间
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sourceItems) { item ->
                    val isSelected = selectedIds.contains(item.source.id)
                    SourceItem(
                        item = item,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onDelete = { viewModel.deleteSource(item.source) },
                        onEdit = { onEditSource(item.source.id) },
                        onUpdate = {
                            Toast.makeText(context, "开始更新: ${item.source.name}", Toast.LENGTH_SHORT).show()
                            viewModel.syncSource(item.source.id) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    Toast.makeText(context, "${item.source.name} 更新完成", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onShare = {
                            val json = viewModel.exportSourceToJson(item.source)
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                        onMarkAllRead = { viewModel.markAllAsRead(item.source.name) },
                        onMarkAllUnread = { viewModel.markAllAsUnread(item.source.name) },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedIds.add(item.source.id)
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedIds.remove(item.source.id) else selectedIds.add(item.source.id)
                            } else {
                                onSourceClick(item.source.id)
                            }
                        }
                    )
                }
            }
        }

        if (showSimpleDialog) {
            SimpleAddDialog(
                onDismiss = { showSimpleDialog = false },
                onConfirm = { name, url ->
                    viewModel.addSource(Source(name = name, url = url))
                    showSimpleDialog = false
                },
                onSwitchToAdvanced = {
                    showSimpleDialog = false
                    onOpenAdvanced()
                },
                onImportFromClipboard = {
                    val clipboardContent = clipboardManager.getText()?.text
                    if (!clipboardContent.isNullOrBlank()) {
                        val count = viewModel.importFromJson(clipboardContent)
                        if (count > 0) {
                            Toast.makeText(context, "成功导入 $count 个订阅源", Toast.LENGTH_SHORT)
                                .show()
                            showSimpleDialog = false
                        } else {
                            Toast.makeText(context, "剪贴板内容无效或解析失败", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceItem(
    item: SourceWithStat,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onUpdate: () -> Unit,
    onShare: () -> Unit,
    onMarkAllRead: () -> Unit,
    onMarkAllUnread: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val source = item.source

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp) // [新增] 强制留出右侧空间，防止文字与图标挤在一起
            ){
                Text(text = source.name, style = MaterialTheme.typography.titleMedium)
                Text(text = source.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)

                val unread = item.total - item.read
                Text(
                    text = "已读 ${item.read} / 未读 $unread / 总计 ${item.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unread > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "更多") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("更新") }, onClick = { showMenu = false; onUpdate() }, leadingIcon = { Icon(Icons.Default.Refresh, null) })
                        DropdownMenuItem(text = { Text("全部已读") }, onClick = { showMenu = false; onMarkAllRead() }, leadingIcon = { Icon(Icons.Default.DoneAll, null) })
                        DropdownMenuItem(text = { Text("全部未读") }, onClick = { showMenu = false; onMarkAllUnread() }, leadingIcon = { Icon(Icons.Default.RemoveDone, null) })
                        Divider()
                        DropdownMenuItem(text = { Text("分享") }, onClick = { showMenu = false; onShare() }, leadingIcon = { Icon(Icons.Default.Share, null) })
                        DropdownMenuItem(text = { Text("编辑") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        DropdownMenuItem(text = { Text("删除", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                    }
                }
            }
        }
    }
}

// [修复] 还原为横向布局，左侧功能，右侧操作
@Composable
fun SimpleAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onSwitchToAdvanced: () -> Unit,
    onImportFromClipboard: () -> Unit
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("RSS 地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            // [修复] 使用 Row + SpaceBetween 将按钮分组到两端
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：辅助功能
                Row {
                    TextButton(onClick = onImportFromClipboard) { Text("剪贴板导入") }
                    TextButton(onClick = onSwitchToAdvanced) { Text("自定义") }
                }
                // 右侧：主要操作
                Row {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = { if (name.isNotBlank() && url.isNotBlank()) onConfirm(name, url) }) { Text("保存") }
                }
            }
        }
    )
}