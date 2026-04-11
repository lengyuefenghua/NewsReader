# Changelog

本文件记录 NewsReader 项目的所有重要修改。

### 2026-04-11 发现订阅流、管理入口与发布信息完善

**修改原因：**
1. 用户希望从“发现”页和“订阅”页都能先预览文章样例，再决定是否保存订阅源。
2. 用户需要更直接的界面反馈，快速判断未读数量和订阅管理相关操作入口。
3. 发布前需要在应用内展示版本信息，并统一导出文件和 APK 的命名规则，降低分享和备份时的混乱。

**修改内容：**
- **发现页与订阅预览扩展**：
  - 新增 `Plink`、`Awesome RSSHub Routes`、`Top RSS List`、`Wechat2RSS` 和 `QiReader` 等发现入口
  - 新增通用 `FeedPreviewScreen`，支持从“订阅”页预览文章样例后直接保存或进入高级编辑
  - `QiReader` 采用 WebView 借壳发现模式，支持登录后在网页内拦截“订阅”动作并跳转到应用内预览页
- **订阅管理与时间线反馈优化**：
  - 调整“订阅”页组织方式，让导入导出、预览和编辑流转更集中
  - 为时间线“未读”筛选增加数量角标，减少来回切换确认成本
- **发布信息与导出命名统一**：
  - “关于”弹窗展示版本号、构建日期、作者和 GitHub 链接
  - 订阅源导出和完整数据备份文件名统一为 `NewsReader_Feeds_Backup_YYYYMMDD.json` 与 `NewsReader_Data_Backup_YYYYMMDD.json`
  - release APK 输出名统一为 `NewsReaderV<version>.apk`

**技术细节：**
- `DiscoverScreen.kt` / `PlinkMarketScreen.kt` / `QiReaderMarketScreen.kt`：扩展发现页入口与市场导航
- `FeedPreviewScreen.kt` / `FeedPreviewViewModel.kt` / `PlinkFeedPreviewScreen.kt`：实现订阅预览、直接保存和高级编辑流转
- `TimelineScreen.kt`：为“未读”筛选增加角标显示
- `ProfileScreen.kt` / `app/build.gradle.kts`：展示版本与构建日期，并通过 `BuildConfig` 注入发布信息
- `BackupFileNameUtils.kt` / `SourceManagerScreen.kt` / `SettingsScreen.kt`：统一导出文件名

**相关提交：**
- `4121f91`: feat: add feed preview subscription flow
- `a20d577`: feat: add Plink discovery flow
- `07c5fec`: feat: expand discover feed catalogs
- `bc6ecfa`: feat: add QiReader WebView discovery flow
- `7caef9c`: feat: show unread badge on timeline filter
- `3c2eaa1`: feat: improve source manager organization
- `cd6b058`: feat: surface release info and standardize exports

---

### 2026-04-04 设置页滑条交互优化

**修改原因：**
1. 用户反馈“文章保留”输入框编辑时会出现小水滴样式的文本句柄，影响体验
2. 用户希望“文章保留”和“并发刷新”都改成更直观的一致性离散滑条交互
3. 用户希望后续界面调整前先输出文字版 UI 图，减少对布局理解的偏差

**修改内容：**
- **设置页离散滑条统一**：
  - 将“文章保留”从文本输入改为 5 个吸附点的大圆点滑条
  - “文章保留”固定吸附到 `10 / 50 / 100 / 500 / 1000`
  - 将“并发刷新”从增减按钮改为同样样式的离散滑条
  - “并发刷新”固定吸附到 `1 / 2 / 3 / 4 / 5`
  - 当前值继续显示在滑条前方，两个滑条的数值列宽度保持一致
- **交互与视觉优化**：
  - 吸附点常显为圆点，当前点高亮
  - 去掉吸附点下方数字标签，界面更简洁
  - 保持拖动即保存，避免再次出现文本输入句柄问题
- **协作规则补充**：
  - 在 `AGENTS.md` 新增规则：后续做 UI/布局调整前，先给出文字版 UI 图或布局图

