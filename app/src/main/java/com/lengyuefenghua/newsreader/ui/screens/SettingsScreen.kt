package com.lengyuefenghua.newsreader.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.R
import com.lengyuefenghua.newsreader.ui.common.FilterType
import com.lengyuefenghua.newsreader.ui.common.displayName
import com.lengyuefenghua.newsreader.util.SettingsManager
import com.lengyuefenghua.newsreader.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
private fun SettingRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    settingsManager: SettingsManager = koinInject()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val autoUpdate by viewModel.autoUpdate.collectAsState()
    val cacheLimit by viewModel.cacheLimit.collectAsState()

    var concurrentCount by remember { mutableIntStateOf(settingsManager.getConcurrentCount()) }
    var defaultFilter by remember { mutableStateOf(settingsManager.getDefaultFilterType()) }
    var tempLimit by remember { mutableStateOf(cacheLimit.toString()) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val msg = viewModel.saveBackupToFile(uri, context)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val data = viewModel.loadBackupFromFile(uri, context)
                    if (data != null) {
                        pendingRestoreUri = uri
                        showRestoreDialog = true
                    } else {
                        Toast.makeText(context, "备份文件格式错误", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.desc_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 自动更新
            SettingRow(label = "自动更新") {
                Switch(
                    checked = autoUpdate,
                    onCheckedChange = { viewModel.setAutoUpdate(it) }
                )
            }

            // 并发刷新
            SettingRow(label = "并发刷新") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (concurrentCount > 1) {
                                concurrentCount--
                                settingsManager.setConcurrentCount(concurrentCount)
                            }
                        },
                        enabled = concurrentCount > 1
                    ) {
                        Text("−", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "$concurrentCount",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.width(30.dp)
                    )
                    IconButton(
                        onClick = {
                            if (concurrentCount < 5) {
                                concurrentCount++
                                settingsManager.setConcurrentCount(concurrentCount)
                            }
                        },
                        enabled = concurrentCount < 5
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // 默认筛选
            SettingRow(label = "默认筛选") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterType.values().forEach { type ->
                        FilterChip(
                            selected = defaultFilter == type,
                            onClick = {
                                defaultFilter = type
                                settingsManager.setDefaultFilterType(type)
                            },
                            label = { Text(type.displayName, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }

            // 文章保留
            SettingRow(label = "文章保留") {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(36.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = tempLimit,
                        onValueChange = {
                            tempLimit = it
                            it.toIntOrNull()?.let { num ->
                                if (num > 0) viewModel.setCacheLimit(num)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        singleLine = true
                    )
                }
            }

            // 清理缓存
            SettingRow(label = "清理缓存") {
                Button(
                    onClick = { viewModel.clearCacheNow() },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("清理", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 数据备份
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        backupLauncher.launch("NewsReader_${System.currentTimeMillis()}.json")
                    },
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("备份", style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/json"))
                    },
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("恢复", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // 恢复确认对话框
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("确认恢复") },
            text = { Text("恢复将覆盖所有数据，是否继续？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val uri = pendingRestoreUri ?: return@launch
                            viewModel.loadBackupFromFile(uri, context)?.let {
                                Toast.makeText(context, viewModel.restoreBackup(it), Toast.LENGTH_LONG).show()
                            }
                            showRestoreDialog = false
                            pendingRestoreUri = null
                        }
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        pendingRestoreUri = null
                    }
                ) { Text("取消") }
            }
        )
    }
}
