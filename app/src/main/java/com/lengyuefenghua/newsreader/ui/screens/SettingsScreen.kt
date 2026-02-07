package com.lengyuefenghua.newsreader.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.R
import com.lengyuefenghua.newsreader.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val autoUpdate by viewModel.autoUpdate.collectAsState()
    val cacheLimit by viewModel.cacheLimit.collectAsState()

    // 临时状态，用于输入框编辑
    var tempLimit by remember(cacheLimit) { mutableStateOf(cacheLimit.toString()) }

    // 数据备份相关状态
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Pre-load content description strings
    val descSettingAutoRefresh = context.getString(R.string.desc_setting_auto_refresh)
    val descSettingClearCache = context.getString(R.string.desc_setting_clear_cache)

    // 备份文件选择器 launcher
    val backupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val message = viewModel.saveBackupToFile(uri, context)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // 恢复文件选择器 launcher
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val backupData = viewModel.loadBackupFromFile(uri, context)
                    if (backupData != null) {
                        pendingRestoreUri = uri
                        showRestoreConfirmDialog = true
                    } else {
                        Toast.makeText(context, "备份文件格式错误或已损坏", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "读取备份文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 自动更新开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自动更新", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "打开应用时自动检查更新",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = autoUpdate,
                    onCheckedChange = { viewModel.setAutoUpdate(it) },
                    modifier = Modifier.semantics { contentDescription = descSettingAutoRefresh }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // 缓存数量设置
            Text("缓存管理", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = tempLimit,
                onValueChange = {
                    tempLimit = it
                    // 仅当输入是纯数字时尝试保存
                    val num = it.toIntOrNull()
                    if (num != null && num > 0) {
                        viewModel.setCacheLimit(num)
                    }
                },
                label = { Text("文章保留数量 (条)") },
                supportingText = { Text("超出此数量的旧文章将被清理（已收藏除外）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.clearCacheNow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = descSettingClearCache }
            ) {
                Text("立即清理过期缓存")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // 数据备份区域
            Text("数据备份", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "备份所有订阅源、文章和设置，可用于跨设备迁移或升级后恢复数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val fileName = "NewsReader_Full_Backup_${System.currentTimeMillis()}.json"
                        backupFileLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📤 备份数据")
                }
                Button(
                    onClick = {
                        restoreFileLauncher.launch(arrayOf("application/json"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📥 恢复数据")
                }
            }
        }

        // 恢复确认对话框
        if (showRestoreConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirmDialog = false },
                title = { Text("确认恢复数据") },
                text = {
                    Text("恢复数据将覆盖现有所有数据（订阅源、文章、设置等），此操作不可撤销。是否继续？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val uri = pendingRestoreUri ?: return@launch
                                val backupData = viewModel.loadBackupFromFile(uri, context)
                                if (backupData != null) {
                                    val message = viewModel.restoreBackup(backupData)
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                                showRestoreConfirmDialog = false
                                pendingRestoreUri = null
                            }
                        }
                    ) {
                        Text("确认恢复")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRestoreConfirmDialog = false
                            pendingRestoreUri = null
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}