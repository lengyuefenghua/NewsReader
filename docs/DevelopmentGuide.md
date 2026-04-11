# 开发指南

## 先读这里

这是 `NewsReader` 面向 AI 辅助开发的主要技术交接文档。

使用本指南的目标有四个：
- 快速理解项目
- 找到各功能对应的代码文件
- 梳理关键运行调用链
- 避免在高风险区域做错误修改

在修改代码前：
- 先读本文件
- 再读你准备修改的目标文件
- 先给出明确方案并等待用户确认

不要用本指南代替对实际目标文件的阅读。

## 给 AI 代理的快速起步

如果你对仓库完全没有上下文，按以下顺序阅读：

1. `AGENTS.md`
2. `README.md`
3. `app/src/main/java/com/lengyuefenghua/newsreader/NewsReaderApplication.kt`
4. `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt`
5. `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/TimelineViewModel.kt`
6. `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`
7. `app/src/main/java/com/lengyuefenghua/newsreader/data/AppDatabase.kt`
8. 本文后面列出的功能相关文件

如果任务描述不清楚，不要猜。先从下文的任务入口定位功能区，再只阅读该功能相关的文件。

## 技术栈

- 平台：Android
- 语言：Kotlin
- UI：Jetpack Compose Material3
- 架构：Compose UI -> ViewModel -> Repository -> Room/network
- 依赖注入：Koin，部分 ViewModel 仍使用 `AndroidViewModel`
- 数据库：Room
- 设置存储：DataStore Preferences + SharedPreferences
- 网络：OkHttp
- 解析：Jsoup、自定义 RSS 解析器、自定义 HTML 解析器
- 网页内容提取：WebView + `app/src/main/assets/algorithm/` 下的注入式 JavaScript 算法
- 异步与状态：Coroutines、Flow、StateFlow、Channel
- 测试：JUnit4 JVM 测试、Android instrumentation 测试

## 这个应用是什么

`NewsReader` 是一个本地优先的 Android RSS 阅读器。

应用会从 RSS 订阅源或自定义 HTML 源抓取内容，把文章存入本地 Room 数据库，再通过 Compose UI 展示给用户。最重要的产品主循环是：

```text
抓取订阅源 -> 解析内容 -> 保存文章到本地 -> 渲染时间线 -> 打开文章 -> 记录阅读状态
```

做修改时，优先从这条主循环去理解影响范围。

## 系统架构

`NewsReader` 是单模块 Android 应用，核心围绕“本地优先 RSS 阅读”流程展开。

```text
Compose UI
  -> ViewModel
  -> Repository / DAO / Settings
  -> Room / Network / WebView
  -> Flow / StateFlow 回流到 UI
```

关键分层如下。

### 1. 启动与依赖图

- `app/src/main/java/com/lengyuefenghua/newsreader/NewsReaderApplication.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/core/di/AppModule.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/core/di/ViewModelModule.kt`

职责：
- 初始化 Koin
- 注册全局异常处理器
- 初始化 `WebViewManager`
- 创建应用级设置与偏好仓库

### 2. 应用外壳与导航

- `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/core/navigation/NavRoutes.kt`

职责：
- 承载根 Compose UI
- 定义主标签结构与次级路由
- 在启用自动更新时于启动阶段触发刷新

### 3. 状态与业务编排

- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/`

职责：
- 通过 `StateFlow` 暴露长期页面状态
- 通过 `Channel` 发送一次性 UI 事件
- 协调 repository、DAO 和 settings 的访问

### 4. 数据编排与持久化

- `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/AppDatabase.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/ArticleDao.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/SourceDao.kt`

职责：
- 抓取 RSS 或自定义 HTML 源
- 将内容解析成 `Article`
- 通过 Room 读写本地数据
- 向 UI 层暴露文章与订阅源相关 Flow

### 5. 全文阅读与正文提取

- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/utils/WebViewContentExtractor.kt`
- `app/src/main/assets/algorithm/`

职责：
- 在 WebView 中显示文章页面
- 注入正文提取脚本
- 用不同算法提取可读内容

## 如何快速定位代码

在做更大范围搜索前，先用下面这些入口。

