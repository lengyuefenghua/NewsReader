package com.lengyuefenghua.newsreader.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.viewmodel.EditSourceViewModel
import com.lengyuefenghua.newsreader.viewmodel.EditSourceViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSourceScreen(
    sourceId: Int = -1,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDebug: (String) -> Unit
) {
    val context = LocalContext.current
    val database = (context.applicationContext as NewsReaderApplication).database
    val viewModel: EditSourceViewModel = viewModel(
        factory = EditSourceViewModelFactory(database.sourceDao())
    )
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(sourceId) {
        viewModel.loadSourceIfNeed(sourceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (sourceId == -1) "配置订阅源" else "编辑订阅源") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    // [新增] 剪贴板导入/粘贴按钮
                    IconButton(onClick = {
                        val text = clipboardManager.getText()?.text
                        if (!text.isNullOrBlank()) {
                            try {
                                val source = Gson().fromJson(text, Source::class.java)
                                if (source != null) {
                                    // 填充数据到 ViewModel
                                    viewModel.name = source.name
                                    viewModel.url = source.url
                                    viewModel.selectedTab = if (source.isCustom) 1 else 0
                                    viewModel.useAutoExtract = source.useAutoExtract
                                    viewModel.ruleContent = source.ruleContent
                                    viewModel.requestMethod = source.requestMethod
                                    viewModel.enablePcUserAgent = source.enablePcUserAgent
                                    viewModel.ruleList = source.ruleList
                                    viewModel.ruleTitle = source.ruleTitle
                                    viewModel.ruleLink = source.ruleLink
                                    viewModel.ruleSummary = source.ruleSummary
                                    viewModel.ruleImage = source.ruleImage
                                    Toast.makeText(context, "配置已填入", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "格式错误，无法解析", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "从剪贴板粘贴")
                    }

                    // 调试按钮
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
            // (原有 UI 代码，保持不变)
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
                // RSS 模式 UI
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
                // 自定义模式 UI
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