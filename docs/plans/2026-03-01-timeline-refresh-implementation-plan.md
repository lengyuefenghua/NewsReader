# Timeline 刷新优化（先快后全）Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将时间线刷新升级为“先快后全”两阶段流程，使用户更快看到首批新增文章，同时保持全量刷新能力与稳定性。

**Architecture:** 在 Repository 层新增两阶段调度入口，先执行 Fast Lane（优先源）再执行 Full Lane（剩余源）；在 ViewModel 层新增首批就绪事件与阶段状态；在 Timeline UI 层新增阶段化反馈，保留现有最终汇总事件。全程不改数据库 schema、不改 DAO 表结构。

**Tech Stack:** Kotlin, Coroutines/Flow, Room, Jetpack Compose, Koin, OkHttp

---

### Task 1: 建立行为基线（测试先行）

**Files:**
- Modify: `app/src/test/java/com/lengyuefenghua/newsreader/ExampleUnitTest.kt`
- Create: `app/src/test/java/com/lengyuefenghua/newsreader/viewmodel/TimelineRefreshPlanTest.kt`
- Create: `app/src/test/java/com/lengyuefenghua/newsreader/data/RefreshPlanStrategyTest.kt`

**Step 1: Write the failing test**

```kotlin
// RefreshPlanStrategyTest.kt
@Test
fun `fast lane should prioritize first batch and split remaining`() {
    val sources = listOf("A", "B", "C", "D", "E")
    val plan = RefreshPlanner.split(sources, fastBatchSize = 2)

    assertEquals(listOf("A", "B"), plan.fastBatch)
    assertEquals(listOf("C", "D", "E"), plan.remainingBatch)
}

@Test
fun `fast lane size should be clamped to valid range`() {
    val sources = listOf("A", "B")
    val plan = RefreshPlanner.split(sources, fastBatchSize = 10)

    assertEquals(2, plan.fastBatch.size)
    assertTrue(plan.remainingBatch.isEmpty())
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.data.RefreshPlanStrategyTest"`
Expected: FAIL with "Unresolved reference: RefreshPlanner"

**Step 3: Write minimal implementation**

```kotlin
// minimal skeleton only
object RefreshPlanner {
    data class Plan<T>(val fastBatch: List<T>, val remainingBatch: List<T>)

    fun <T> split(sources: List<T>, fastBatchSize: Int): Plan<T> {
        val size = fastBatchSize.coerceAtLeast(1).coerceAtMost(sources.size)
        return Plan(
            fastBatch = sources.take(size),
            remainingBatch = sources.drop(size)
        )
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.data.RefreshPlanStrategyTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add app/src/test/java/com/lengyuefenghua/newsreader/data/RefreshPlanStrategyTest.kt app/src/main/java/com/lengyuefenghua/newsreader/data/RefreshPlanner.kt
git commit -m "test: add refresh planner baseline tests"
```

---

### Task 2: 提取可测试的两阶段规划器

**Files:**
- Create: `app/src/main/java/com/lengyuefenghua/newsreader/data/RefreshPlanner.kt`
- Test: `app/src/test/java/com/lengyuefenghua/newsreader/data/RefreshPlanStrategyTest.kt`

**Step 1: Write the failing test**

```kotlin
@Test
fun `split should keep stable order for deterministic execution`() {
    val sources = listOf("S1", "S2", "S3")
    val plan = RefreshPlanner.split(sources, fastBatchSize = 2)

    assertEquals(listOf("S1", "S2"), plan.fastBatch)
    assertEquals(listOf("S3"), plan.remainingBatch)
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.data.RefreshPlanStrategyTest.split should keep stable order for deterministic execution"`
Expected: FAIL if order or clamping behavior is missing

**Step 3: Write minimal implementation**