如果任务提到：
- 时间线、未读/已读筛选、刷新、进度条：从 `TimelineViewModel.kt` 开始
- 订阅源列表、导入、导出、重复订阅源、编辑订阅源：从 `SourceViewModel.kt` 开始
- 发现页、订阅市场插件、目录型 RSS 源接入：先看 `docs/DiscoverSourceGuide.md`；直连解析模式再看 `DiscoverViewModel.kt`，WebView 借壳模式先看对应 `XxxMarketScreen.kt`（如 `QiReaderMarketScreen.kt`）
- 订阅源预览、从预览页直接保存、从预览页跳高级编辑：先看 `FeedPreviewScreen.kt`；发现市场预览再看 `PlinkFeedPreviewScreen.kt`
- 文章页、WebView、提取、可读性、内容模式：从 `ArticleScreen.kt` 开始
- 备份、恢复、自动更新、缓存、默认筛选：从 `SettingsViewModel.kt` 开始
- 个人中心、关于、版本号/构建日期、开源致敬：从 `ProfileScreen.kt` 开始
- 收藏或阅读统计：从 `ArticleDao.kt` 开始，再看对应 ViewModel

如果拿不准，先定位负责该功能的 ViewModel，再沿着调用链往下追到 repository、DAO 和工具类。

## 核心调用关系

### 启动与自动刷新

```text
NewsReaderApplication.onCreate()
  -> startKoin(appModule + viewModelModule)
  -> WebViewManager.init()
  -> create UserPreferencesRepository and SettingsManager

MainActivity.NewsReaderApp()
  -> read autoUpdateFlow.first()
  -> TimelineViewModel.refresh() when auto update is enabled
```

### 时间线刷新流程

```text
TimelineScreen
  -> TimelineViewModel.fetchArticles()
  -> TimelineViewModel.refresh()
  -> NewsRepository.syncAll(concurrentLimit, onProgress)
  -> SourceDao.getAllSources().first()
  -> fetchAndSave(source)
  -> RssParser or HtmlParser
  -> ArticleDao.insertArticles(items)
  -> RefreshSummary returned to TimelineViewModel
  -> UiEvent.RefreshCompleted / UiEvent.ScrollToTop
  -> TimelineScreen reacts to event and updates UI
```

解读：
- UI 从 `TimelineScreen` 触发刷新
- 编排逻辑在 `TimelineViewModel`
- 抓取与持久化在 `NewsRepository`
- 解析结果写入 Room
- Room Flow 自动推动 UI 更新

### 单订阅源时间线流程

```text
SourceManagerScreen
  -> navigate to source feed route
  -> TimelineViewModel.showSource(sourceId)
  -> _sourceFilter switches the article flow
  -> TimelineScreen reads filtered articles StateFlow
```

### 文章详情与内容提取流程

```text
TimelineScreen / FavoritesScreen
  -> navigate to article route
  -> ArticleScreen
  -> load article URL in WebView
  -> WebViewContentExtractor injects JS assets
  -> extraction result rendered in content mode or page mode
```

解读：
- 导航层传入文章 URL
- 文章页解析并订阅实时文章状态
- WebView 负责整页渲染
- 提取工具通过注入 JavaScript 算法生成正文模式内容

### 设置与备份流程

```text
SettingsScreen
  -> SettingsViewModel
  -> UserPreferencesRepository / SettingsManager / DAO
  -> export or restore sources, articles, and settings
```

## 功能总览

### 1. 时间线阅读
- 聚合所有已订阅源文章
- 支持 `ALL`、`UNREAD`、`READ` 筛选
- 刷新时展示每个订阅源的进度
- 有新内容时可在刷新后滚动回顶部

### 2. 订阅源管理
- 新增、编辑、删除、批量删除订阅源
- 打开单个订阅源时间线
- 刷新单个订阅源
- 以 JSON 导入导出订阅源定义
- 导入时支持重复项处理策略

### 3. 发现与订阅预览
- 提供目录型与 WebView 型发现市场入口
- 支持从发现页和“订阅”页预览文章样例
- 预览后可直接保存订阅，或进入高级编辑

### 4. 文章详情阅读
- 在 WebView 中打开原文页面
- 在页面模式与正文模式间切换
- 标记文章为已读
- 收藏/取消收藏
- 记录阅读时长
- 支持浏览器打开、分享链接、复制链接、打开源设置

### 5. 自定义提取
- 支持 RSS 与自定义 HTML 源
- 支持基于选择器的内容提取
- 支持通过注入 JS 算法自动提取可读正文

### 6. 收藏
- 仅展示已收藏文章
- 按标题或摘要搜索收藏
- 批量移除收藏

### 7. 统计
- 记录已读数量与阅读时长
- 展示今日、本周、本月、全年与总计统计
- 展示最近每日趋势
- 展示按订阅源聚合的阅读统计

