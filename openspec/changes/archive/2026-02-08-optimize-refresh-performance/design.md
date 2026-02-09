## Context

当前应用使用串行刷新机制（`for` 循环逐个刷新订阅源），导致在多订阅源场景下总耗时较长。虽然已实现增量进度反馈（每个源刷新完立即显示Toast），但用户反馈：
1. 刷新总时间过长（10个源 × 2秒 = 20秒）
2. 每个源的Toast提示过于频繁和干扰
3. 设置页面默认筛选选项占用空间过大（3行垂直布局）

**当前架构**:
- `NewsRepository.syncAll()` 使用串行 `for` 循环
- `TimelineViewModel` 发送 `UiEvent.SourceRefreshed` 事件
- `TimelineScreen` 处理单源和总计Toast
- `SettingsScreen` 使用垂直 `RadioButton` 布局

**技术栈**: Kotlin Coroutines + Flow + Room + Compose Material 3

## Goals / Non-Goals

**Goals:**
- 并发刷新订阅源，减少总刷新时间（3并发时约 1/3 时间）
- 智能自动滚动：有新文章时滚动到顶部，无新文章时不滚动
- 简化刷新提示：仅显示总计汇总，移除单源Toast
- 精简默认筛选UI：使用紧凑布局（1-2行）
- 支持用户配置并发数（1-5，默认3）

**Non-Goals:**
- 不改变数据库schema或Room配置
- 不引入新的依赖库（使用Kotlin标准库和现有组件）
- 不修改现有错误处理机制（保持Result包装）
- 不实现并行进度回调的排序（按完成顺序触发即可）

## Decisions

### 1. 使用 Semaphore 限制并发数

**决策**: 使用 `Semaphore(permits)` 限制并发协程数量，而非固定大小的线程池

**理由**:
- Semaphore 轻量级，适用于协程
- 易于动态调整（用户修改设置后立即生效）
- 自动管理等待队列，无需手动调度
- 符合 Kotlin Coroutines 最佳实践

**权衡**:
- 优点: 简单高效，资源占用少，易于测试
- 缺点: Semaphore 不保证线程亲和性（但对网络IO任务无影响）

**实现位置**: `NewsRepository.kt:syncAll()`
```kotlin
suspend fun syncAll(
    concurrentLimit: Int = settingsManager.getConcurrentCount(),
    onProgress: (RefreshProgress) -> Unit
): Result<RefreshSummary> {
    val semaphore = Semaphore(concurrentLimit)
    val sources = sourceDao.getAllSources().first()

    sources.map { source ->
        async {
            semaphore.acquire()
            try {
                fetchAndSave(source)
            } finally {
                semaphore.release()
            }
        }
    }.awaitAll()
}
```

### 2. 智能滚动使用 Channel 通知机制

**决策**: 在 `TimelineViewModel` 中使用 `Channel<ScrollEvent>` 通知滚动，避免直接操作UI组件

**理由**:
- 保持 MVVM 架构，ViewModel 不直接持有 UI 引用
- Channel 天然支持背压（多个源快速完成时自动合并）
- 易于测试（可验证Channel发送逻辑）
- 符合现有架构模式（类似 `Channel<UiEvent>`）

**权衡**:
- 优点: 架构清晰，可测试，支持防抖
- 缺点: 需要额外数据类定义（但这是合理的抽象）

**实现位置**: `TimelineViewModel.kt`
```kotlin
sealed class ScrollEvent {
    object ScrollToTop : ScrollEvent()
    object NoOp : ScrollEvent()
}

private val scrollChannel = Channel<ScrollEvent>(Channel.CONFLATED)

// 在 TimelineScreen 中监听
LaunchedEffect(Unit) {
    viewModel.scrollChannel.receiveAsFlow().collect { event ->
        if (event is ScrollEvent.ScrollToTop) {
            listState.animateScrollToItem(0)
        }
    }
}
```

### 3. 移除 UiEvent.SourceRefreshed，保留 RefreshCompleted

**决策**: 完全移除 `SourceRefreshed` 事件，不再发送单源进度通知

**理由**:
- 用户反馈单源Toast过于频繁和干扰
- 简化事件流，减少不必要的UI更新
- 汇总信息已包含所有必要数据（总数+失败数）

**权衡**:
- 优点: 减少Toast数量（从N+1减少到1），用户体验更流畅
- 缺点: 失去单个源的详细进度（但汇总信息已足够）

