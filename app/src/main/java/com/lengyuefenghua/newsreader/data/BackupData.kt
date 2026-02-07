package com.lengyuefenghua.newsreader.data

import com.google.gson.annotations.SerializedName

/**
 * 完整数据备份模型
 * 用于序列化和反序列化应用的所有数据
 */
data class BackupData(
    @SerializedName("version")
    val version: String,              // 备份格式版本

    @SerializedName("backupDate")
    val backupDate: String,           // 备份时间戳 (ISO 8601 格式)

    @SerializedName("databaseVersion")
    val databaseVersion: Int,         // 数据库版本号

    @SerializedName("sources")
    val sources: List<Source>,        // 所有订阅源

    @SerializedName("articles")
    val articles: List<Article>,      // 所有文章

    @SerializedName("settings")
    val settings: BackupSettings      // 用户设置
)

/**
 * 用户设置备份数据
 */
data class BackupSettings(
    @SerializedName("autoUpdate")
    val autoUpdate: Boolean,          // 自动更新开关

    @SerializedName("cacheLimit")
    val cacheLimit: Int               // 缓存限制数量
)
