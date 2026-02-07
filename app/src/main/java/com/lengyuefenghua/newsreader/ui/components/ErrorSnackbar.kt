package com.lengyuefenghua.newsreader.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import com.lengyuefenghua.newsreader.core.domain.model.NetworkError

/**
 * 统一的错误提示 SnackBar
 */
@Composable
fun ErrorSnackbar(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data
        )
    }
}

/**
 * 显示错误消息的扩展函数
 */
suspend fun SnackbarHostState.showError(error: NetworkError) {
    showSnackbar(
        message = error.userMessage,
        duration = androidx.compose.material3.SnackbarDuration.Long
    )
}
