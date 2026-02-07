package com.lengyuefenghua.newsreader.utils

interface AutoExtractor {
    fun extract(html: String): String
}

// 一个简单的占位实现
object DefaultAutoExtractor : AutoExtractor {
    override fun extract(html: String): String {
        // TODO: 这里实现你的智能正文提取算法
        // 暂时返回一段模拟文本
        return "【自动提取算法结果】\n检测到正文内容长度：${html.length}...\n(此处应显示算法处理后的纯净文本)"
    }
}