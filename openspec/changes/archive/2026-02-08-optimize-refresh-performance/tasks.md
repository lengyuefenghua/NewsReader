## 1. 并发刷新基础设施

- [ ] 1.1 在 `SettingsManager.kt` 添加 `getConcurrentCount(): Int` 方法（从 SharedPreferences 读取，默认值 3）
- [ ] 1.2 添加 `setConcurrentCount(count: Int)` 方法（保存到 SharedPreferences，范围 1-5）
- [ ] 1.3 添加并发数范围验证（< 1 时设为 1，> 5 时设为 5）
- [ ] 1.4 在 Koin 模块中验证 SettingsManager 已注册（复用现有配置）
- [ ] 1.5 添加 SettingsManager 单元测试（读写逻辑、边界值、默认值）

## 2. 设置页面 - 并发数配置

- [ ] 2.1 在 `SettingsScreen.kt` 导入 `Slider` 组件（Material 3）
- [ ] 2.2 在设置列表中添加"刷新并发数"部分（位于自动更新和缓存管理之间）
- [ ] 2.3 添加标题："刷新并发数"和说明："同时刷新的订阅源数量（1-5个）"
- [ ] 2.4 实现 `Slider` 组件（范围 1-5，步进 1，当前值从 SettingsManager 读取）
- [ ] 2.5 显示当前值文本："当前：X 个同时刷新"
- [ ] 2.6 在 `onValueChange` 回调中调用 `settingsManager.setConcurrentCount()`
- [ ] 2.7 添加值变更 Toast："刷新并发数已设置为：X"

## 3. 并发刷新实现

- [ ] 3.1 修改 `NewsRepository.syncAll()` 签名，添加 `concurrentLimit: Int` 参数（默认 3）
- [ ] 3.2 移除串行 `for` 循环，使用 `async { ... }.awaitAll()` 并发模式
- [ ] 3.3 创建 `Semaphore(concurrentLimit)` 限制并发协程数
- [ ] 3.4 在每个 async 块中使用 `semaphore.acquire()` 和 `semaphore.release()`
- [ ] 3.5 保留 `try-finally` 确保 Semaphore 正确释放（即使发生异常）
- [ ] 3.6 保留 `fetchAndSave()` 返回值（新增文章数量）
- [ ] 3.7 确保进度回调 `onProgress(RefreshProgress)` 在每个源完成时调用
- [ ] 3.8 修改 `syncAll()` 返回值为 `Result<RefreshSummary>`（保持不变）

## 4. 智能滚动机制

- [ ] 4.1 在 `TimelineViewModel.kt` 添加 `sealed class ScrollEvent`（ScrollToTop / NoOp）
- [ ] 4.2 创建 `Channel<ScrollEvent>(Channel.CONFLATED)` 用于滚动事件
- [ ] 4.3 添加 `scrollChannel` 公开为 StateFlow 或 receiveAsFlow()
- [ ] 4.4 添加滚动防抖逻辑：`lastScrollTime: Long` 和 `SCROLL_DEBOUNCE_MS = 500L`
- [ ] 4.5 创建私有方法 `notifyNewArticles(count: Int)`，判断是否发送滚动事件
- [ ] 4.6 在 `refresh()` 方法的 `onProgress` 回调中调用 `notifyNewArticles(progress.newArticleCount)`
- [ ] 4.7 在 `TimelineScreen.kt` 的 `LaunchedEffect` 中监听 `viewModel.scrollChannel`
- [ ] 4.8 接收到 `ScrollEvent.ScrollToTop` 时调用 `listState.animateScrollToItem(0, animSpec = ...)`

## 5. 简化刷新提示

- [ ] 5.1 在 `TimelineViewModel.kt` 的 `refresh()` 方法中移除所有 `_event.send(UiEvent.SourceRefreshed(...))`
- [ ] 5.2 保留 `_event.send(UiEvent.RefreshCompleted(...))` 在所有源完成后
- [ ] 5.3 移除源名称截断逻辑（不再需要，因为不显示单源Toast）
- [ ] 5.4 在 `TimelineScreen.kt` 中移除 `is UiEvent.SourceRefreshed` 分支
- [ ] 5.5 保留 `is UiEvent.RefreshCompleted` 分支（总计Toast显示逻辑保持不变）
- [ ] 5.6 在 `ui/common/UiEvent.kt` 中删除 `SourceRefreshed` 数据类
- [ ] 5.7 验证汇总Toast消息格式："刷新完成,共更新 N 篇新文章"或"刷新完成,更新 N 篇新文章,M 个源失败"

