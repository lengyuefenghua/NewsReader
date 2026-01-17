package com.lengyuefenghua.newsreader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val autoUpdate by viewModel.autoUpdate.collectAsState()
    val cacheLimit by viewModel.cacheLimit.collectAsState()

    // 临时状态，用于输入框编辑
    var tempLimit by remember(cacheLimit) { mutableStateOf(cacheLimit.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
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
                    Text("打开应用时自动检查更新", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Switch(
                    checked = autoUpdate,
                    onCheckedChange = { viewModel.setAutoUpdate(it) }
                )
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp))

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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("立即清理过期缓存")
            }
        }
    }
}