### 8. 设置与维护
- 自动更新开关
- 刷新并发设置
- 默认时间线筛选设置
- 缓存保留与清理设置
- 数据与设置的完整备份/恢复

### 9. 个人中心与关于
- 聚合收藏、统计、设置等维护入口
- 展示版本号、构建日期、作者和 GitHub 链接
- 提供开源致敬与调试入口

## 面向 AI 开发的工作规则

每次都遵守以下规则：

1. 在提出改动前先识别功能区域。
2. 先阅读该区域的 ViewModel、repository、DAO 和 screen 文件。
3. 保持改动最小且局部。
4. 除非用户明确要求，否则不要引入新架构。
5. 如果某项业务逻辑已有 ViewModel 或 repository 归属，不要把它搬进 composable。
6. 不要随意改变持久化行为。
7. 不要假设某个设置只存在一个存储层；同时检查 DataStore 和 SharedPreferences。

推荐的推理顺序：

```text
用户任务
  -> 识别功能区域
  -> 阅读入口文件
  -> 追踪真实调用路径
  -> 提出最小改动方案
  -> 等待确认
  -> 实施
```

## 功能分布

### 时间线与刷新

主要文件：
- `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/TimelineScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/TimelineViewModel.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/ArticleDao.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/util/SettingsManager.kt`

职责拆分：
- `MainActivity.kt`：应用级启动刷新触发与导航初始化
- `TimelineScreen.kt`：时间线 UI、进度条、筛选 chips、刷新动作
- `TimelineViewModel.kt`：筛选状态、同步状态、事件编排、单源时间线状态
- `NewsRepository.kt`：并发刷新订阅源、解析、持久化、汇总上报
- `ArticleDao.kt`：时间线查询、已读状态、收藏状态、缓存清理、统计查询
- `SettingsManager.kt`：默认筛选和刷新并发设置

### 订阅源管理与导入导出

主要文件：
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SourceManagerScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/EditSourceScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/SourceViewModel.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/EditSourceViewModel.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/SourceDao.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/ArticleDao.kt`

职责拆分：
- `SourceManagerScreen.kt`：订阅源列表与管理 UI
- `EditSourceScreen.kt`：订阅源编辑 UI
- `SourceViewModel.kt`：订阅源 CRUD、单源刷新、导入导出、重复策略处理
- `EditSourceViewModel.kt`：订阅源编辑状态与持久化
- `SourceDao.kt`：订阅源持久化与查询
- `ArticleDao.kt`：与订阅源相关的批量文章更新与清理

### 发现源与预览订阅

主要文件：
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/DiscoverScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/PlinkMarketScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/QiReaderMarketScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/FeedPreviewScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/PlinkFeedPreviewScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/DiscoverViewModel.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/FeedPreviewViewModel.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/PlinkFeedPreviewViewModel.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/core/navigation/NavRoutes.kt`

职责拆分：
- `DiscoverScreen.kt`：发现页卡片入口与市场说明
- `PlinkMarketScreen.kt`：目录型发现源市场页与预览页跳转
- `QiReaderMarketScreen.kt`：WebView 借壳发现页、登录态保持与网页内订阅拦截
- `FeedPreviewScreen.kt`：订阅页内通用预览流，支持直接保存或跳高级编辑
- `PlinkFeedPreviewScreen.kt`：发现市场预览页，承接目录型和 WebView 市场的订阅确认
- `DiscoverViewModel.kt`：目录型市场加载状态与刷新入口
- `FeedPreviewViewModel.kt`：订阅页内预览会话和返回路由
- `PlinkFeedPreviewViewModel.kt`：发现市场预览数据加载与清理
- `NavRoutes.kt`：发现页、市场页、预览页和编辑返回链路的路由定义

### 文章详情与正文提取

主要文件：
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/utils/WebViewContentExtractor.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/utils/WebViewManager.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`
- `app/src/main/assets/algorithm/loader.js`
- `app/src/main/assets/algorithm/Readability-android.js`
- `app/src/main/assets/algorithm/gne-custom.js`
- `app/src/main/assets/algorithm/main.js`

职责拆分：
- `ArticleScreen.kt`：阅读 UI、WebView 模式、菜单动作、翻译触发、阅读时长跟踪
- `WebViewContentExtractor.kt`：注入脚本并执行提取算法
- `WebViewManager.kt`：应用级 WebView 初始化支持
- `NewsRepository.kt`：订阅源抓取行为与文章存储内容
- JS 资源：真正的提取算法与页面加载器行为

### 设置、备份与恢复

主要文件：
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/SettingsViewModel.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/UserPreferencesRepository.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/util/SettingsManager.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/BackupData.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/util/BackupFileNameUtils.kt`

