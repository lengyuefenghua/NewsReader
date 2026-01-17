package com.lengyuefenghua.newsreader.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey val id: String,// 唯一标识
    val title: String,       // 标题
    val summary: String,     // 摘要
    val content: String?,    // 正文
    val sourceName: String,  // 来源
    val pubDate: String,     // 发布时间
    val url: String          // 原文链接
)
