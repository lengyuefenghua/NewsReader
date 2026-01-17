package com.lengyuefenghua.newsreader.data

import androidx.room.ColumnInfo
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
    val url: String,          // 原文链接
    val isRead: Boolean = false, // 是否已读
    val isFavorite: Boolean = false, // 是否收藏
    // [新增] 首次阅读时间戳 (毫秒)
    @ColumnInfo(defaultValue = "0")
    val readTimestamp: Long = 0,

    // [新增] 累计阅读时长 (毫秒)
    @ColumnInfo(defaultValue = "0")
    val readDuration: Long = 0
)
