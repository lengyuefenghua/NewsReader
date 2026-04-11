package com.lengyuefenghua.newsreader.data

/**
 * 两阶段刷新汇总
 */
data class TwoPhaseRefreshSummary(
    val fastNewArticles: Int,
    val totalNewArticles: Int,
    val failedSources: Int,
    val fastSources: Int,
    val totalSources: Int
)

/**
 * Fast phase 完成时的阶段汇总
 */
data class FastPhaseRefreshSummary(
    val fastNewArticles: Int,
    val fastSources: Int,
    val failedSources: Int,
    val totalSources: Int
)

enum class TwoPhaseRefreshEventType {
    FAST_PROGRESS,
    FAST_DONE,
    FULL_PROGRESS,
    FULL_DONE
}

data class TwoPhaseRefreshEvent(
    val type: TwoPhaseRefreshEventType,
    val sourceName: String? = null,
    val success: Boolean? = null,
    val newArticleCount: Int = 0,
    val current: Int = 0,
    val total: Int = 0,
    val fastPhaseSummary: FastPhaseRefreshSummary? = null,
    val summary: TwoPhaseRefreshSummary? = null
)

internal suspend fun runTwoPhaseRefresh(
    sources: List<Source>,
    fastBatchSize: Int,
    fetcher: suspend (Source) -> Int,
    onEvent: (TwoPhaseRefreshEvent) -> Unit
): TwoPhaseRefreshSummary {
    if (sources.isEmpty()) {
        val emptySummary = TwoPhaseRefreshSummary(
            fastNewArticles = 0,
            totalNewArticles = 0,
            failedSources = 0,
            fastSources = 0,
            totalSources = 0
        )
        onEvent(TwoPhaseRefreshEvent(type = TwoPhaseRefreshEventType.FULL_DONE, summary = emptySummary))
        return emptySummary
    }

    val plan = RefreshPlanner.split(sources, fastBatchSize)
    val fastBatch = plan.fastBatch
    val remainingBatch = plan.remainingBatch

    val totalSources = sources.size
    var completedCount = 0
    var failedSources = 0
    var fastNewArticles = 0
    var totalNewArticles = 0

    fastBatch.forEach { source ->
        try {
            val newCount = fetcher(source)
            fastNewArticles += newCount
            totalNewArticles += newCount
            completedCount += 1
            onEvent(
                TwoPhaseRefreshEvent(
                    type = TwoPhaseRefreshEventType.FAST_PROGRESS,
                    sourceName = source.name,
                    success = true,
                    newArticleCount = newCount,
                    current = completedCount,
                    total = totalSources
                )
            )
        } catch (_: Exception) {
            failedSources += 1
            completedCount += 1
            onEvent(
                TwoPhaseRefreshEvent(
                    type = TwoPhaseRefreshEventType.FAST_PROGRESS,
                    sourceName = source.name,
                    success = false,
                    newArticleCount = 0,
                    current = completedCount,
                    total = totalSources
                )
            )
        }
    }

    val fastPhaseSummary = FastPhaseRefreshSummary(
        fastNewArticles = fastNewArticles,
        fastSources = fastBatch.size,
        failedSources = failedSources,
        totalSources = totalSources
    )
    onEvent(
        TwoPhaseRefreshEvent(
            type = TwoPhaseRefreshEventType.FAST_DONE,
            fastPhaseSummary = fastPhaseSummary
        )
    )

    remainingBatch.forEach { source ->
        try {
            val newCount = fetcher(source)
            totalNewArticles += newCount
            completedCount += 1
            onEvent(
                TwoPhaseRefreshEvent(
                    type = TwoPhaseRefreshEventType.FULL_PROGRESS,
                    sourceName = source.name,
                    success = true,
                    newArticleCount = newCount,
                    current = completedCount,
                    total = totalSources
                )
            )
        } catch (_: Exception) {
            failedSources += 1
            completedCount += 1
            onEvent(
                TwoPhaseRefreshEvent(
                    type = TwoPhaseRefreshEventType.FULL_PROGRESS,
                    sourceName = source.name,
                    success = false,
                    newArticleCount = 0,
                    current = completedCount,
                    total = totalSources
                )
            )
        }
    }

    val summary = TwoPhaseRefreshSummary(
        fastNewArticles = fastNewArticles,
        totalNewArticles = totalNewArticles,
        failedSources = failedSources,
        fastSources = fastBatch.size,
        totalSources = totalSources
    )

    onEvent(TwoPhaseRefreshEvent(type = TwoPhaseRefreshEventType.FULL_DONE, summary = summary))

    return summary
}