**实现位置**:
- `TimelineViewModel.kt:refresh()` - 移除 `_event.send(SourceRefreshed(...))`
- `TimelineScreen.kt` - 移除 `is UiEvent.SourceRefreshed` 分支
- `ui/common/UiEvent.kt` - 删除 `SourceRefreshed` 数据类

### 4. 使用 Material 3 SegmentedButton 替代 RadioButton

**决策**: 使用 `SegmentedButton` 组件实现紧凑的单行筛选选择器

**理由**:
- Material 3 推荐组件，专为选项选择设计
- 单行布局，占用空间小（约48dp高度）
- 自带选中状态动画和视觉反馈
- 符合 Material Design 规范

**权衡**:
- 优点: 空间效率高（从3行减少到1行），视觉美观，开箱即用
- 缺点: 选项标签需要简短（但"全部/未读/已读"已经很短）

**实现位置**: `SettingsScreen.kt`
```kotlin
@Composable
fun CompactFilterSelector(
    currentFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterType.values().forEach { type ->
            FilterChip(
                selected = currentFilter == type,
                onClick = { onFilterSelected(type) },
                label = { Text(type.displayName) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

**注**: 使用 `FilterChip` 而非 `SegmentmentedButton`，因为 FilterChip 更适合可点击的选项按钮，且支持多选布局。

### 5. 滚动防抖使用时间窗口机制

**决策**: 使用 500ms 时间窗口防抖，窗口内多次滚动请求合并为一次

**理由**:
- 并发刷新时多个源可能几乎同时完成（如 3个源在100ms内完成）
- 避免快速连续滚动导致用户眩晕
- 500ms 是人眼感知的合理阈值

**权衡**:
- 优点: 用户体验流畅，避免抖动
- 缺点: 可能延迟滚动（但500ms延迟可接受）

**实现位置**: `TimelineViewModel.kt`
```kotlin
private var lastScrollTime = 0L
private val scrollDebounceMs = 500L

