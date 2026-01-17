package com.lengyuefenghua.newsreader.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.viewmodel.EditSourceViewModel
import com.lengyuefenghua.newsreader.viewmodel.EditSourceViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSourceScreen(
    sourceId: Int = -1, // 传入 ID
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDebug: (String) -> Unit
) {
    // 1. 获取数据库实例
    val context = LocalContext.current
    val database = (context.applicationContext as NewsReaderApplication).database

    // 2. 获取 ViewModel (使用 Factory 注入 Dao)
    val viewModel: EditSourceViewModel = viewModel(
        factory = EditSourceViewModelFactory(database.sourceDao())
    )

    // 3. 初始化数据 (仅第一次有效)
    LaunchedEffect(sourceId) {
        viewModel.loadSourceIfNeed(sourceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (sourceId == -1) "配置订阅源" else "编辑订阅源") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null
                        )
                    }
                },
                actions = {
                    // 调试按钮：从 VM 获取最新数据
                    IconButton(onClick = {
                        val source = viewModel.buildSource()
                        val json = Uri.encode(Gson().toJson(source))
                        onDebug(json)
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "调试")
                    }
                    // 保存按钮
                    IconButton(onClick = {
                        if (viewModel.name.isNotBlank() && viewModel.url.isNotBlank()) {
                            viewModel.saveSource()
                            onSave()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- 现在所有的 value 都直接绑定到 viewModel.xxx ---
            // 这样无论怎么跳转，只要 ViewModel 不死，数据就在

            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.url,
                onValueChange = { viewModel.url = it },
                label = { Text("地址") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 模式切换
            TabRow(selectedTabIndex = viewModel.selectedTab) {
                Tab(
                    selected = viewModel.selectedTab == 0,
                    onClick = { viewModel.selectedTab = 0 },
                    text = { Text("标准 RSS / 混合") })
                Tab(
                    selected = viewModel.selectedTab == 1,
                    onClick = { viewModel.selectedTab = 1 },
                    text = { Text("完全自定义") })
            }

            if (viewModel.selectedTab == 0) {
                // RSS 模式
                Text(
                    "高级选项：正文抓取",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = viewModel.useAutoExtract,
                        onCheckedChange = { viewModel.useAutoExtract = it })
                    Text("启用自动正文提取算法")
                }
                if (!viewModel.useAutoExtract) {
                    OutlinedTextField(
                        value = viewModel.ruleContent,
                        onValueChange = { viewModel.ruleContent = it },
                        label = { Text("正文 CSS 选择器") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {

                Text(
                    "列表抓取规则",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.ruleList,
                    onValueChange = { viewModel.ruleList = it },
                    label = { Text("列表容器") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.ruleTitle,
                    onValueChange = { viewModel.ruleTitle = it },
                    label = { Text("标题规则") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.ruleLink,
                    onValueChange = { viewModel.ruleLink = it },
                    label = { Text("链接规则") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.ruleImage,
                    onValueChange = { viewModel.ruleImage = it },
                    label = { Text("图片规则") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.ruleSummary,
                    onValueChange = { viewModel.ruleSummary = it },
                    label = { Text("摘要规则 (选填)") },
                    placeholder = { Text("提取列表中的简介文字") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("高级选项", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                // 1. WebView 复选框
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.requestMethod = !viewModel.requestMethod
                    }
                ) {
                    Checkbox(
                        checked = viewModel.requestMethod,
                        onCheckedChange = {
                            viewModel.requestMethod = it
                        }
                    )
                    Text("使用 WebView 加载地址")
                }

                // 2. 模拟 PC 复选框
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.enablePcUserAgent = !viewModel.enablePcUserAgent
                    }
                ) {
                    Checkbox(
                        checked = viewModel.enablePcUserAgent,
                        onCheckedChange = { viewModel.enablePcUserAgent = it }
                    )
                    Text("模拟 PC 浏览器")
                }
            }
        }
    }
}