- Implement deterministic split with clamp logic.
- Keep YAGNI: no historical score model in V1.

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.data.RefreshPlanStrategyTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/data/RefreshPlanner.kt app/src/test/java/com/lengyuefenghua/newsreader/data/RefreshPlanStrategyTest.kt
git commit -m "feat: add deterministic refresh planner for fast and remaining batches"
```

---

### Task 3: Repository 增加两阶段刷新入口

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`
- Create: `app/src/main/java/com/lengyuefenghua/newsreader/data/TwoPhaseRefreshModels.kt`
- Test: `app/src/test/java/com/lengyuefenghua/newsreader/data/TwoPhaseRefreshRepositoryTest.kt`

**Step 1: Write the failing test**

```kotlin
@Test
fun `syncAllTwoPhase should report fast phase before full phase`() = runTest {
    // Given fake source list >= 3
    // When syncAllTwoPhase called with fastBatchSize=2
    // Then callbacks contain phase order: FAST_PROGRESS -> FAST_DONE -> FULL_PROGRESS -> FULL_DONE
    fail("not implemented")
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.data.TwoPhaseRefreshRepositoryTest"`
Expected: FAIL with missing API `syncAllTwoPhase`

**Step 3: Write minimal implementation**

- Add `syncAllTwoPhase(...)` beside existing `syncAll(...)`.
- Reuse existing `fetchAndSave` and error handling style.
- Return structured result:
  - `fastNewArticles`
  - `totalNewArticles`
  - `failedSources`
  - `fastSources`
  - `totalSources`

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.data.TwoPhaseRefreshRepositoryTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt app/src/main/java/com/lengyuefenghua/newsreader/data/TwoPhaseRefreshModels.kt app/src/test/java/com/lengyuefenghua/newsreader/data/TwoPhaseRefreshRepositoryTest.kt
git commit -m "feat: add two-phase repository refresh pipeline"
```

---

### Task 4: 新增 ViewModel 阶段化状态与首批事件

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/common/UiEvent.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/common/TimelineUiState.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/TimelineViewModel.kt`
- Test: `app/src/test/java/com/lengyuefenghua/newsreader/viewmodel/TimelineRefreshPlanTest.kt`

**Step 1: Write the failing test**

```kotlin
@Test
fun `refresh should emit fast ready event before refresh completed`() = runTest {
    // Arrange fake repository two-phase result
    // Act refresh()
    // Assert first event is FastBatchReady and later RefreshCompleted
    fail("not implemented")
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.viewmodel.TimelineRefreshPlanTest"`
Expected: FAIL with missing event/state

**Step 3: Write minimal implementation**

- Add `UiEvent.FastBatchReady(newArticles, completedSources, totalFastSources)`.
- In `TimelineViewModel.refresh()`:
  1. call repository two-phase API
  2. emit fast-ready event once fast phase completes
  3. keep existing `RefreshCompleted` for final summary
- Keep existing single-source refresh branch unchanged (YAGNI)

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.viewmodel.TimelineRefreshPlanTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/ui/common/UiEvent.kt app/src/main/java/com/lengyuefenghua/newsreader/ui/common/TimelineUiState.kt app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/TimelineViewModel.kt app/src/test/java/com/lengyuefenghua/newsreader/viewmodel/TimelineRefreshPlanTest.kt
git commit -m "feat: emit fast batch event from timeline refresh"
```

---

### Task 5: Timeline UI 增加首批就绪反馈

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/TimelineScreen.kt`
- Test: `app/src/androidTest/java/com/lengyuefenghua/newsreader/TimelineTwoPhaseUiTest.kt`

**Step 1: Write the failing test**

```kotlin
@Test
fun fast_batch_message_is_shown_before_final_summary() {
    // Launch timeline
    // Trigger refresh with fake VM/events
    // Verify fast-batch indicator/message appears before final completion message
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lengyuefenghua.newsreader.TimelineTwoPhaseUiTest`
Expected: FAIL with missing fast-batch UI handling

**Step 3: Write minimal implementation**

