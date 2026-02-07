package com.lengyuefenghua.newsreader.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.R
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

    LaunchedEffect(sourceId) {
        viewModel.loadSourceIfNeed(sourceId)
    }

    // Pre-load content description strings
    val descName = context.getString(R.string.desc_input_name)
    val descUrl = context.getString(R.string.desc_input_url)
    val descTabStandard = context.getString(R.string.desc_tab_standard)
    val descTabCustom = context.getString(R.string.desc_tab_custom)
    val descRadioNoExtract = context.getString(R.string.desc_radio_no_extract)
    val descRadioGne = context.getString(R.string.desc_radio_gne)
    val descRadioReadability = context.getString(R.string.desc_radio_readability)
    val descRadioCss = context.getString(R.string.desc_radio_css)
    val descInputRuleList = context.getString(R.string.desc_input_rule_list)
    val descInputRuleTitle = context.getString(R.string.desc_input_rule_title)
    val descInputRuleLink = context.getString(R.string.desc_input_rule_link)
    val descInputRuleImage = context.getString(R.string.desc_input_rule_image)
    val descInputRuleSummary = context.getString(R.string.desc_input_rule_summary)
    val descInputRuleContent = context.getString(R.string.desc_input_rule_content)
    val descCheckboxWebview = context.getString(R.string.desc_checkbox_webview)
    val descCheckboxPcUa = context.getString(R.string.desc_checkbox_pc_ua)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (sourceId == -1) "配置订阅源" else "编辑订阅源") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    // 调试按钮
                    IconButton(onClick = {
                        val source = viewModel.buildSource()
                        val json = Uri.encode(Gson().toJson(source))
                        onDebug(json)
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.desc_debug))
                    }
                    // 保存按钮
                    IconButton(onClick = {
                        if (viewModel.name.isNotBlank() && viewModel.url.isNotBlank()) {
                            viewModel.saveSource()
                            onSave()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.desc_save))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = descName
                        text = AnnotatedString(descName)
                        editableText = AnnotatedString(viewModel.name)
                    }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.url,
                onValueChange = { viewModel.url = it },
                label = { Text("地址") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = descUrl
                        text = AnnotatedString(descUrl)
                        editableText = AnnotatedString(viewModel.url)
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = viewModel.selectedTab) {
                Tab(
                    selected = viewModel.selectedTab == 0,
                    onClick = { viewModel.selectedTab = 0 },
                    text = { Text("标准 RSS / 混合") },
                    modifier = Modifier.semantics { contentDescription = descTabStandard }
                )
                Tab(
                    selected = viewModel.selectedTab == 1,
                    onClick = { viewModel.selectedTab = 1 },
                    text = { Text("完全自定义") },
                    modifier = Modifier.semantics { contentDescription = descTabCustom }
                )
            }

            if (viewModel.selectedTab == 0) {
                // RSS 模式 UI
                Text(
                    "正文获取方式",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // 选项1: 不提取（使用 RSS 原始内容）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.useAutoExtract = false
                        viewModel.ruleContent = ""
                        viewModel.extractionAlgorithm = "gne"
                    }
                ) {
                    RadioButton(
                        selected = !viewModel.useAutoExtract,
                        onClick = {
                            viewModel.useAutoExtract = false
                            viewModel.ruleContent = ""
                            viewModel.extractionAlgorithm = "gne"
                        },
                        modifier = Modifier.semantics { contentDescription = descRadioNoExtract }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("不提取（使用 RSS 原始内容）")
                }

                // 选项2: GNE 自动提取（文本密度算法）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.useAutoExtract = true
                        viewModel.ruleContent = ""
                        viewModel.extractionAlgorithm = "gne"
                    }
                ) {
                    RadioButton(
                        selected = viewModel.useAutoExtract && viewModel.extractionAlgorithm == "gne" && viewModel.ruleContent.isEmpty(),
                        onClick = {
                            viewModel.useAutoExtract = true
                            viewModel.ruleContent = ""
                            viewModel.extractionAlgorithm = "gne"
                        },
                        modifier = Modifier.semantics { contentDescription = descRadioGne }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("GNE 自动提取")
                        Text(
                            "基于文本密度智能识别（默认）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 选项3: Readability 提取（Mozilla 算法）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.useAutoExtract = true
                        viewModel.ruleContent = ""
                        viewModel.extractionAlgorithm = "readability"
                    }
                ) {
                    RadioButton(
                        selected = viewModel.useAutoExtract && viewModel.extractionAlgorithm == "readability",
                        onClick = {
                            viewModel.useAutoExtract = true
                            viewModel.ruleContent = ""
                            viewModel.extractionAlgorithm = "readability"
                        },
                        modifier = Modifier.semantics { contentDescription = descRadioReadability }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Readability 提取")
                        Text(
                            "Mozilla 算法（Firefox 阅读器使用）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 选项5: CSS 选择器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.useAutoExtract = true
                        viewModel.extractionAlgorithm = "custom"
                        // 如果 ruleContent 为空，设置一个占位值以选中此选项
                        if (viewModel.ruleContent.isEmpty()) {
                            viewModel.ruleContent = ".article-content"
                        }
                    }
                ) {
                    RadioButton(
                        selected = viewModel.useAutoExtract && viewModel.extractionAlgorithm == "custom" && viewModel.ruleContent.isNotEmpty(),
                        onClick = {
                            viewModel.useAutoExtract = true
                            viewModel.extractionAlgorithm = "custom"
                            // 如果 ruleContent 为空，设置一个占位值以选中此选项
                            if (viewModel.ruleContent.isEmpty()) {
                                viewModel.ruleContent = ".article-content"
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = descRadioCss }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("CSS 选择器")
                        Text(
                            "精确指定正文容器元素",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // CSS 选择器输入框（只在选择"CSS 选择器"时显示）
                if (viewModel.useAutoExtract && viewModel.extractionAlgorithm == "custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.ruleContent,
                        onValueChange = { viewModel.ruleContent = it },
                        label = { Text("正文 CSS 选择器") },
                        placeholder = { Text("例如：.article-content") },
                        supportingText = { Text("输入 CSS 选择器，如 .article-content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = descInputRuleContent
                                text = AnnotatedString(descInputRuleContent)
                                editableText = AnnotatedString(viewModel.ruleContent)
                            },
                        singleLine = true
                    )
                } else if (viewModel.useAutoExtract && viewModel.extractionAlgorithm == "readability") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "将使用 Readability 算法自动提取正文",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (viewModel.useAutoExtract && viewModel.extractionAlgorithm == "readability") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "将使用 Mozilla Readability 算法自动提取正文",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (viewModel.useAutoExtract && viewModel.extractionAlgorithm == "gne") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "将使用 GNE 算法自动提取正文",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = descInputRuleList
                            text = AnnotatedString(descInputRuleList)
                            editableText = AnnotatedString(viewModel.ruleList)
                        }
                )
                OutlinedTextField(
                    value = viewModel.ruleTitle,
                    onValueChange = { viewModel.ruleTitle = it },
                    label = { Text("标题规则") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = descInputRuleTitle
                            text = AnnotatedString(descInputRuleTitle)
                            editableText = AnnotatedString(viewModel.ruleTitle)
                        }
                )
                OutlinedTextField(
                    value = viewModel.ruleLink,
                    onValueChange = { viewModel.ruleLink = it },
                    label = { Text("链接规则") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = descInputRuleLink
                            text = AnnotatedString(descInputRuleLink)
                            editableText = AnnotatedString(viewModel.ruleLink)
                        }
                )
                OutlinedTextField(
                    value = viewModel.ruleImage,
                    onValueChange = { viewModel.ruleImage = it },
                    label = { Text("图片规则") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = descInputRuleImage
                            text = AnnotatedString(descInputRuleImage)
                            editableText = AnnotatedString(viewModel.ruleImage)
                        }
                )
                OutlinedTextField(
                    value = viewModel.ruleSummary,
                    onValueChange = { viewModel.ruleSummary = it },
                    label = { Text("摘要规则 (选填)") },
                    placeholder = { Text("提取列表中的简介文字") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = descInputRuleSummary
                            text = AnnotatedString(descInputRuleSummary)
                            editableText = AnnotatedString(viewModel.ruleSummary)
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "正文获取方式",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // 选项1: 不提取（使用列表中的摘要）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.useAutoExtract = false
                        viewModel.ruleContent = ""
                        viewModel.extractionAlgorithm = "gne"
                    }
                ) {
                    RadioButton(
                        selected = !viewModel.useAutoExtract,
                        onClick = {
                            viewModel.useAutoExtract = false
                            viewModel.ruleContent = ""
                            viewModel.extractionAlgorithm = "gne"
                        },
                        modifier = Modifier.semantics { contentDescription = descRadioNoExtract }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("不提取（使用列表中的摘要）")
                }

                // 选项2: GNE 自动提取（文本密度算法）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.useAutoExtract = true
                        viewModel.ruleContent = ""
                        viewModel.extractionAlgorithm = "gne"
                    }
                ) {
                    RadioButton(
                        selected = viewModel.useAutoExtract && viewModel.extractionAlgorithm == "gne" && viewModel.ruleContent.isEmpty(),
                        onClick = {
                            viewModel.useAutoExtract = true
                            viewModel.ruleContent = ""
                            viewModel.extractionAlgorithm = "gne"
                        },
                        modifier = Modifier.semantics { contentDescription = descRadioGne }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("GNE 自动提取")
                        Text(
                            "基于文本密度智能识别（默认）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 选项3: Readability 提取（Mozilla 算法）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.useAutoExtract = true
                        viewModel.ruleContent = ""
                        viewModel.extractionAlgorithm = "readability"
                    }
                ) {
                    RadioButton(
                        selected = viewModel.useAutoExtract && viewModel.extractionAlgorithm == "readability",
                        onClick = {
                            viewModel.useAutoExtract = true
                            viewModel.ruleContent = ""
                            viewModel.extractionAlgorithm = "readability"
                        },
                        modifier = Modifier.semantics { contentDescription = descRadioReadability }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Readability 提取")
                        Text(
                            "Mozilla 算法（Firefox 阅读器使用）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 选项5: CSS 选择器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.useAutoExtract = true
                        viewModel.extractionAlgorithm = "custom"
                        // 如果 ruleContent 为空，设置一个占位值以选中此选项
                        if (viewModel.ruleContent.isEmpty()) {
                            viewModel.ruleContent = ".article-content"
                        }
                    }
                ) {
                    RadioButton(
                        selected = viewModel.useAutoExtract && viewModel.extractionAlgorithm == "custom" && viewModel.ruleContent.isNotEmpty(),
                        onClick = {
                            viewModel.useAutoExtract = true
                            viewModel.extractionAlgorithm = "custom"
                            // 如果 ruleContent 为空，设置一个占位值以选中此选项
                            if (viewModel.ruleContent.isEmpty()) {
                                viewModel.ruleContent = ".article-content"
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = descRadioCss }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("CSS 选择器")
                        Text(
                            "精确指定正文容器元素",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // CSS 选择器输入框（只在选择"CSS 选择器"时显示）
                if (viewModel.useAutoExtract && viewModel.extractionAlgorithm == "custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.ruleContent,
                        onValueChange = { viewModel.ruleContent = it },
                        label = { Text("正文 CSS 选择器") },
                        placeholder = { Text("例如：.article-content") },
                        supportingText = { Text("输入 CSS 选择器，如 .article-content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = descInputRuleContent
                                text = AnnotatedString(descInputRuleContent)
                                editableText = AnnotatedString(viewModel.ruleContent)
                            },
                        singleLine = true
                    )
                } else if (viewModel.useAutoExtract && viewModel.extractionAlgorithm == "readability") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "将使用 Readability 算法自动提取正文",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (viewModel.useAutoExtract && viewModel.extractionAlgorithm == "readability") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "将使用 Mozilla Readability 算法自动提取正文",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (viewModel.useAutoExtract && viewModel.extractionAlgorithm == "gne") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "将使用 GNE 算法自动提取正文",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "高级选项",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

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
                        },
                        modifier = Modifier.semantics { contentDescription = descCheckboxWebview }
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
                        onCheckedChange = { viewModel.enablePcUserAgent = it },
                        modifier = Modifier.semantics { contentDescription = descCheckboxPcUa }
                    )
                    Text("模拟 PC 浏览器")
                }
            }
        }
    }
}