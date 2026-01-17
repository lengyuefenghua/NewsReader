package com.lengyuefenghua.newsreader.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.utils.DebugHelper
import kotlinx.coroutines.launch
import java.net.URLDecoder

// 需要添加这个 OptIn 注解才能使用 combinedClickable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DebugConsoleScreen(
    sourceJson: String,
    onBack: () -> Unit
) {
    val source = remember {
        try {
            val json = URLDecoder.decode(sourceJson, "UTF-8")
            Gson().fromJson(json, Source::class.java)
        } catch (_: Exception) {
            null
        }
    }

    // 状态管理
    var logs by remember { mutableStateOf("正在初始化调试环境...\n") }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // 获取剪贴板管理器和上下文
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // 定义复制操作函数
    fun copyLogs() {
        if (logs.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(logs))
            // 在 Android 13+ 系统会自动提示复制成功，低版本手动弹 Toast
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
            } else {
                // 哪怕是新系统，为了交互明确，弹个短提示也不错
                Toast.makeText(context, "已复制全部日志", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(source) {
        if (source != null) {
            // 这里传入 context，确保 DebugHelper 能用 WebView
            scope.launch {
                val result = DebugHelper.runNewTest(context, source)
                logs += result
            }
        } else {
            logs += "❌ 错误：参数解析失败。"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调试控制台", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null
                        )
                    }
                },
                actions = {
                    // [新增] 顶部栏复制按钮 (图标这里用个通用的，如果有 Copy 图标最好)
                    // Material Icons 默认可能没有 Copy，可以用 Share 或者 Info 代替，或者直接 TextButton
                    TextButton(onClick = { copyLogs() }) {
                        Text("复制日志")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 移除 SelectionContainer，直接使用 Box + Text 配合 combinedClickable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E)) // 深色背景
                    .padding(16.dp)
            ) {
                Text(
                    text = logs,
                    color = Color(0xFF00FF00), // 绿色极客风文字
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .verticalScroll(scrollState)
                )
            }
        }
    }
}