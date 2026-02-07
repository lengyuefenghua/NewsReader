## Why

当前时间线页面默认显示"全部"文章,用户每次打开应用都需要手动切换到"未读"才能看到新内容,体验不佳。同时,刷新订阅源时缺乏反馈,用户不知道更新了多少新文章,且当前采用批量刷新策略(所有订阅源全部刷新完才更新列表),导致用户等待时间过长,无法及时看到已刷新的内容。

## What Changes

- **时间线默认筛选状态**: 提供默认筛选设置选项,用户可在设置中选择"全部/未读/已读"作为启动时的默认筛选条件(默认值为"未读")
- **设置页面新增选项**: 在设置页面添加"默认筛选条件"选项,支持单选按钮切换(全部/未读/已读)
- **筛选偏好持久化**: 使用 SharedPreferences 保存用户选择的默认筛选条件,应用重启后自动恢复
- **刷新进度反馈**: 在刷新过程中显示 Toast 提示,告知用户每个订阅源更新了多少篇新文章
- **增量刷新策略**: 改为每完成一个订阅源刷新就立即更新 UI,而不是等待所有订阅源全部刷新完成
- **总计统计**: 刷新全部完成后,显示总计更新了多少篇新文章

## Capabilities

### New Capabilities
- `timeline-default-filter`: 时间线默认筛选状态管理
- `default-filter-settings`: 设置页面中的默认筛选条件配置选项
- `incremental-feed-refresh`: 增量式订阅源刷新机制
- `refresh-progress-feedback`: 刷新进度和结果反馈

### Modified Capabilities
- 无现有规范需要修改(这是首次使用 OPSX 工作流)

## Impact

**受影响的代码模块**:
- `ui/screens/TimelineViewModel.kt` - 默认筛选状态、刷新逻辑修改,从 SharedPreferences 读取设置
- `ui/common/TimelineUiState.kt` - 可能需要添加刷新进度状态
- `ui/common/FilterType.kt` - 确认枚举定义(无需修改)
- `data/NewsRepository.kt` - 刷新接口需要支持增量更新回调
- `ui/screens/TimelineScreen.kt` - Toast 提示显示
- `ui/screens/ProfileScreen.kt` - 设置页面添加"默认筛选条件"选项
- `util/SettingsManager.kt` - 新增设置管理工具类,负责 SharedPreferences 读写

**用户体验变化**:
- 时间线首次打开默认显示未读文章(可在设置中修改)
- 用户可在设置页面自定义启动时的默认筛选条件(全部/未读/已读)
- 刷新时实时看到每个订阅源的更新进度
- 刷新完成后显示总计更新数量

**无破坏性变更** - 向后兼容,用户设置不会被覆盖
