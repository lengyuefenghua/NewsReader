package com.lengyuefenghua.newsreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(tableName = "sources")
data class Source(
    @PrimaryKey(autoGenerate = true) @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String,
    @SerializedName("iconUrl") val iconUrl: String? = null, // [新增] 订阅源图标

    // --- 模式: 是否为完全自定义列表抓取 ---
    @SerializedName("isCustom") val isCustom: Boolean = false,
    // 请求方式: 0=HTTP-GET (默认), 1=WebView (用于反爬/动态网页)
    @SerializedName("requestMethod") val requestMethod: Boolean = false,
    // 是否模拟 PC 浏览器 UA (默认为 false，即 Android)
    @SerializedName("enablePcUserAgent") val enablePcUserAgent: Boolean = false,
    // --- 列表抓取规则 (isCustom = true 时生效) ---
    @SerializedName("ruleList") val ruleList: String = "",
    @SerializedName("ruleTitle") val ruleTitle: String = "",
    @SerializedName("ruleLink") val ruleLink: String = "",
    @SerializedName("ruleImage") val ruleImage: String = "",
    @SerializedName("ruleSummary") val ruleSummary: String = "",

    // --- 正文抓取规则 (通用，但主要用于 RSS 只有摘要时) ---
    // CSS 选择器，例如: div.article-content
    @SerializedName("ruleContent") val ruleContent: String = "",
    // 是否启用自动提取算法 (优先于 ruleContent)
    @SerializedName("useAutoExtract") val useAutoExtract: Boolean = false,
    // 正文提取算法：readability=Mozilla算法, gne=基于文本密度, custom=自定义选择器
    @SerializedName("extractionAlgorithm") val extractionAlgorithm: String = "readability"
) : Serializable