职责拆分：
- `SettingsScreen.kt`：设置 UI 与文件选择器集成
- `SettingsViewModel.kt`：备份生成、恢复逻辑、缓存清理、设置写入
- `UserPreferencesRepository.kt`：DataStore 持久化设置，如自动更新和缓存上限
- `SettingsManager.kt`：SharedPreferences 持久化设置，如筛选类型和刷新并发
- `BackupData.kt`：备份载荷模型
- `BackupFileNameUtils.kt`：统一订阅源导出和完整数据备份的文件名格式

### 个人中心、关于与调试

主要文件：
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ProfileScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/DebugConsoleScreen.kt`
- `app/build.gradle.kts`

职责拆分：
- `ProfileScreen.kt`：个人中心入口、关于弹窗、开源致敬与功能导航
- `DebugConsoleScreen.kt`：调试信息查看与复制
- `app/build.gradle.kts`：注入 `BUILD_DATE`，并定义 debug/release APK 输出命名

### 收藏与统计

主要文件：
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/FavoritesScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/FavoritesViewModel.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/StatsScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/StatsViewModel.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/ArticleDao.kt`

职责拆分：
- `FavoritesScreen.kt`：收藏页 UI
- `FavoritesViewModel.kt`：收藏搜索与批量移除
- `StatsScreen.kt`：统计展示 UI
- `StatsViewModel.kt`：阅读统计聚合与趋势暴露
- `ArticleDao.kt`：所有底层收藏与统计查询

## 关键入口文件

在你需要快速建立全局认知时，优先读这些文件：

- `README.md`：产品功能层面的总览
- `docs/DiscoverSourceGuide.md`：发现源与订阅市场接入手册，包含直连解析模式与 WebView 借壳模式
- `AGENTS.md`：仓库工作流与安全规则
- `app/src/main/java/com/lengyuefenghua/newsreader/NewsReaderApplication.kt`：启动入口
- `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt`：应用外壳与导航
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/TimelineViewModel.kt`：核心状态编排
- `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`：刷新与抓取核心
- `app/src/main/java/com/lengyuefenghua/newsreader/data/AppDatabase.kt`：Room 数据库与迁移行为
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/SettingsViewModel.kt`：备份与设置维护
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt`：复杂文章阅读行为
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ProfileScreen.kt`：个人中心、关于和发布信息展示

## 面向任务的入口点

### 如果你要改时间线行为
阅读：
- `MainActivity.kt`
- `TimelineScreen.kt`
- `TimelineViewModel.kt`
- `NewsRepository.kt`
- `ArticleDao.kt`

同时查看：
- `ui/common/UiEvent.kt`
- `ui/common/TimelineUiState.kt`

注意：
- 单源筛选逻辑与全局时间线逻辑共享同一个 ViewModel
- 刷新完成事件可能触发滚动或 toast 行为

### 如果你要改刷新逻辑或并发控制
阅读：
- `TimelineViewModel.kt`
- `NewsRepository.kt`
- `SettingsManager.kt`
- `RefreshPlanner.kt`
- `TwoPhaseRefreshModels.kt`

同时查看：
- `app/src/test/java/com/lengyuefenghua/newsreader/data/RefreshPlannerTest.kt`
- `app/src/test/java/com/lengyuefenghua/newsreader/data/TwoPhaseRefreshRepositoryTest.kt`

注意：
- 并发必须保持有边界
- 局部订阅源失败是有意允许的

### 如果你要改订阅源 CRUD 或导入导出
阅读：
- `SourceManagerScreen.kt`
- `EditSourceScreen.kt`
- `SourceViewModel.kt`
- `EditSourceViewModel.kt`
- `SourceDao.kt`

同时查看：
- `ArticleDao.kt`
- `FeedPreviewScreen.kt`
- `FeedPreviewViewModel.kt`

注意：
- 删除订阅源可能需要同时删除其文章
- 导入逻辑支持 skip 和 update 两种策略
- 订阅页内预览保存后通常要回到原入口，而不是发现页市场路由

