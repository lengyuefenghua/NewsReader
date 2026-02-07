package com.lengyuefenghua.newsreader.core.domain.model

sealed class NetworkError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class NetworkException(
        override val message: String,
        override val cause: Throwable? = null
    ) : NetworkError(message, cause)

    data class ParseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : NetworkError(message, cause)

    data class DatabaseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : NetworkError(message, cause)

    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : NetworkError(message, cause)

    // 用户友好的错误信息
    val userMessage: String
        get() = when (this) {
            is NetworkException -> "网络连接失败，请检查网络设置"
            is ParseError -> "数据解析失败，请检查订阅源配置"
            is DatabaseError -> "数据存储失败"
            is UnknownError -> message
        }
}