**技术细节：**
- `SettingsScreen.kt`：新增复用的 `DiscreteDotSlider`，统一“文章保留”和“并发刷新”的离散滑条样式与吸附逻辑
- `SettingsScreenStateTest.kt`：补充缓存保留值、并发数吸附逻辑与统一数值列宽度的回归测试
- `AGENTS.md`：新增 UI 调整前需先输出文字版布局图的工作流规则
- `docs/plans/`：补充本轮设置页滑条设计与实现计划文档

---

### 2026-02-08 v0.0.5 Bug 修复

**修改原因：**
1. 用户反馈数据备份恢复功能失败，无法正确导入之前的设置
2. 用户反馈本地订阅源导入失败时没有明确错误提示，难以排查问题

**修改内容：**
- **修复数据备份恢复**：
  - `BackupSettings` 添加 `defaultFilterType` 字段（默认筛选条件）
  - `BackupSettings` 添加 `concurrentCount` 字段（并发刷新数量）
  - 备份版本号从 "1.0" 更新到 "2.0"
  - 恢复时兼容旧版本备份（缺失字段使用默认值）
  - `SettingsViewModel` 添加 `settingsManager` 依赖注入
  - `NewsReaderApplication` 添加 `settingsManager` 初始化
- **修复订阅源导入错误提示**：
  - `ImportResult` 添加 `error` 字段用于传递错误详情
  - `ImportResult` 添加 `hasError` 属性快速判断是否有错误
  - JSON 解析失败时返回详细错误信息和格式示例
  - 导入失败时显示具体错误原因而非通用提示
  - 支持显示 JSON 格式错误的详细信息
  - 空数据检查，返回明确的"导入内容为空"提示

**技术细节：**
- `BackupData.kt`: 扩展 `BackupSettings` 数据类，新增 2 个字段
- `SettingsViewModel.kt`: `createBackup()` 保存新设置，`restoreBackup()` 兼容旧版本
- `SourceViewModel.kt`: `ImportResult` 添加错误支持，改进错误处理
- `SourceManagerScreen.kt`: UI 层检查并显示详细错误信息

**相关提交：**
- `860abc1`: fix: 修复数据备份恢复和订阅源导入问题

---

### 2026-02-08 v0.0.4 性能优化与 UI 简化

**修改原因：**
1. 用户反馈串行刷新速度太慢，10 个订阅源需要等待 30-60 秒
2. 用户反馈每次刷新都滚动到顶部很烦人，尤其是没有新文章时
3. 用户反馈单源刷新时提示太多 Toast，影响阅读体验
4. 用户反馈设置页面太繁杂，占用空间太大，需要滚动很多
5. 用户反馈输入框太大，边框太明显

**修改内容：**
- **并发刷新性能优化**：
  - 新增"并发刷新"设置项，支持调整并发刷新数量（1-5，默认 3）
  - 使用增减按钮（−/+）控制，比 Slider 更直观
  - 使用 Semaphore 控制并发数，避免资源耗尽
  - 使用 Kotlin Coroutines async/awaitAll 实现并发刷新
  - 高并发数刷新速度提升 2-5 倍（取决于网络和设备性能）
  - 单源失败不影响其他源继续刷新
- **智能滚动优化**：
  - 仅当有新文章时才自动滚动到列表顶部
  - 无新文章时保持当前阅读位置
  - 单源刷新显示详细新增文章数
  - 避免不必要的界面跳转，提升阅读体验
- **精简刷新通知**：
  - 移除单源刷新完成时的 Toast 提示
  - 仅保留最终汇总通知："刷新完成,共更新 N 篇新文章"
  - 减少提示干扰，让用户专注于阅读
- **设置页面大幅简化**：
  - 所有设置项高度统一为 48dp，视觉整齐
  - 删除所有冗余的灰色描述文字
  - 移除所有分割线，减少视觉噪音
  - 整体间距从 16dp 减少到 12dp，更紧凑
  - 筛选选项从垂直 RadioButton 改为水平 FilterChip 布局
  - 并发刷新使用增减按钮（− 数字 +）代替 Slider
  - 文章保留数量使用紧凑的 BasicTextField（80dp × 36dp，1dp 细边框）
  - 按钮高度统一为 40dp，字体为 bodyMedium
  - 移除所有 Toast 提示，点击即生效
  - 代码从 356 行减少到 243 行（减少 32%）

