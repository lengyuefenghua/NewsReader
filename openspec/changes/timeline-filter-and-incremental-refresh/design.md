## Context

当前时间线(Timeline)功能使用 `FilterType.ALL` 作为默认筛选器,用户首次打开应用需要手动切换到"未读"才能看到新内容。刷新机制采用并行批量策略(使用 `async/awaitAll`),所有订阅源全部刷新完成后才统一更新 UI,导致用户等待时间较长且缺乏进度反馈。

**当前架构**:
- **UI 层**: `TimelineViewModel` 管理状态,使用 StateFlow + Channel 模式
- **数据层**: `NewsRepository` 使用 `async/awaitAll` 并行刷新所有订阅源
- **状态管理**: `TimelineUiState` 包含 `filterType: FilterType = FilterType.ALL`
- **刷新反馈**: 仅显示"全部更新完成"的简单 Toast,无详细进度

**技术栈**: Kotlin Coroutines + Flow + Room, MVVM 架构

## Goals / Non-Goals

**Goals:**
- 时间线默认显示未读文章,提升新内容可见性
- 支持用户在设置中自定义默认筛选条件(全部/未读/已读)
- 使用 SharedPreferences 持久化用户的默认筛选偏好
- 刷新过程中实时显示每个订阅源的更新进度和新增文章数
- 采用增量刷新策略,每个订阅源刷新完立即更新 UI,减少用户等待时间
- 提供清晰的刷新完成统计(总新文章数、失败源数量)

**Non-Goals:**
- 不修改数据库 schema 或迁移策略
- 不改变现有的错误处理机制(保持 Result 包装)
- 不引入新的依赖库(仅使用现有 Kotlin 标准库和 Compose API)
- 不支持"记住上次的筛选状态"(仅支持用户在设置中配置默认值)

## Decisions

### 1. 默认筛选状态改为未读(支持用户自定义)

**决策**: 将 `TimelineUiState` 的 `filterType` 默认值改为从 SharedPreferences 读取的设置,工厂默认为 `FilterType.UNREAD`

**理由**:
- 用户主要关注未读内容,减少操作步骤
- 符合 RSS 阅读器的常见交互模式
- 提供可配置性,满足不同用户需求
- 用户偏好持久化,跨会话保持一致

**权衡**:
- 优点: 提升新内容发现效率,减少 1 次点击操作;支持高级用户自定义
- 缺点: 需要新增 SettingsManager 工具类和设置 UI,增加约 150 行代码

**实现位置**:
- `util/SettingsManager.kt` - 新增工具类,管理 SharedPreferences
- `TimelineViewModel.kt:30` - 从 SettingsManager 读取默认值
- `ui/screens/ProfileScreen.kt` - 添加设置 UI

**核心实现**:
```kotlin
// SettingsManager.kt
class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun getDefaultFilterType(): FilterType {
        val value = prefs.getString("default_filter_type", null)
        return when (value) {
            "ALL" -> FilterType.ALL
            "READ" -> FilterType.READ
            else -> FilterType.UNREAD  // 工厂默认
        }
    }

    fun setDefaultFilterType(type: FilterType) {
        prefs.edit().putString("default_filter_type", type.name).commit()
    }
}

// TimelineViewModel.kt
class TimelineViewModel(
    private val repository: NewsRepository,
    private val settingsManager: SettingsManager  // 依赖注入
) : ViewModel() {
    private val _filterState = MutableStateFlow(settingsManager.getDefaultFilterType())
    val filterState = _filterState.asStateFlow()
    // ...
}
```

### 2. 设置 UI 使用单选按钮组

**决策**: 在设置页面(ProfileScreen)添加"默认筛选条件"选项,使用 Radio Button 组件实现三选一

**理由**:
- Radio Button 是单选的标准 UI 模式,用户熟悉
- Material 3 提供原生 RadioButton 组件,无需自定义
- 实现简单,代码量少(约 50 行)

**权衡**:
- 优点: UI 清晰直观,符合 Material Design 规范
- 缺点: 无(这是最佳实践)

**实现位置**: `ui/screens/ProfileScreen.kt`

