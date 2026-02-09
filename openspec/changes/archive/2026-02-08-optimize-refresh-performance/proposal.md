## Why

当前刷新功能存在以下用户体验问题需要优化：1) 串行刷新导致多订阅源场景下总耗时过长；2) 每个订阅源刷新完成都弹出 Toast 提示过于频繁，造成干扰；3) 设置页面中默认筛选选项占用空间过大，影响页面美观和滚动体验。需要通过并发刷新、简化提示、精简UI来提升整体用户体验。

## What Changes

- **并发刷新可配置**: 在设置中添加"刷新并发数"选项，允许用户选择同时刷新的订阅源数量（1-5个，默认3个）
- **智能自动滚动**: 每个订阅源刷新完成且有新文章时，自动滚动到列表顶部；无新文章时不滚动
- **简化刷新提示**: 移除单个订阅源的 Toast 提示，仅在所有订阅源刷新完成后显示一次总计提示
- **精简筛选设置UI**: 将默认筛选的三个选项从垂直布局改为单行/双行水平布局，使用紧凑的 Segmented Button 或下拉选择器
- **刷新行为优化**: 仅在有新文章时触发滚动到顶部，避免频繁滚动影响用户体验

## Capabilities

### New Capabilities
- `concurrent-refresh-config`: 并发刷新数量配置选项，允许用户自定义并发度
- `smart-scroll-on-refresh`: 智能滚动机制，根据新文章数量决定是否滚动到顶部
- `simplified-refresh-notification`: 简化的刷新提示，仅显示总计信息

### Modified Capabilities
- `default-filter-settings`: 优化UI布局，减少占用空间
- `incremental-feed-refresh`: 修改为并发执行，调整进度反馈机制

## Impact

**受影响的代码模块**:
- `data/NewsRepository.kt` - syncAll() 方法改为并发执行，添加并发数控制参数
- `ui/screens/SettingsScreen.kt` - 添加"刷新并发数"设置选项，精简默认筛选UI布局
- `util/SettingsManager.kt` - 添加并发数设置的读写方法
- `viewmodel/TimelineViewModel.kt` - 调整刷新逻辑，添加智能滚动触发
- `ui/screens/SettingsScreen.kt` - 重构默认筛选UI为紧凑布局（SegmentedButton 或 ExposedDropdownMenu）
- `ui/screens/TimelineScreen.kt` - 移除 SourceRefreshed 事件处理，保留 RefreshCompleted

**用户体验变化**:
- 刷新速度显著提升（3个并发源时约为串行的1/3时间）
- 减少了Toast提示的干扰（从N+1个减少到1个）
- 设置页面更简洁，滚动更流畅
- 自动滚动行为更智能，只在有意义时触发

**无破坏性变更** - 向后兼容，默认配置下行为优化但保持原有体验