**技术细节：**
- `SettingsManager`: 新增 `getConcurrentCount()` 和 `setConcurrentCount()` 方法（范围 1-5）
- `NewsRepository.syncAll()`: 使用 Semaphore 和 async/awaitAll 实现并发刷新，使用 AtomicInteger 线程安全地跟踪完成数
- `NewsRepository.syncSource()`: 明确返回类型为 `Int?`
- `RefreshProgress`: 添加 `current` 和 `total` 字段，正确显示刷新进度（如 "2/5"）
- `TimelineViewModel`: 智能判断是否发送 ScrollToTop 事件，传入 `settingsManager.getConcurrentCount()` 到 `syncAll()`
- `SettingsScreen`: 完全重构，使用 `SettingRow` 辅助函数统一布局，BasicTextField 替代 OutlinedTextField
- `UiEvent`: 移除 `SourceRefreshed` 事件类
- 设置页面新增 `SettingRow` 辅助函数，确保所有设置项高度统一为 48dp

---

### 2026-02-08 v0.0.3 用户体验优化

**修改原因：**
1. 用户反馈每次打开应用都需要手动切换到"未读"才能看到新内容，操作繁琐
2. 用户反馈刷新订阅源时不知道进度，也不知道更新了多少新文章
3. 用户反馈批量刷新需要等待所有源完成才能看到新内容，等待时间过长
4. 用户希望能够自定义启动时的默认筛选条件，满足个人阅读习惯

**修改内容：**
- **默认筛选状态可配置**：
  - 新增设置页面"默认筛选条件"选项
  - 支持用户选择"全部/未读/已读"作为应用启动时的默认筛选器
  - 工厂默认值：未读（关注新内容）
  - 使用 SharedPreferences 持久化用户选择
  - 重启应用后自动恢复用户设置
- **智能刷新进度反馈**：
  - 刷新过程中显示每个订阅源的实时进度提示
  - 单源完成提示："{订阅源名}: 已更新 X 篇新文章"或"无新文章"或"刷新失败"
  - 所有源完成提示："刷新完成,共更新 N 篇新文章"（失败时显示失败源数量）
  - Toast 消息时长：成功 2 秒，无新文章 1.5 秒，失败 2 秒，总计 3 秒
  - 源名称超过 20 字符时自动截断为 17 字符 + "..."
- **增量刷新策略**：
  - 从批量刷新改为增量刷新，每个订阅源刷新完立即更新 UI
  - 串行执行策略确保进度反馈准确（按顺序逐个刷新）
  - 单源刷新失败不影响其他源继续刷新
  - 自动统计新增文章数量（通过对比数据库插入前后数量）
- **筛选器显示名称扩展**：
  - 为 `FilterType` 枚举添加 `displayName` 属性（"全部"/"未读"/"已读"）
  - 添加 `description` 属性用于设置页面说明文字
- **设置管理器新增**：
  - 新增 `SettingsManager.kt` 工具类，封装 SharedPreferences 读写
  - 提供类型安全的 API：`getDefaultFilterType()` / `setDefaultFilterType()`
  - 在 Koin 中注册为单例，支持依赖注入
- **数据模型扩展**：
  - 新增 `RefreshProgress.kt`：单源刷新进度数据
  - 新增 `RefreshSummary.kt`：刷新汇总数据
  - 扩展 `UiEvent`：添加 `SourceRefreshed` 和 `RefreshCompleted` 事件
  - 扩展 `ArticleDao`：添加 `getArticleCountBySource()` 方法
- **Repository 层重构**：
  - `syncAll()` 改为串行执行 + 结构化进度回调
  - `fetchAndSave()` 返回新增文章数量
  - 完善错误处理，单源失败不影响其他源
- **ViewModel 层优化**：
  - `TimelineViewModel` 集成 `SettingsManager`，从设置读取默认筛选值
  - 移除硬编码的 `FilterType.ALL` 默认值
  - 刷新逻辑发送增量进度事件和汇总事件

**影响范围：**
- 新增文件：`SettingsManager.kt`、`RefreshProgress.kt`
- 修改文件：`TimelineViewModel.kt`、`NewsRepository.kt`、`ArticleDao.kt`、`TimelineScreen.kt`、`SettingsScreen.kt`、`TimelineUiState.kt`、`UiEvent.kt`、`ViewModelModule.kt`、`AppModule.kt`
- 用户体验：首次打开应用显示未读文章，刷新时实时看到进度，设置更灵活
- 无破坏性变更：向后兼容，用户设置不会被覆盖

