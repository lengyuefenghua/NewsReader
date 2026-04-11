package com.lengyuefenghua.newsreader.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.R
import com.lengyuefenghua.newsreader.core.navigation.NavRoutes
import com.lengyuefenghua.newsreader.data.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.lengyuefenghua.newsreader.viewmodel.IndexedSourceWithStat
import com.lengyuefenghua.newsreader.viewmodel.SourceViewModel
import com.lengyuefenghua.newsreader.viewmodel.SourceGroup
import com.lengyuefenghua.newsreader.viewmodel.SourceWithStat
import com.lengyuefenghua.newsreader.viewmodel.ImportStrategy
import com.lengyuefenghua.newsreader.viewmodel.FeedPreviewViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SourceManagerScreen(
    viewModel: SourceViewModel = viewModel(),
    previewViewModel: FeedPreviewViewModel = viewModel(),
    onOpenAdvanced: (String, String) -> Unit = { _, _ -> },
    onEditSource: (Int) -> Unit = {},
    onSourceClick: (Int) -> Unit = {},
    onOpenFeedPreview: () -> Unit = {}
) {
    val uiState by viewModel.sourceManagerUiState.collectAsState()
    val sourceItems = uiState.visibleSources
    val groupedSources = uiState.groupedSources
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Int>() }
    var showSimpleDialog by remember { mutableStateOf(false) }
    var isSearchingPreview by remember { mutableStateOf(false) }
    var previewSearchJob by remember { mutableStateOf<Job?>(null) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var sourceToDelete by remember { mutableStateOf<Source?>(null) }
    var sourcesToDelete by remember { mutableStateOf<List<Source>?>(null) }
    var sourceToMove by remember { mutableStateOf<Source?>(null) }
    var groupToRename by remember { mutableStateOf<String?>(null) }
    val collapsedGroups = remember { mutableStateListOf<String>() }
    val coroutineScope = rememberCoroutineScope()
    val collapsedGroupSet = collapsedGroups.toSet()
    val visibleEntries = remember(groupedSources, collapsedGroupSet) {
        buildVisibleSourceEntries(groupedSources, collapsedGroupSet)
    }
    val availableLetters = remember(visibleEntries) {
        visibleEntries.mapNotNull { entry ->
            (entry as? SourceListEntry.Item)?.indexedItem?.indexLetter
        }.toSet()
    }
    val alphabetIndexTargets = remember(visibleEntries) { buildAlphabetIndexTargets(visibleEntries) }
    val currentIndexLetter by remember(listState, visibleEntries) {
        derivedStateOf {
            findCurrentIndexLetter(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                entries = visibleEntries
            )
        }
    }

    // [新增] 导入策略选择
    var showImportStrategyDialog by remember { mutableStateOf(false) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }

    // [新增] 文件选择器 launcher（用于备份）
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val message = viewModel.saveToFile(uri, context)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                isSelectionMode = false
                selectedIds.clear()
            }
        }
    }

    // [新增] 文件选择器 launcher（用于导入）
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                    if (!json.isNullOrBlank()) {
                        // 先检测是否有重复
                        val hasDuplicates = viewModel.hasDuplicates(json)
                        if (hasDuplicates) {
                            // 有重复，显示策略选择对话框
                            pendingImportContent = json
                            showImportStrategyDialog = true
                        } else {
                            // 没有重复，直接导入
                            val result = viewModel.importFromJson(json, ImportStrategy.SKIP)
                            val message = if (result.hasError) {
                                result.error ?: "导入失败"
                            } else if (result.imported > 0) {
                                "成功导入 ${result.imported} 个订阅源"
                            } else {
                                "文件内容无效或解析失败"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (result.imported > 0) {
                                showSimpleDialog = false
                            }
                        }
                    } else {
                        Toast.makeText(context, "文件内容为空", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "读取文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                            isSelectionMode = false; selectedIds.clear()
                        }) { Icon(Icons.Default.Close, stringResource(R.string.desc_cancel)) }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedIds.size == sourceItems.size) selectedIds.clear() else {
                                selectedIds.clear()
                                selectedIds.addAll(sourceItems.map { it.source.id })
                            }
                        }) { Icon(Icons.Default.SelectAll, stringResource(R.string.desc_select_all)) }
                        Box {
                            IconButton(onClick = { showShareMenu = true }) {
                                Icon(Icons.Default.Share, "分享")
                            }
                            DropdownMenu(
                                expanded = showShareMenu,
                                onDismissRequest = { showShareMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("复制到剪贴板") },
                                    onClick = {
                                        showShareMenu = false
                                        val selectedSources =
                                            sourceItems.filter { it.source.id in selectedIds }
                                                .map { it.source }
                                        val json = viewModel.exportSourcesToJson(selectedSources)
                                        clipboardManager.setText(AnnotatedString(json))
                                        Toast.makeText(
                                            context,
                                            "已复制 ${selectedSources.size} 个源到剪贴板",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isSelectionMode = false
                                        selectedIds.clear()
                                    },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("更多分享") },
                                    onClick = {
                                        showShareMenu = false
                                        val selectedSources =
                                            sourceItems.filter { it.source.id in selectedIds }
                                                .map { it.source }
                                        val json = viewModel.exportSourcesToJson(selectedSources)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, json)
                                            putExtra(Intent.EXTRA_SUBJECT, "NewsReader 订阅源")
                                        }
                                        context.startActivity(Intent.createChooser(intent, "分享订阅源"))
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("备份到本地") },
                                    onClick = {
                                        showShareMenu = false
                                        // 生成默认文件名
                                        val fileName = "NewsReader_Feeds_backup.json"
                                        filePickerLauncher.launch(fileName)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Save, null) }
                                )
                            }
                        }
                        IconButton(onClick = {
                            val selectedSources =
                                sourceItems.filter { it.source.id in selectedIds }.map { it.source }
                            sourcesToDelete = selectedSources
                            showDeleteConfirmDialog = true
                        }) { Icon(Icons.Default.Delete, stringResource(R.string.desc_delete)) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Surface(shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "订阅源管理",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        CompactSearchField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { showSimpleDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_add))
                }
            }
        }
    ) { innerPadding ->
        if (sourceItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(if (searchQuery.isBlank()) "暂无订阅源，点击右下角添加" else "未找到匹配的订阅源")
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                AlphabetIndexSidebar(
                    letters = ALPHABET_INDEX_LETTERS,
                    availableLetters = availableLetters,
                    currentLetter = currentIndexLetter,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 4.dp, top = 12.dp, bottom = 96.dp),
                    onLetterClick = { letter ->
                        val targetIndex = alphabetIndexTargets[letter] ?: return@AlphabetIndexSidebar
                        coroutineScope.launch {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                )

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        start = 12.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    groupedSources.forEach { group ->
                        val isCollapsed = group.name in collapsedGroupSet
                        stickyHeader(key = "header_${group.name}") {
                            SourceGroupHeader(
                                groupName = group.name,
                                count = group.sources.size,
                                isCollapsed = isCollapsed,
                                canRename = true,
                                onToggle = {
                                    if (isCollapsed) {
                                        collapsedGroups.remove(group.name)
                                    } else {
                                        collapsedGroups.add(group.name)
                                    }
                                },
                                onRename = { groupToRename = group.name }
                            )
                        }
                        if (!isCollapsed) {
                            items(group.sources, key = { it.item.source.id }) { indexedItem ->
                                val item = indexedItem.item
                                val isSelected = selectedIds.contains(item.source.id)

                                SourceItem(
                                    item = item,
                                    groupLabel = group.name,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onDelete = {
                                        sourceToDelete = item.source
                                        showDeleteConfirmDialog = true
                                    },
                                    onEdit = { onEditSource(item.source.id) },
                                    onMoveToGroup = { sourceToMove = item.source },
                                    onUpdate = {
                                        Toast.makeText(
                                            context,
                                            "开始更新: ${item.source.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        viewModel.syncSource(item.source.id) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                Toast.makeText(
                                                    context,
                                                    "${item.source.name} 更新完成",
                                                    Toast.LENGTH_SHORT
                                                ).show()
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
                                            if (isSelected) selectedIds.remove(item.source.id) else selectedIds.add(
                                                item.source.id
                                            )
                                        } else {
                                            onSourceClick(item.source.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        groupToRename?.let { groupName ->
            RenameGroupDialog(
                title = if (groupName == SourceViewModel.DEFAULT_GROUP_NAME) "为未分组创建分组" else "重命名分组",
                initialGroupName = if (groupName == SourceViewModel.DEFAULT_GROUP_NAME) "" else groupName,
                onDismiss = { groupToRename = null },
                onConfirm = { newGroupName ->
                    val oldGroupName = groupName
                    viewModel.renameGroup(oldGroupName, newGroupName)

                    if (oldGroupName in collapsedGroups) {
                        collapsedGroups.remove(oldGroupName)
                        if (newGroupName !in collapsedGroups) {
                            collapsedGroups.add(newGroupName)
                        }
                    }

                    groupToRename = null
                }
            )
        }

        sourceToMove?.let { source ->
            MoveSourceGroupDialog(
                source = source,
                existingGroups = uiState.availableGroups,
                onDismiss = { sourceToMove = null },
                onConfirm = { groupName ->
                    viewModel.updateSourceGroup(source, groupName)
                    sourceToMove = null
                }
            )
        }

        // [新增] 导入策略选择对话框
        if (showImportStrategyDialog) {
            ImportStrategyDialog(
                onDismiss = {
                    showImportStrategyDialog = false
                    pendingImportContent = null
                },
                onConfirm = { strategy ->
                    showImportStrategyDialog = false
                    val content = pendingImportContent
                    pendingImportContent = null

                    if (!content.isNullOrBlank()) {
                        coroutineScope.launch {
                            val result = viewModel.importFromJson(content, strategy)

                            // 根据结果显示不同的提示
                            val message = when {
                                result.hasError -> result.error ?: "导入失败"
                                result.total == 0 -> "文件内容无效或解析失败"
                                strategy == ImportStrategy.SKIP && result.imported > 0 && result.skipped > 0 ->
                                    "成功导入 ${result.imported} 个，跳过 ${result.skipped} 个重复"
                                strategy == ImportStrategy.SKIP && result.skipped > 0 ->
                                    "全部 ${result.skipped} 个订阅源都已存在"
                                strategy == ImportStrategy.UPDATE && result.updated > 0 ->
                                    "成功导入 ${result.imported} 个，更新 ${result.updated} 个"
                                else -> "成功导入 ${result.imported} 个订阅源"
                            }

                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                            if (result.imported > 0 || result.updated > 0) {
                                showSimpleDialog = false
                            }
                        }
                    }
                }
            )
        }

        // 删除确认对话框
        if (showDeleteConfirmDialog) {
            DeleteConfirmDialog(
                source = sourceToDelete,
                sources = sourcesToDelete,
                onDismiss = {
                    showDeleteConfirmDialog = false
                    sourceToDelete = null
                    sourcesToDelete = null
                },
                onConfirm = {
                    if (sourceToDelete != null) {
                        viewModel.deleteSource(sourceToDelete!!)
                    } else if (sourcesToDelete != null) {
                        viewModel.deleteSources(sourcesToDelete!!)
                    }
                    showDeleteConfirmDialog = false
                    sourceToDelete = null
                    sourcesToDelete = null
                    isSelectionMode = false
                    selectedIds.clear()
                }
            )
        }

        if (showSimpleDialog) {
            SimpleAddDialog(
                onDismiss = {
                    previewSearchJob?.cancel()
                    previewSearchJob = null
                    isSearchingPreview = false
                    showSimpleDialog = false
                },
                onConfirm = { _, url ->
                    val job = coroutineScope.launch {
                        isSearchingPreview = true
                        try {
                            val previewResult = viewModel.fetchFeedPreview(url)
                            if (previewResult.articles.isNotEmpty()) {
                                previewViewModel.setPreview(
                                    title = previewResult.title,
                                    url = url,
                                    articles = previewResult.articles,
                                    iconUrl = previewResult.iconUrl,
                                    returnRoute = NavRoutes.SOURCES
                                )
                                showSimpleDialog = false
                                onOpenFeedPreview()
                            } else {
                                Toast.makeText(context, "未找到可预览的文章，请检查订阅源地址", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: CancellationException) {
                            // 用户主动取消搜索时不提示错误
                        } finally {
                            isSearchingPreview = false
                            previewSearchJob = null
                        }
                    }
                    previewSearchJob = job
                },
                onSwitchToAdvanced = { name, url ->
                    showSimpleDialog = false
                    onOpenAdvanced(name, url)
                },
                onImportFromClipboard = {
                    val clipboardContent = clipboardManager.getText()?.text
                    if (!clipboardContent.isNullOrBlank()) {
                        coroutineScope.launch {
                            // 先检测是否有重复
                            val hasDuplicates = viewModel.hasDuplicates(clipboardContent)
                            if (hasDuplicates) {
                                // 有重复，显示策略选择对话框
                                pendingImportContent = clipboardContent
                                showImportStrategyDialog = true
                            } else {
                                // 没有重复，直接导入
                                val result = viewModel.importFromJson(clipboardContent, ImportStrategy.SKIP)
                                val message = if (result.hasError) {
                                    result.error ?: "导入失败"
                                } else if (result.imported > 0) {
                                    "成功导入 ${result.imported} 个订阅源"
                                } else {
                                    "文件内容无效或解析失败"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (result.imported > 0) {
                                    showSimpleDialog = false
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
                    }
                },
                onImportFromFile = { importFileLauncher.launch(arrayOf("*/*")) },
                showNameInput = false,
                showImportButton = true,
                confirmText = "搜索",
                isLoading = isSearchingPreview,
                loadingText = "正在获取订阅源信息..."
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceItem(
    item: SourceWithStat,
    groupLabel: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onMoveToGroup: () -> Unit,
    onUpdate: () -> Unit,
    onShare: () -> Unit,
    onMarkAllRead: () -> Unit,
    onMarkAllUnread: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val source = item.source

    // Pre-load content description strings
    val descSourceCard = context.getString(R.string.desc_source_card)
    val descMenuMore = context.getString(R.string.desc_menu_more)
    val descButtonEditSource = context.getString(R.string.desc_button_edit_source)
    val descButtonDeleteSource = context.getString(R.string.desc_button_delete_source)
    val descSync = context.getString(R.string.desc_sync)
    val descMarkAllRead = context.getString(R.string.desc_mark_all_read)
    val descMarkAllUnread = context.getString(R.string.desc_mark_all_unread)
    val descShare = context.getString(R.string.desc_share)
    val descCheckboxSelectSource = context.getString(R.string.desc_checkbox_select_source)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics { contentDescription = descSourceCard }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.semantics { contentDescription = descCheckboxSelectSource }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp) // [新增] 强制留出右侧空间，防止文字与图标挤在一起
            ) {
                Text(text = source.name, style = MaterialTheme.typography.titleMedium)
                Text(text = source.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(
                    text = "分组：$groupLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                val unread = item.total - item.read
                Text(
                    text = "已读 ${item.read} / 未读 $unread / 总计 ${item.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unread > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            descMenuMore
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("更新") },
                            onClick = { showMenu = false; onUpdate() },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            modifier = Modifier.semantics { contentDescription = descSync }
                        )
                        DropdownMenuItem(
                            text = { Text("全部已读") },
                            onClick = { showMenu = false; onMarkAllRead() },
                            leadingIcon = { Icon(Icons.Default.DoneAll, null) },
                            modifier = Modifier.semantics { contentDescription = descMarkAllRead }
                        )
                        DropdownMenuItem(
                            text = { Text("全部未读") },
                            onClick = { showMenu = false; onMarkAllUnread() },
                            leadingIcon = { Icon(Icons.Default.RemoveDone, null) },
                            modifier = Modifier.semantics { contentDescription = descMarkAllUnread }
                        )
                        DropdownMenuItem(
                            text = { Text("移动分组") },
                            onClick = { showMenu = false; onMoveToGroup() },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("分享") },
                            onClick = { showMenu = false; onShare() },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            modifier = Modifier.semantics { contentDescription = descShare }
                        )
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            modifier = Modifier.semantics { contentDescription = descButtonEditSource }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "删除",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier.semantics { contentDescription = descButtonDeleteSource }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Row(
        modifier = modifier
            .height(40.dp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "搜索订阅源",
            tint = secondaryContentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = "搜索订阅源...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryContentColor
                    )
                }
                innerTextField()
            }
        )
        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "清空搜索",
                    modifier = Modifier.size(16.dp),
                    tint = secondaryContentColor
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SourceGroupHeader(
    groupName: String,
    count: Int,
    isCollapsed: Boolean,
    canRename: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = onToggle,
                onLongClick = {
                    if (canRename) {
                        onRename()
                    }
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCollapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isCollapsed) "展开分组" else "折叠分组",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$groupName ($count)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AlphabetIndexSidebar(
    letters: List<String>,
    availableLetters: Set<String>,
    currentLetter: String?,
    modifier: Modifier = Modifier,
    onLetterClick: (String) -> Unit
) {
    Column(
        modifier = modifier.width(24.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            val enabled = letter in availableLetters
            val isCurrent = letter == currentLetter
            Text(
                text = letter,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                    enabled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(20.dp)
                    .background(
                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .clickable(enabled = enabled) { onLetterClick(letter) }
                    .padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun RenameGroupDialog(
    title: String,
    initialGroupName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupName by remember(initialGroupName) { mutableStateOf(initialGroupName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("新的分组名称") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(groupName.trim()) },
                enabled = groupName.trim().isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun MoveSourceGroupDialog(
    source: Source,
    existingGroups: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupName by remember(source.id, source.groupName) { mutableStateOf(source.groupName) }
    val recommendedGroups = remember(existingGroups, source.groupName) {
        existingGroups.filter { it != source.groupName.trim() }.take(8)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动到分组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("为「${source.name}」设置分组名称，留空表示未分组。")
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("分组名称") },
                    placeholder = { Text("例如：技术、资讯、播客") }
                )
                if (recommendedGroups.isNotEmpty()) {
                    Text(
                        text = "已有分组",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    recommendedGroups.forEach { group ->
                        Text(
                            text = group,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { groupName = group }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(groupName.trim()) }) {
                Text("保存")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (source.groupName.isNotBlank()) {
                    TextButton(onClick = { onConfirm("") }) {
                        Text("移出分组")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

private fun buildAlphabetIndexTargets(entries: List<SourceListEntry>): Map<String, Int> {
    val indexMap = linkedMapOf<String, Int>()
    entries.forEachIndexed { index, entry ->
        if (entry is SourceListEntry.Item) {
            indexMap.putIfAbsent(entry.indexedItem.indexLetter, index)
        }
    }
    return indexMap
}

private fun buildVisibleSourceEntries(
    groups: List<SourceGroup>,
    collapsedGroups: Set<String>
): List<SourceListEntry> {
    val entries = mutableListOf<SourceListEntry>()

    groups.forEach { group ->
        val isCollapsed = group.name in collapsedGroups
        entries.add(
            SourceListEntry.Header(
                groupName = group.name,
                count = group.sources.size,
                isCollapsed = isCollapsed
            )
        )
        if (!isCollapsed) {
            group.sources.forEach { indexedItem ->
                entries.add(
                    SourceListEntry.Item(
                        groupName = group.name,
                        indexedItem = indexedItem
                    )
                )
            }
        }
    }

    return entries
}

private fun findCurrentIndexLetter(
    firstVisibleItemIndex: Int,
    entries: List<SourceListEntry>
): String? {
    if (entries.isEmpty()) {
        return null
    }

    val safeStartIndex = firstVisibleItemIndex.coerceIn(0, entries.lastIndex)

    for (index in safeStartIndex..entries.lastIndex) {
        val entry = entries[index]
        if (entry is SourceListEntry.Item) {
            return entry.indexedItem.indexLetter
        }
    }

    for (index in safeStartIndex downTo 0) {
        val entry = entries[index]
        if (entry is SourceListEntry.Item) {
            return entry.indexedItem.indexLetter
        }
    }

    return null
}

private sealed interface SourceListEntry {
    data class Header(
        val groupName: String,
        val count: Int,
        val isCollapsed: Boolean
    ) : SourceListEntry

    data class Item(
        val groupName: String,
        val indexedItem: IndexedSourceWithStat
    ) : SourceListEntry
}

private val ALPHABET_INDEX_LETTERS = ('A'..'Z').map { it.toString() } + "#"

// [修复] 还原为横向布局，左侧功能，右侧操作
@Composable
fun SimpleAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onSwitchToAdvanced: (String, String) -> Unit,
    onImportFromClipboard: () -> Unit,
    onImportFromFile: () -> Unit,
    showNameInput: Boolean = true,
    showImportButton: Boolean = true,
    confirmText: String = "保存",
    initialName: String = "",
    initialUrl: String = "",
    isLoading: Boolean = false,
    loadingText: String = "正在处理..."
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var showImportMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        title = { Text("添加 RSS 订阅") },
        text = {
            Column {
                if (showNameInput) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("名称") },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("RSS 地址") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = loadingText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：辅助功能组
                    if (showImportButton) {
                        TextButton(
                            enabled = !isLoading,
                            onClick = { showImportMenu = true }
                        ) {
                            Text("导入")
                        }
                    }
                    TextButton(
                        enabled = !isLoading,
                        onClick = { onSwitchToAdvanced(name.trim(), url.trim()) }
                    ) {
                        Text("自定义")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // 右侧：主要操作组
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(
                        enabled = !isLoading,
                        onClick = {
                            val trimmedName = name.trim()
                            val trimmedUrl = url.trim()
                            val canSubmit = trimmedUrl.isNotBlank() && (!showNameInput || trimmedName.isNotBlank())
                            if (canSubmit) onConfirm(trimmedName, trimmedUrl)
                        }
                    ) {
                        Text(confirmText)
                    }
                }

                // [优化] 导入选项下拉菜单 - 显示在导入按钮下方
                if (showImportButton) {
                    DropdownMenu(
                        expanded = showImportMenu,
                        onDismissRequest = { showImportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("从剪贴板导入")
                                }
                            },
                            onClick = {
                                showImportMenu = false
                                onImportFromClipboard()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("从本地文件导入")
                                }
                            },
                            onClick = {
                                showImportMenu = false
                                onImportFromFile()
                            }
                        )
                    }
                }
            }
        }
    )
}

// [新增] 导入策略选择对话框
@Composable
fun ImportStrategyDialog(
    onDismiss: () -> Unit,
    onConfirm: (ImportStrategy) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择导入方式") },
        text = {
            Column {
                Text("检测到可能存在重复的订阅源，请选择处理方式：")
                Spacer(modifier = Modifier.height(8.dp))
                Text("• 跳过重复：只导入新的订阅源", style = MaterialTheme.typography.bodySmall)
                Text("• 覆盖重复：更新已存在的订阅源", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { onConfirm(ImportStrategy.SKIP) }) {
                    Text("跳过重复")
                }
                TextButton(onClick = { onConfirm(ImportStrategy.UPDATE) }) {
                    Text("覆盖重复")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// [新增] 删除确认对话框
@Composable
fun DeleteConfirmDialog(
    source: Source?,
    sources: List<Source>?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = when {
        sources != null -> "批量删除订阅源"
        source != null -> "删除订阅源"
        else -> ""
    }

    val message = when {
        sources != null -> "确定要删除选中的 ${sources.size} 个订阅源吗？删除后无法恢复，该订阅源的所有文章也会被删除。"
        source != null -> "确定要删除订阅源「${source.name}」吗？删除后无法恢复，该订阅源的所有文章也会被删除。"
        else -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
            }) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
