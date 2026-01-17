package com.lengyuefenghua.newsreader.data

data class Article(
    val id: String,          // 唯一标识
    val title: String,       // 标题
    val summary: String,     // 摘要
    val content: String?,    // 正文
    val sourceName: String,  // 来源
    val pubDate: String,     // 发布时间
    val url: String          // 原文链接
)
