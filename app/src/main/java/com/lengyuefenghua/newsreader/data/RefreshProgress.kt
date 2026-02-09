package com.lengyuefenghua.newsreader.data

/**
 * 刷新进度数据
 *
 * @param sourceName 订阅源名称
 * @param success 是否成功
 * @param newArticleCount 新增文章数量
 * @param current 当前进度（已完成数）
 * @param total 总数
 */
data class RefreshProgress(
    val sourceName: String,
    val success: Boolean,
    val newArticleCount: Int,
    val current: Int,
    val total: Int
)

/**
 * 刷新汇总数据
 *
 * @param totalNewArticles 总新增文章数
 * @param totalSources 总订阅源数
 * @param failedSources 失败的订阅源数
 */
data class RefreshSummary(
    val totalNewArticles: Int,
    val totalSources: Int,
    val failedSources: Int
)
