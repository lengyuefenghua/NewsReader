package com.lengyuefenghua.newsreader.ui.common

import com.lengyuefenghua.newsreader.core.domain.model.NetworkError

sealed class UiEvent {
    data class Toast(val message: String) : UiEvent()
    data class ShowError(val error: NetworkError) : UiEvent()
    object ScrollToTop : UiEvent()
    data class Navigate(val route: String) : UiEvent()
}
