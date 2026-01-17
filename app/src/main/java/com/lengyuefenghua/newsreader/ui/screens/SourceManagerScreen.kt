package com.lengyuefenghua.newsreader.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.viewmodel.SourceViewModel
import java.net.SocketImpl



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceManagerScreen(
    viewModel: SourceViewModel = viewModel(),
    onOpenAdvanced: () -> Unit = {},
    onEditSource: (Int) -> Unit = {}
) {
    val sources by viewModel.sources.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // 多选状态
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Int>() }

    // 弹窗状态
    var showSimpleDialog by remember { mutableStateOf(false) }

    // 处理返回键：如果是多选模式，则退出多选
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
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedIds.clear()
                        }) { Icon(Icons.Default.Close, "取消") }
                    },
                    actions = {
                        // 全选
                        IconButton(onClick = {
                            if (selectedIds.size == sources.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(sources.map { it.id })
                            }
                        }) { Icon(Icons.Default.SelectAll, "全选") }
                        // 导出选中
                        IconButton(onClick = {
                            val selectedSources = sources.filter { it.id in selectedIds }
                            val json = viewModel.exportSourcesToJson(selectedSources)
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(
                                context,
                                "已复制 ${selectedSources.size} 个源到剪贴板",
                                Toast.LENGTH_SHORT
                            ).show()
                            isSelectionMode = false
                            selectedIds.clear()
                        }) { Icon(Icons.Default.Share, "导出") }
                        // 删除选中
                        IconButton(onClick = {
                            val selectedSources = sources.filter { it.id in selectedIds }
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
        if (sources.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无订阅源，点击右下角添加")
            }
        } else {
            LazyColumn(contentPadding = innerPadding, modifier = Modifier.fillMaxSize()) {
                items(sources) { source ->
                    val isSelected = selectedIds.contains(source.id)
                    SourceItem(
                        source = source,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onDelete = { viewModel.deleteSource(source) },
                        onEdit = { onEditSource(source.id) },
                        onShare = {
                            val json = viewModel.exportSourceToJson(source)
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedIds.add(source.id)
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedIds.remove(source.id) else selectedIds.add(
                                    source.id
                                )
                            } else {
                                // 非多选模式下的点击行为，暂时可以为空，或者进入编辑
                                // 这里保持空，让用户点编辑按钮进入编辑
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
    source: Source,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 多选模式下显示 Checkbox
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = source.name, style = MaterialTheme.typography.titleMedium)
                Text(text = source.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                val modeText =
                    if (source.isCustom) "自定义" else if (source.useAutoExtract || source.ruleContent.isNotBlank()) "RSS+正文" else "RSS"
                Text(
                    text = modeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // 非多选模式下显示操作按钮
            if (!isSelectionMode) {
                Row {
                    // [新增] 分享按钮
                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "分享",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun SimpleAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onSwitchToAdvanced: () -> Unit,
    onImportFromClipboard: () -> Unit // 添加空函数作为默认值
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
            Row(
                Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onImportFromClipboard) {
                    Text("剪贴板导入")
                }
                TextButton(onClick = onSwitchToAdvanced) {
                    Text("自定义")
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                TextButton(
                    onClick = {
                        if (name.isNotBlank() && url.isNotBlank()) {
                            onConfirm(name, url)
                        }
                    }
                ) { Text("保存") }

            }
        }
    )
}