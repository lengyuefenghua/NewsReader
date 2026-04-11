package com.lengyuefenghua.newsreader.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshPlannerTest {

    @Test
    fun `fast lane should prioritize first batch and split remaining`() {
        val sources = listOf("s1", "s2", "s3", "s4", "s5")

        val plan = RefreshPlanner.split(sources, fastBatchSize = 2)

        assertEquals(listOf("s1", "s2"), plan.fastBatch)
        assertEquals(listOf("s3", "s4", "s5"), plan.remainingBatch)
    }

    @Test
    fun `fast lane size should be clamped to valid range`() {
        val sources = listOf("s1", "s2", "s3")

        val planMin = RefreshPlanner.split(sources, fastBatchSize = 0)
        assertEquals(listOf("s1"), planMin.fastBatch)
        assertEquals(listOf("s2", "s3"), planMin.remainingBatch)

        val planMax = RefreshPlanner.split(sources, fastBatchSize = 99)
        assertEquals(listOf("s1", "s2", "s3"), planMax.fastBatch)
        assertEquals(emptyList<String>(), planMax.remainingBatch)
    }

    @Test
    fun `split should keep stable order for deterministic execution`() {
        val sources = listOf("s3", "s1", "s4", "s2")

        val plan = RefreshPlanner.split(sources, fastBatchSize = 2)

        assertEquals(listOf("s3", "s1"), plan.fastBatch)
        assertEquals(listOf("s4", "s2"), plan.remainingBatch)
        assertEquals(sources, plan.fastBatch + plan.remainingBatch)
    }

    @Test
    fun `fast lane split should handle empty source list`() {
        val sources = emptyList<String>()

        val plan = RefreshPlanner.split(sources, fastBatchSize = 3)

        assertEquals(emptyList<String>(), plan.fastBatch)
        assertEquals(emptyList<String>(), plan.remainingBatch)
    }
}