**测试结果：**
- 功能测试：✅ 默认筛选未读、设置页面 UI、即时生效、增量刷新、进度反馈、源名称截断
- 兼容性测试：✅ 单源刷新模式、快速点击防重复、网络异常处理
- 性能测试：✅ 10 个订阅源串行刷新总耗时约 20 秒（每个源 2 秒）
- UI 测试：✅ 筛选器切换、Toast 顺序显示、设置页面交互

---

### 2026-02-07 v0.0.2 新增功能 & 性能优化

**修改原因：**
1. 用户希望在重装应用后能够恢复订阅源、阅读数据等所有数据，即使数据库版本升级也能无损导入
2. 用户反馈启用正文提取算法后加载速度慢，影响使用体验
3. 用户反馈原始网页会闪现，影响视觉体验

**修改内容：**
- **新增完整数据备份恢复功能**：
  - 在设置页面添加"数据备份"区域
  - 备份功能：导出所有订阅源、文章、用户设置为 JSON 格式
  - 恢复功能：从备份文件导入数据（全部覆盖策略）
  - 备份文件名：`NewsReader_Full_Backup_[时间戳].json`
  - 支持跨版本升级恢复，数据库版本变更时也能无损导入
- **正文提取性能大幅提升**：
  - 优化前：1500-1700ms（包含 PageLoader 等待）
  - 优化后：200-400ms（跳过等待，直接提取）
  - 性能提升约 75-85%
  - 修改策略：在 `onPageFinished` 后立即提取，跳过 `hasContent()` 检查
- **资源拦截优化**：
  - 拦截广告脚本（`popup.js`, `adsbygoogle.js` 等）
  - 拦截字体文件（`.woff`, `.ttf` 等）
  - 拦截视频文件（`.mp4`, `.webm` 等）
  - 保留所有图片（正文图片正常显示）
  - 进一步提升加载速度
- **遮罩层显示优化**：
  - 白色遮罩层覆盖原始网页（完全无闪现）
  - 顶部进度条实时反馈
  - 提取完成后延迟 300ms 隐藏（等待 DOM 更新）
- **模式切换功能修复**：
  - 添加 `lastViewMode` 状态检测模式切换
  - 模式切换时强制重新加载 URL
  - 修复标准 RSS 无法切换到网页模式的问题
- **图标显示逻辑修正**：
  - viewMode = 0（内容模式）→ 📄 文档图标
  - viewMode = 1（网页模式）→ 🌍 地球图标
  - 所有文章默认网页模式，统一用户体验
- **切换按钮启用条件优化**：
  - 纯网页文章（无内容、无算法、无CSS）：禁用切换按钮
  - 其他情况（至少有一种可切换）：启用切换按钮
- **数据模型扩展**：
  - 新增 `BackupData.kt`：完整数据备份模型
  - 新增 `BackupSettings.kt`：用户设置备份数据
  - 扩展 `ArticleDao` 和 `SourceDao`：添加 `getAllArticles()`、`deleteAllArticles()` 等方法

**影响范围：**
- app/build.gradle.kts (版本号更新至 v0.0.2)
- app/src/main/java/com/lengyuefenghua/newsreader/data/BackupData.kt (新增)
- app/src/main/java/com/lengyuefenghua/newsreader/data/ArticleDao.kt
- app/src/main/java/com/lengyuefenghua/newsreader/data/SourceDao.kt
- app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/SettingsViewModel.kt
- app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt
- app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreen.kt
- app/src/main/java/com/lengyuefenghua/newsreader/utils/WebViewContentExtractor.kt
- gradlew.bat (新增 Windows 批处理脚本)

**相关提交：**
- feat: 添加完整数据备份恢复功能
- perf: 大幅优化正文提取性能（提升 75-85%）
- perf: 添加资源拦截功能（广告、字体、视频）
- fix: 修复原始网页闪现问题
- fix: 修复模式切换功能
- fix: 修正图标显示逻辑

---