### 如果你要改发现页、市场页或订阅预览
阅读：
- `docs/DiscoverSourceGuide.md`
- `DiscoverScreen.kt`
- `MainActivity.kt`
- `NavRoutes.kt`
- 对应的 `XxxMarketScreen.kt`
- `FeedPreviewScreen.kt` 或 `PlinkFeedPreviewScreen.kt`
- 对应的 `FeedPreviewViewModel.kt` 或 `PlinkFeedPreviewViewModel.kt`

注意：
- 订阅页内预览和发现市场预览是两条不同的返回链路
- 从预览页进入高级编辑后，保存时最容易漏掉回退目标

### 如果你要改文章显示或正文提取
阅读：
- `ArticleScreen.kt`
- `WebViewContentExtractor.kt`
- `WebViewManager.kt`
- `app/src/main/assets/algorithm/`

同时查看：
- `NewsRepository.kt`

注意：
- 文章模式切换同时依赖文章内容和订阅源配置
- 这里同时涉及 UI、WebView、JavaScript 注入和订阅源规则

### 如果你要改设置或备份恢复
阅读：
- `SettingsScreen.kt`
- `SettingsViewModel.kt`
- `UserPreferencesRepository.kt`
- `SettingsManager.kt`
- `BackupData.kt`
- `BackupFileNameUtils.kt`

注意：
- 设置故意分布在两套存储系统中
- 恢复逻辑是覆盖式的，并会同时影响订阅源和文章

### 如果你要改收藏或统计
阅读：
- `FavoritesViewModel.kt`
- `FavoritesScreen.kt`
- `StatsViewModel.kt`
- `StatsScreen.kt`
- `ArticleDao.kt`

注意：
- 统计依赖 `readTimestamp` 和 `readDuration`
- 收藏状态存放在 article 表上，而不是独立领域对象

## 约束与风险

以下是安全开发最重要的技术约束：

- 修改前先读目标文件
- 改代码前先给出明确方案并等待用户确认
- 除非用户要求，否则不要运行 build 或 test
- 不要假设项目里的依赖注入风格完全一致
- `AppDatabase.kt` 使用 `fallbackToDestructiveMigration()`，schema 变更可能清空本地数据
- `Article.id` 基于 URL，并参与去重语义
- `SettingsManager` 将刷新并发限制在 `1..5`
- `ArticleScreen.kt` 体量较大且职责较多，优先做最小、聚焦的改动
- 部分 ViewModel 使用 Koin 注入，部分仍从 `NewsReaderApplication` 读取依赖

高风险区域：
- `AppDatabase.kt`：schema 或版本变更可能清空用户数据
- `NewsRepository.kt`：刷新行为改动会影响应用大部分功能
- `ArticleScreen.kt`：改动会同时影响 WebView、内容提取、阅读跟踪和用户动作
- `SettingsViewModel.kt`：恢复逻辑可能覆盖本地数据

安全默认策略：
- 优先在现有职责归属层做最小改动
- 修 bug 时避免顺手做大重构
- 如果任务同时涉及 UI 与 repository 行为，先确认逻辑真正归属哪一层，再动手修改

## 测试与验证建议

按改动区域选择最小且相关的验证方式：

- 刷新规划逻辑：`app/src/test/java/com/lengyuefenghua/newsreader/data/RefreshPlannerTest.kt`
- 两阶段刷新行为：`app/src/test/java/com/lengyuefenghua/newsreader/data/TwoPhaseRefreshRepositoryTest.kt`
- 订阅预览状态：`app/src/test/java/com/lengyuefenghua/newsreader/viewmodel/FeedPreviewViewModelTest.kt`
- Repository 或解析器改动：优先运行有覆盖的定向 JVM 测试
- Compose 或导航改动：在用户明确要求时考虑 `assembleDebug` 与手动验证
- 备份恢复与 WebView 提取改动：通常需要手动验证，因为自动化覆盖有限

## 总结

大多数开发任务，都可以先从以下四个文件起步：

- `MainActivity.kt`
- `TimelineViewModel.kt`
- `NewsRepository.kt`
- `ArticleDao.kt`

它们定义了主产品循环：应用启动、刷新编排、内容持久化和文章展示。
如果是维护型任务，也应尽早阅读 `SettingsViewModel.kt` 和 `ArticleScreen.kt`，因为这两个文件包含较复杂的逻辑路径。

如果你仍然不知道从哪开始，使用这个兜底顺序：

1. 找到用户可见的功能。
2. 阅读该功能对应的 screen。
3. 阅读它的 ViewModel。
4. 阅读该 ViewModel 调用的 repository 或 DAO。
5. 然后再提出修改方案。