**核心实现**:
```kotlin
// 在 ProfileScreen 中添加新的设置项
@Composable
fun DefaultFilterSetting(
    currentFilter: FilterType,
    onFilterChanged: (FilterType) -> Unit
) {
    Column {
        Text("默认筛选条件", style = MaterialTheme.typography.titleMedium)
        FilterType.values().forEach { type ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onFilterChanged(type) }
                    .padding(16.dp)
            ) {
                RadioButton(
                    selected = currentFilter == type,
                    onClick = { onFilterChanged(type) }
                )
                Text(
                    text = type.displayName,  // "全部", "未读", "已读"
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
```

### 3. 从并行刷新改为串行增量刷新

**决策**: 将 `NewsRepository.syncAll()` 的并行策略(`async/awaitAll`)改为串行执行,并在每个订阅源刷新完成后立即触发回调

**理由**:
- 并行刷新导致 UI 无法及时更新(需等待所有源完成)
- 串行执行可以精确控制每个源的刷新时机
- 串行更易实现增量进度反馈和错误处理

**权衡**:
- 优点: 增量更新 UI,用户体验更好;错误处理更清晰
- 缺点: 总刷新时间可能略长(并行改为串行),但用户感知的等待时间更短(可立即看到内容)

**实现位置**: `NewsRepository.kt:52-94`
```kotlin
// 移除 async/awaitAll,改为串行 for 循环
suspend fun syncAll(onProgress: (RefreshProgress) -> Unit): Result<RefreshSummary>
```

**新增数据类**:
```kotlin
data class RefreshProgress(
    val sourceName: String,
    val success: Boolean,
    val newArticleCount: Int
)

data class RefreshSummary(
    val totalNewArticles: Int,
    val totalSources: Int,
    val failedSources: Int
)
```

### 4. Repository 层提供结构化进度回调

**决策**: 回调参数从 `(Int, Int, String)` 改为结构化数据类 `RefreshProgress`,最终回调使用 `RefreshSummary`

**理由**:
- 类型安全,避免参数顺序错误
- 易于扩展(未来可添加更多字段)
- 语义更清晰,代码可读性更好

**权衡**:
- 优点: 类型安全、易维护、符合 Kotlin 最佳实践
- 缺点: 需要新增 2 个数据类(但这是值得的抽象)

### 5. Toast 消息由 ViewModel 层统一管理

**决策**: 在 `TimelineViewModel` 中根据 `RefreshProgress` 和 `RefreshSummary` 生成 Toast 消息,通过 `Channel<UiEvent>` 发送到 UI 层

**理由**:
- 业务逻辑集中在 ViewModel,符合 MVVM 架构
- UI 层只需负责显示,无需关心消息格式化逻辑
- 便于单元测试(可验证 Toast 消息内容)

**权衡**:
- 优点: 职责分离清晰,易于测试和维护
- 缺点: ViewModel 需要处理 UI 相关的消息格式化(但这是合理的业务逻辑)

**实现位置**: `TimelineViewModel.kt:92-142` (refresh 方法)

**新增 UiEvent 类型**:
```kotlin
sealed class UiEvent {
    data class Toast(val message: String, val duration: Int = 2000) : UiEvent()
    data class SourceRefreshed(
        val sourceName: String,
        val newCount: Int,
        val success: Boolean
    ) : UiEvent()
    data class RefreshCompleted(
        val totalNew: Int,
        val failedSources: Int
    ) : UiEvent()
    // ... 其他事件
}
```

### 6. 防止重复刷新的锁机制

**决策**: 在 `TimelineViewModel.refresh()` 中使用 `_syncState.isSyncing` 作为互斥锁,忽略重复请求

**理由**:
- 当前已存在此机制(第 94 行:`if (_syncState.value.isSyncing) return@launch`)
- 无需额外实现,复用现有逻辑
- 简单有效,避免并发刷新问题

**权衡**:
- 优点: 复用现有代码,零额外成本
- 缺点: 用户连续点击刷新时无任何反馈(但这是合理的行为,用户可能误操作)

## Risks / Trade-offs

### Risk 1: 串行刷新导致总耗时增加
**风险**: 从并行改为串行后,如果有 10 个订阅源,每个平均 2 秒,总耗时从 2 秒增加到 20 秒

**缓解措施**:
- 用户感知的等待时间实际减少(每 2 秒就能看到新内容,而非等待 20 秒)
- 网络良好的情况下,单个源刷新通常 < 1 秒,总耗时仍可接受
- 未来可优化为"并行刷新,串行回调"的混合模式

### Risk 2: Toast 消息过于频繁
**风险**: 如果有 20 个订阅源,用户会看到 20 个 Toast,可能造成干扰

