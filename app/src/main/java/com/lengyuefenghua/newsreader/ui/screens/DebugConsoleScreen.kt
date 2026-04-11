package com.lengyuefenghua.newsreader.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.lengyuefenghua.newsreader.R
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.utils.DebugHelper
import com.lengyuefenghua.newsreader.utils.DebugResult
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
    var debugResult by remember { mutableStateOf<DebugResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var showRaw by remember { mutableStateOf(false) } // true=显示源码, false=显示日志
    
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // 获取剪贴板管理器和上下文
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Pre-load content description strings
    val descButtonCopyLog = context.getString(R.string.desc_button_copy_log)
    val descButtonViewLog = context.getString(R.string.desc_button_view_log)
    val descButtonViewSource = context.getString(R.string.desc_button_view_source)

    // 定义复制操作函数
    fun copyLogs() {
        val textToCopy = if (showRaw) debugResult?.rawSource else debugResult?.log
        if (!textToCopy.isNullOrBlank()) {
            clipboardManager.setText(AnnotatedString(textToCopy))
            // 在 Android 13+ 系统会自动提示复制成功，低版本手动弹 Toast
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, "内容已复制到剪贴板", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "已复制全部内容", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(source) {
        isLoading = true
        errorMessage = ""
        debugResult = null // 重置
        
        if (source != null) {
            // 这里传入 context，确保 DebugHelper 能用 WebView
            scope.launch {
                debugResult = DebugHelper.runNewTest(context, source)
                isLoading = false
            }
        } else {
            errorMessage = "❌ 错误：参数解析失败。"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showRaw) "网页源码" else "调试日志", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.desc_back)
                        )
                    }
                },
                actions = {
                    // [新增] 切换按钮
                    TextButton(
                        onClick = { showRaw = !showRaw },
                        modifier = Modifier.semantics { contentDescription = if (showRaw) descButtonViewSource else descButtonViewLog }
                    ) {
                        Text(if (showRaw) "查看日志" else "查看源码")
                    }
                    // 复制按钮
                    TextButton(
                        onClick = { copyLogs() },
                        modifier = Modifier.semantics { contentDescription = descButtonCopyLog }
                    ) {
                        Text("复制")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E)) // 深色背景
                    .padding(16.dp)
            ) {
                val displayText = if (isLoading) "正在启动调试任务...\n"
                                  else if (errorMessage.isNotEmpty()) errorMessage
                                  else if (showRaw) debugResult?.rawSource ?: "无源码数据"
                                  else debugResult?.log ?: "无日志数据"
                                  
                SelectionContainer {
                    Text(
                        text = displayText,
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
}
