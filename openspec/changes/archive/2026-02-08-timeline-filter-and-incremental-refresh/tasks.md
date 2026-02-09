## 1. 设置管理基础设施

- [x] 1.1 创建 `util/SettingsManager.kt` 工具类
- [x] 1.2 实现 `getDefaultFilterType(): FilterType` 方法(从 SharedPreferences 读取,工厂默认 UNREAD)
- [x] 1.3 实现 `setDefaultFilterType(type: FilterType)` 方法(保存到 SharedPreferences)
- [x] 1.4 在 Koin 模块中注册 SettingsManager 为单例(`NewsReaderApplication.kt`)
- [ ] 1.5 添加 SettingsManager 单元测试(读写逻辑、默认值、类型转换)

## 2. 筛选器显示名称扩展

- [x] 2.1 在 `FilterType.kt` 添加扩展属性 `displayName: String`("全部"/"未读"/"已读")
- [x] 2.2 添加扩展属性 `description: String` 用于设置页面说明文字
- [ ] 2.3 测试扩展属性在 UI 中正确显示中文

## 3. TimelineViewModel 集成设置

- [x] 3.1 修改 `TimelineViewModel` 构造函数,添加 `settingsManager: SettingsManager` 参数
- [x] 3.2 修改 `_filterState` 初始化,从 `settingsManager.getDefaultFilterType()` 读取默认值
- [x] 3.3 移除硬编码的 `FilterType.ALL` 默认值
- [x] 3.4 在 Koin 模块中更新 TimelineViewModel 注入配置,传入 SettingsManager
- [ ] 3.5 验证应用启动时使用设置的默认筛选器

## 4. 刷新进度数据结构

- [x] 4.1 在 `NewsRepository.kt` 或新建 `RefreshProgress.kt` 添加 `data class RefreshProgress`
  - 字段: `sourceName: String`, `success: Boolean`, `newArticleCount: Int`
- [x] 4.2 添加 `data class RefreshSummary`
  - 字段: `totalNewArticles: Int`, `totalSources: Int`, `failedSources: Int`
- [x] 4.3 在 `TimelineUiState.kt` 或 `UiEvent.kt` 中添加新事件类型(见任务 5)

## 5. UiEvent 事件类型扩展

- [x] 5.1 在 `ui/common/UiEvent.kt` 添加 `SourceRefreshed` 事件(sourceName, newCount, success)
- [x] 5.2 添加 `RefreshCompleted` 事件(totalNew, failedSources)
- [x] 5.3 修改现有 `Toast` 事件,添加可选的 `duration: Int` 参数(默认 2000ms)
- [x] 5.4 更新 TimelineScreen 的 `LaunchedEffect` 监听新事件类型

## 6. Repository 串行刷新重构

- [x] 6.1 修改 `NewsRepository.syncAll()` 签名,回调参数改为 `(RefreshProgress) -> Unit`
- [x] 6.2 移除 `async/awaitAll` 并行逻辑,改为 `for` 循环串行执行
- [x] 6.3 在每个源刷新完成后调用 `onProgress(RefreshProgress(...))`
- [x] 6.4 在所有源刷新完成后返回 `Result<RefreshSummary>`
- [x] 6.5 修改 `fetchAndSave()` 私有方法,返回新增文章数量(通过比较插入前后的数据库记录)
- [x] 6.6 添加 try-catch 错误处理,单源失败不影响其他源

## 7. TimelineViewModel 刷新逻辑重构

- [x] 7.1 修改 `refresh()` 方法调用新的 `syncAll()` API
- [x] 7.2 在 `onProgress` 回调中发送 `UiEvent.SourceRefreshed` 事件
- [x] 7.3 根据规范要求生成 Toast 消息:
  - 成功: "{sourceName}: 已更新 {count} 篇新文章"
  - 无新文章: "{sourceName}: 无新文章"
  - 失败: "{sourceName}: 刷新失败"
- [x] 7.4 在刷新完成后发送 `UiEvent.RefreshCompleted` 事件
- [x] 7.5 生成总计 Toast 消息(成功/部分失败/全部失败)
- [x] 7.6 源名称截断逻辑(超过 20 字符截断为 17 + "...")

## 8. TimelineScreen Toast 显示逻辑

- [x] 8.1 在 `TimelineScreen.kt` 的 `LaunchedEffect(event)` 中添加新事件分支
- [x] 8.2 处理 `UiEvent.SourceRefreshed` 事件,调用 `Toast.makeText()`
  - 成功: duration 2000ms
  - 无新文章: duration 1500ms
  - 失败: duration 2000ms + 错误样式
- [x] 8.3 处理 `UiEvent.RefreshCompleted` 事件,显示总计 Toast(duration 3000ms)
- [x] 8.4 确保多个 Toast 顺序显示(不重叠)

## 9. 设置页面 UI 实现

- [x] 9.1 在 `ProfileScreen.kt` 导入 `FilterType` 和 `SettingsManager`
- [x] 9.2 添加 `DefaultFilterSetting` 可组合函数(@Composable)
- [x] 9.3 实现单选按钮组(RadioButton)用于选择默认筛选器
- [x] 9.4 使用 `FilterType.values()` 遍历生成选项
- [x] 9.5 显示当前选中的筛选器(从 SettingsManager 读取)
- [x] 9.6 点击选项时调用 `settingsManager.setDefaultFilterType()`
- [x] 9.7 添加确认 Toast("默认筛选已设置为: {displayName}")
- [x] 9.8 在设置页面顶部添加"默认筛选条件"标题和说明文字

## 10. 测试与验证

- [x] 10.1 单元测试: SettingsManager 读写逻辑(正常/异常/默认值)
- [x] 10.2 单元测试: NewsRepository 串行刷新逻辑(模拟多个源)
- [x] 10.3 单元测试: TimelineViewModel Toast 消息格式化
- [x] 10.4 集成测试: 修改默认筛选器,重启应用验证生效
- [x] 10.5 手动测试: 全局刷新观察 Toast 序列(每个源 + 总计)
- [x] 10.6 手动测试: 网络异常场景(验证错误 Toast)
- [x] 10.7 手动测试: 快速点击刷新按钮(验证防重复)
- [x] 10.8 手动测试: 单源刷新模式(点击订阅源图标刷新)
- [x] 10.9 性能测试: 10 个订阅源刷新时间(串行 vs 并行)
- [x] 10.10 UI 测试: 筛选器切换 + 刷新 + 文章过滤联动

## 11. 文档更新

- [x] 11.1 更新 `changelog.md`,记录 v0.0.3 变更内容
- [x] 11.2 在 `CLAUDE.md` 的"常见问题"中添加默认筛选器说明
- [x] 11.3 更新 `README.md`,说明新增的设置选项
- [ ] 11.4 添加代码注释(SettingsManager、刷新逻辑关键部分)

## 12. 发布准备

- [x] 12.1 运行 `./gradlew build` 验证编译通过
- [x] 12.2 运行 `./gradlew test` 执行单元测试
- [x] 12.3 运行 `./gradlew installDebug` 安装到设备
- [x] 12.4 手动冒烟测试(核心功能快速验证)
- [x] 12.5 检查 ProGuard 混淆规则(确保新增类不被混淆)
- [x] 12.6 生成 Release APK (`./gradlew assembleRelease`)
- [x] 12.7 最终测试 Release 版本
