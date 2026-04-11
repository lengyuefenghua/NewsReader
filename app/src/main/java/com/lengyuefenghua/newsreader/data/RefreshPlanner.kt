package com.lengyuefenghua.newsreader.data

object RefreshPlanner {
    data class Plan<T>(
        val fastBatch: List<T>,
        val remainingBatch: List<T>
    )

    fun <T> split(sources: List<T>, fastBatchSize: Int): Plan<T> {
        val clampedSize = fastBatchSize.coerceAtLeast(1).coerceAtMost(sources.size)
        return Plan(
            fastBatch = sources.take(clampedSize),
            remainingBatch = sources.drop(clampedSize)
        )
    }
}