**缓解措施**:
- 规范要求每个 Toast 显示 2 秒,串行显示可避免重叠
- 新文章为 0 时,Toast 显示 1.5 秒(稍短)
- 可考虑未来添加"静默模式"设置选项

### Risk 3: 默认筛选改变影响现有用户
**风险**: 现有用户习惯"全部"视图,升级后默认看到"未读"可能造成困惑

**缓解措施**:
- 规范要求"保存用户偏好",如果用户之前手动选择过"全部",则使用保存的值
- 默认仅在"首次启动"或"无保存偏好"时生效
- 在 Changelog 中明确说明此变更

## Migration Plan

### 部署步骤
1. **创建 SettingsManager 工具类** - `util/SettingsManager.kt`,封装 SharedPreferences 读写
2. **修改 TimelineViewModel** - 从 SettingsManager 读取默认筛选值,依赖注入 SettingsManager
3. **新增 FilterType.displayName 扩展** - 为枚举添加中文显示名称("全部"/"未读"/"已读")
4. **修改 ProfileScreen** - 添加"默认筛选条件"设置 UI,使用 RadioButton 组件
5. **重构 NewsRepository.syncAll()** - 改为串行 + 结构化回调
6. **新增数据类** - `RefreshProgress`、`RefreshSummary`
7. **修改 TimelineViewModel.refresh()** - 处理增量进度,生成 Toast 事件
8. **扩展 UiEvent** - 添加 `SourceRefreshed`、`RefreshCompleted` 事件
9. **修改 TimelineScreen** - 监听新事件类型,显示 Toast
10. **单元测试** - SettingsManager 读写逻辑、Repository 刷新逻辑、ViewModel Toast 生成

### 回滚策略
- 新增文件:`SettingsManager.kt`,可直接删除
- 修改文件:`TimelineViewModel`、`ProfileScreen`、`NewsRepository`、`TimelineScreen`
- 如果设置功能导致问题,可保留刷新功能改进,仅回滚设置相关代码
- 如果刷新功能导致问题,可快速回滚到并行刷新逻辑(保留 `async/awaitAll` 代码分支)
- 数据库无变更,无需数据迁移
- SharedPreferences 数据可安全删除(无副作用)

### 测试计划
- **单元测试**:
  - SettingsManager: 读写逻辑、默认值处理、类型转换
  - Repository: 串行刷新逻辑、进度回调触发
  - ViewModel: Toast 消息格式化、防重复刷新、设置读取
- **集成测试**:
  - 设置保存后 Timeline 是否正确应用
  - 刷新流程端到端测试
  - 错误场景测试(网络异常、解析失败)
- **手动测试**:
  - 设置页面切换默认筛选,重启应用验证
  - 多订阅源场景(观察 Toast 序列)
  - 网络异常场景(验证错误 Toast)
  - 快速点击刷新按钮(验证防重复)

## Open Questions

1. **Q**: 是否需要添加"记住上次的筛选状态"功能?
   - **A**: 当前不实现。用户可通过设置"默认筛选条件"来指定启动时的筛选状态。"记住上次"会导致状态混乱(用户可能只是临时切换筛选器查看,不想改变默认行为)。

2. **Q**: 是否需要添加"静默刷新"选项(不显示 Toast)?
   - **A**: 当前不实现,保留未来扩展性。如果用户反馈 Toast 过于频繁,可在设置中添加开关。

3. **Q**: 串行刷新是否影响性能(如后台自动刷新)?
   - **A**: 后台自动刷新同样使用 `syncAll()`,会受到影响。但后台刷新通常不要求实时性,串行刷新可以接受。未来可考虑添加 `syncAllParallel()` 方法供后台使用。

4. **Q**: 如何处理用户在刷新过程中切换筛选器?
   - **A**: 刷新过程与筛选器独立,刷新完成后文章列表会根据当前筛选器自动过滤。不需要特殊处理。

5. **Q**: Toast 消息的国际化支持?
   - **A**: 当前所有消息硬编码为中文,项目暂未引入国际化框架。未来如果支持多语言,需要将消息字符串提取到资源文件。

6. **Q**: SettingsManager 是否需要监听 SharedPreferences 变化?
   - **A**: 当前不需要。设置修改后,只有在下次应用启动时才会生效。如果需要实时生效,可添加 SharedPreferences.OnSharedPreferenceChangeListener,但会增加复杂度,当前不实现。