- In `TimelineScreen` event collector:
  - handle `UiEvent.FastBatchReady`
  - show concise feedback: “首批已更新 X 篇，正在继续补全...”
- Keep existing `RefreshCompleted` behavior intact
- Avoid introducing new UI components unless needed; reuse existing Toast/progress region

**Step 4: Run test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lengyuefenghua.newsreader.TimelineTwoPhaseUiTest`
Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/TimelineScreen.kt app/src/androidTest/java/com/lengyuefenghua/newsreader/TimelineTwoPhaseUiTest.kt
git commit -m "feat: show fast-phase feedback in timeline screen"
```

---

### Task 6: 指标埋点与验收脚本

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/TimelineViewModel.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/utils/LogUtils.kt`
- Create: `docs/plans/2026-03-01-timeline-refresh-metrics-checklist.md`

**Step 1: Write the failing test**

```kotlin
@Test
fun `refresh should log ttff and total duration markers`() = runTest {
    // Assert log marker calls (or wrapper method invocation)
    fail("not implemented")
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.viewmodel.TimelineRefreshPlanTest.refresh should log ttff and total duration markers"`
Expected: FAIL without metric markers

**Step 3: Write minimal implementation**

- Add timestamp capture in ViewModel refresh lifecycle:
  - refreshStart
  - fastReadyAt
  - fullDoneAt
- Log markers with consistent tags for manual profiling
- Add metrics checklist doc for manual acceptance (TTFF, hit rate, full duration delta)

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.viewmodel.TimelineRefreshPlanTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/TimelineViewModel.kt app/src/main/java/com/lengyuefenghua/newsreader/utils/LogUtils.kt docs/plans/2026-03-01-timeline-refresh-metrics-checklist.md
git commit -m "chore: add timeline refresh metrics markers and checklist"
```

---

### Task 7: 端到端验证与回归

**Files:**
- Modify: `docs/plans/2026-03-01-timeline-refresh-metrics-checklist.md`
- (No production code changes expected)

**Step 1: Write the failing test**

- N/A（本任务为验证执行任务）

**Step 2: Run test to verify it fails**

- N/A

**Step 3: Write minimal implementation**

- N/A

**Step 4: Run test to verify it passes**

Run (unit + instrumentation):

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew installDebug
adb shell am start -n com.lengyuefenghua.newsreader/.MainActivity
```

Expected:
- All tests PASS
- App launches successfully on emulator
- Manual checklist confirms:
  - TTFF improved vs baseline
  - fast-ready feedback appears before final summary
  - full refresh duration not significantly regressed

**Step 5: Commit**

```bash
git add docs/plans/2026-03-01-timeline-refresh-metrics-checklist.md
git commit -m "test: record two-phase refresh verification results"
```

---

## Implementation Notes

- DRY: 复用 `fetchAndSave` 与现有错误映射，不重复实现网络解析逻辑。
- YAGNI: 首版不引入历史评分模型，不新增数据库字段。
- TDD: 每个任务先写失败测试，再最小实现，再验证。
- Frequent commits: 每个任务结束即提交，便于回滚和 code review。

## Risks and Guards

1. **总耗时回退**：通过阶段并发策略和验收阈值（<= +10%）控制。
2. **UI 事件噪声**：首批事件仅发一次，避免反复提示。
3. **行为不兼容**：单源刷新分支不改动；保留原 `RefreshCompleted` 语义。

## Test Plan Summary

- Unit:
  - planner split correctness
  - repository phase ordering
  - viewmodel event ordering and metrics markers
- Instrumentation:
  - timeline fast-phase feedback sequence
- Manual:
  - install/launch and real-device/emulator refresh observation

---

Plan complete and saved to `docs/plans/2026-03-01-timeline-refresh-implementation-plan.md`. Two execution options:

1. Subagent-Driven (this session) - I dispatch fresh subagent per task, review between tasks, fast iteration

2. Parallel Session (separate) - Open new session with executing-plans, batch execution with checkpoints

Which approach?
