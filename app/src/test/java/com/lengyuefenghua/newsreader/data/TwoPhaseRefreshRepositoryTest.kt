package com.lengyuefenghua.newsreader.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TwoPhaseRefreshRepositoryTest {

    @Test
    fun `syncAllTwoPhase should report fast phase before full phase`() = runBlocking {
        val sources = listOf(
            Source(name = "fast-1", url = "https://example.com/1"),
            Source(name = "fast-2", url = "https://example.com/2"),
            Source(name = "full-1", url = "https://example.com/3")
        )

        val events = mutableListOf<TwoPhaseRefreshEventType>()

        val summary = runTwoPhaseRefresh(
            sources = sources,
            fastBatchSize = 2,
            fetcher = { source ->
                when (source.name) {
                    "fast-1" -> 1
                    "fast-2" -> 2
                    "full-1" -> 3
                    else -> 0
                }
            },
            onEvent = { event ->
                events += event.type
            }
        )

        assertContainsInOrder(
            actual = events,
            expected = listOf(
                TwoPhaseRefreshEventType.FAST_PROGRESS,
                TwoPhaseRefreshEventType.FAST_DONE,
                TwoPhaseRefreshEventType.FULL_PROGRESS,
                TwoPhaseRefreshEventType.FULL_DONE
            )
        )

        assertEquals(3, summary.fastNewArticles)
        assertEquals(6, summary.totalNewArticles)
        assertEquals(0, summary.failedSources)
        assertEquals(2, summary.fastSources)
        assertEquals(3, summary.totalSources)
    }

    private fun assertContainsInOrder(
        actual: List<TwoPhaseRefreshEventType>,
        expected: List<TwoPhaseRefreshEventType>
    ) {
        var startIndex = 0
        for (target in expected) {
            val foundIndex = actual.indexOfFirstIndexed(startIndex) { it == target }
            assertTrue("Expected event $target after index $startIndex, but got: $actual", foundIndex >= 0)
            startIndex = foundIndex + 1
        }
    }

    private inline fun <T> List<T>.indexOfFirstIndexed(startIndex: Int, predicate: (T) -> Boolean): Int {
        for (index in startIndex until size) {
            if (predicate(this[index])) {
                return index
            }
        }
        return -1
    }
}