## 6. 精简默认筛选UI

- [ ] 6.1 在 `SettingsScreen.kt` 找到默认筛选设置的垂直布局代码
- [ ] 6.2 移除 `Column { RadioButton + Text + description }` 结构
- [ ] 6.3 添加 `Row` 水平布局，设置 `horizontalArrangement = Arrangement.spacedBy(8.dp)`
- [ ] 6.4 为每个 FilterType 创建 `FilterChip` 组件
- [ ] 6.5 使用 `Modifier.weight(1f)` 确保等宽分布
- [ ] 6.6 移除说明文字（`description`），只保留 `displayName`
- [ ] 6.7 设置每个 FilterChip 的 `selected` 状态（对比 defaultFilter）
- [ ] 6.8 点击 FilterChip 时调用 `settingsManager.setDefaultFilterType(type)`
- [ ] 6.9 Toast 持续时间从 2000ms 改为 1500ms（`Toast.makeText(context, message, 1500)`）
- [ ] 6.10 验证在小屏幕设备（320dp）上 FilterChip 不换行

## 7. 筛选条件导入更新

- [ ] 7.1 在 `SettingsScreen.kt` 确认已导入 `FilterChip` 组件
- [ ] 7.2 确认已导入 `remember` 和 `mutableStateOf`（用于当前选中状态）
- [ ] 7.3 验证 `displayName` 和 `description` 扩展属性已正确导入

## 8. 测试与验证

- [ ] 8.1 单元测试：SettingsManager 并发数读写（正常值、边界值 1/5、异常值 0/6）
- [ ] 8.2 单元测试：并发刷新逻辑（模拟 5 个源，并发数 3，验证不超过 3 个并发）
- [ ] 8.3 单元测试：滚动防抖机制（3 个事件在 500ms 内触发，验证只滚动 1 次）
- [ ] 8.4 集成测试：5 个订阅源，并发数 3，验证总耗时约为串行的 1/3
- [ ] 8.5 手动测试：设置并发数为 1，验证退化为串行刷新
- [ ] 8.6 手动测试：设置并发数为 5，验证刷新速度最快
- [ ] 8.7 手动测试：刷新时有新文章，验证自动滚动到顶部
- [ ] 8.8 手动测试：刷新时无新文章，验证不滚动
- [ ] 8.9 手动测试：多个源快速完成，验证只滚动 1 次（防抖）
- [ ] 8.10 手动测试：验证只显示总计 Toast，无单源 Toast
- [ ] 8.11 手动测试：在设置中切换默认筛选，验证 UI 紧凑（1-2 行）
- [ ] 8.12 UI 测试：小屏幕设备（320dp 宽）验证 FilterChip 不换行或切换为 DropdownMenu

## 9. 文档更新

- [ ] 9.1 更新 `changelog.md`，记录 v0.0.4 变更内容（并发刷新、智能滚动、简化提示、精简UI）
- [ ] 9.2 在 `CLAUDE.md` 中添加并发刷新配置说明
- [ ] 9.3 在 `CLAUDE.md` 中更新智能滚动行为说明
- [ ] 9.4 更新 `README.md`，说明新增的并发数设置选项
- [ ] 9.5 添加代码注释（Semaphore 使用、滚动防抖逻辑）

## 10. 发布准备

- [ ] 10.1 运行 `./gradlew build` 验证编译通过
- [ ] 10.2 运行 `./gradlew test` 执行单元测试
- [ ] 10.3 运行 `./gradlew installDebug` 安装到设备
- [ ] 10.4 手动冒烟测试（核心功能快速验证）
- [ ] 10.5 检查 ProGuard 混淆规则（确保新增类不被混淆）
- [ ] 10.6 性能测试：10 个订阅源，并发数 3，对比串行刷新耗时
- [ ] 10.7 生成 Release APK (`./gradlew assembleRelease`)
- [ ] 10.8 最终测试 Release 版本