fun notifyNewArticles(sourceName: String, count: Int) {
    val now = System.currentTimeMillis()
    if (count > 0 && now - lastScrollTime > scrollDebounceMs) {
        viewModelScope.launch {
            scrollChannel.send(ScrollEvent.ScrollToTop)
            lastScrollTime = now
        }
    }
}
```

## Risks / Trade-offs

### Risk 1: 并发刷新可能导致资源耗尽

**风险**: 用户设置并发数为5时，5个网络请求同时进行可能耗尽内存或连接池

**缓解措施**:
- 限制最大并发数为5（保守值，大多数设备可承受）
- OkHttp 默认连接池最大5个并发，复用现有配置
- 添加监控：如果连续3次OOM异常，自动降级为并发数1
- 在设置中提示用户："高并发数可能增加内存和电量消耗"

### Risk 2: 滚动时机可能影响用户阅读

**风险**: 用户正在阅读某篇文章，突然滚动到顶部会造成干扰

**缓解措施**:
- 仅在用户主动点击刷新按钮时触发滚动
- 后台自动刷新不触发滚动
- 提供设置选项："刷新时自动滚动到顶部"（默认开启）
- 滚动动画使用缓动曲线（300ms ease-out），避免突兀

### Risk 3: 简化Toast可能让用户感觉缺少反馈

**风险**: 移除单源Toast后，用户可能不清楚刷新是否在进行

**缓解措施**:
- 保留现有的进度指示器（LinearProgressIndicator）
- 在刷新过程中显示 "正在刷新 X/Y..." 文本提示
- 汇总Toast包含详细信息（总数+失败数）
- 如用户需要详细进度，可在设置中开启"显示每个源的进度"（未来扩展）

### Risk 4: FilterChip 在小屏幕上可能显示不全

**风险**: 3个选项在小屏幕设备上可能拥挤或换行

**缓解措施**:
- 使用 `Modifier.weight(1f)` 确保等宽分布
- 设置 `minHeight = 40dp` 保证触摸目标大小
- 文字使用 `style = MaterialTheme.typography.bodySmall`
- 如屏幕极小（如320dp宽），自动切换为 ExposedDropdownMenu

## Migration Plan

### 部署步骤

**阶段1: 添加并发刷新基础设施** (无破坏性)
1. 在 `SettingsManager` 添加 `getConcurrentCount()` / `setConcurrentCount()` 方法
2. 在 `SettingsScreen` 添加"刷新并发数"设置项（Slider: 1-5）
3. 添加默认值：SharedPreferences "refresh_concurrent_count" = 3

**阶段2: 实现并发刷新** (保持向后兼容)
4. 修改 `NewsRepository.syncAll()` 为并发执行（使用Semaphore）
5. 保留进度回调接口，回调时机保持不变（每个源完成时）
6. 单元测试：验证并发数限制和进度回调正确性

**阶段3: 实现智能滚动** (新增功能)
7. 在 `TimelineViewModel` 添加 `ScrollEvent` Channel
8. 在 `fetchAndSave()` 返回新文章数量，传递给ViewModel
9. 在 `TimelineViewModel.refresh()` 中判断 `newCount > 0` 时发送滚动事件
10. 在 `TimelineScreen` 监听滚动事件，调用 `animateScrollToItem(0)`
11. 添加防抖逻辑（500ms时间窗口）

**阶段4: 简化刷新提示** (移除现有功能)
12. 在 `TimelineViewModel.refresh()` 移除所有 `SourceRefreshed` 事件发送
13. 保留 `RefreshCompleted` 事件，确保消息格式正确
14. 在 `TimelineScreen` 移除 `SourceRefreshed` 事件处理分支
15. 删除 `ui/common/UiEvent.kt` 中的 `SourceRefreshed` 数据类

**阶段5: 精简筛选UI** (UI重构)
16. 在 `SettingsScreen` 将 `Column` 垂直布局改为 `Row` 水平布局
17. 使用 `FilterChip` 替代 `RadioButton`
18. 每个选项使用 `Modifier.weight(1f)` 实现等宽分布
19. 移除说明文字，只保留标签文字
20. Toast持续时间从 2000ms 改为 1500ms

### 回滚策略

**分阶段回滚**:
- 阶段1-2: 如并发刷新导致崩溃，可回退到串行 `for` 循环
- 阶段3: 如滚动导致用户体验差，可禁用滚动事件发送
- 阶段4: 如用户反馈需要详细进度，可恢复 `SourceRefreshed` 事件
- 阶段5: 如 FilterChip 显示问题，可恢复 `RadioButton` 布局

**数据兼容性**:
- 新增 SharedPreferences 有默认值，老用户自动升级
- 无需数据迁移或版本变更

### 测试计划

**单元测试**:
- `NewsRepositoryTest`: 验证并发刷新正确性，模拟3个worker并发
- `SettingsManagerTest`: 验证并发数读写、默认值、边界值
- `TimelineViewModelTest`: 验证滚动触发逻辑、防抖机制

**集成测试**:
- 5个订阅源，并发数=3，验证总耗时约为串行的1/3
- 10个订阅源，并发数=5，验证不超过5个并发worker
- 测试滚动防抖：3个源快速完成（<500ms），只滚动1次

**手动测试**:
- 小屏幕设备（320dp宽）：验证 FilterChip 不换行
- 大屏幕设备（600dp宽）：验证 FilterChip 等宽分布
- 网络慢速环境：验证并发刷新的优势
- 边界情况：并发数=1（串行行为），并发数=5（最大并发）

## Open Questions

1. **Q**: 是否需要在进度指示器中显示当前并发状态？
   - **A**: 当前不显示。如用户需要，可在设置中添加"显示详细进度"选项。现有进度文本"正在刷新..."已足够。

2. **Q**: 滚动时机是否应该在文章插入前还是插入后？
   - **A**: 在文章插入后滚动。因为需要等待 Room 数据库更新完成，Flow 才能触发UI刷新。

3. **Q**: 如果用户在滚动过程中又手动滚动，如何处理？
   - **A**: 自动滚动优先级较低，如果用户正在滚动（`listState.isScrollInProgress`），则跳过自动滚动。

4. **Q**: FilterChip vs ExposedDropdownMenu 如何选择？
   - **A**: 优先使用 FilterChip（3个选项单行显示）。如果屏幕宽度 < 360dp，则使用 ExposedDropdownMenu。

5. **Q**: 并发刷新是否需要队列优先级？
   - **A**: 当前不实现优先级队列。所有订阅源按现有顺序并发，先到先得。未来可考虑"置顶源优先刷新"。

6. **Q**: 智能滚动是否应该在所有筛选条件下生效？
   - **A**: 只在"未读"筛选下生效。当用户筛选"已读"或"全部"时，说明用户在查看历史文章，不应打断。
