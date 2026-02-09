package com.lengyuefenghua.newsreader.ui.common

import com.lengyuefenghua.newsreader.core.domain.model.NetworkError

sealed class UiEvent {
    data class Toast(val message: String, val duration: Int = 2000) : UiEvent()
    data class ShowError(val error: NetworkError) : UiEvent()
    object ScrollToTop : UiEvent()
    data class Navigate(val route: String) : UiEvent()

    // [新增] 刷新完成事件（简化版：仅保留总计提示，移除单源进度）
    data class RefreshCompleted(
        val totalNew: Int,
        val failedSources: Int
    ) : UiEvent()
}
