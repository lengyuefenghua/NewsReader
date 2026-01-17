package com.lengyuefenghua.newsreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "sources")
data class Source(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,

    // --- 模式: 是否为完全自定义列表抓取 ---
    val isCustom: Boolean = false,
    // 请求方式: 0=HTTP-GET (默认), 1=WebView (用于反爬/动态网页)
    val requestMethod: Boolean = false,
    // 是否模拟 PC 浏览器 UA (默认为 false，即 Android)
    val enablePcUserAgent: Boolean = false,
    // --- 列表抓取规则 (isCustom = true 时生效) ---
    val ruleList: String = "",
    val ruleTitle: String = "",
    val ruleLink: String = "",
    val ruleImage: String = "",
    val ruleSummary: String = "",

    // --- 正文抓取规则 (通用，但主要用于 RSS 只有摘要时) ---
    // CSS 选择器，例如: div.article-content
    val ruleContent: String = "",
    // 是否启用自动提取算法 (优先于 ruleContent)
    val useAutoExtract: Boolean = false
) : Serializable