# NewsReader

`NewsReader` 是一个本地优先的 Android RSS 阅读器，支持时间线阅读、自定义网页解析与正文提取。

## 核心特性

- **时间线阅读**：聚合所有订阅源文章，支持全部/未读/已读筛选，未读筛选带数量角标，并可配置默认筛选项
- **RSS 订阅**：支持手动添加、编辑、导入导出、重复项覆盖更新
- **发现订阅源**：内置 Plink、Awesome RSSHub Routes、Top RSS List、Wechat2RSS 和 QiReader 等订阅市场入口
- **订阅预览**：支持在保存前预览文章样例，再直接订阅或跳转到高级编辑
- **自定义解析**：支持配置 CSS 选择器规则，以适配任意网页
- **动态网页支持**：可通过 WebView 抓取动态页面，适配部分反爬站点
- **文章收藏**：收藏感兴趣的文章，便于后续阅读
- **阅读统计**：记录阅读时长、阅读量和趋势数据
- **智能刷新**：采用增量刷新策略，并展示每个订阅源的刷新进度与新增文章数
- **数据备份**：可完整备份订阅源、文章和设置，并支持恢复
- **发布信息展示**：关于页可查看版本号、构建日期、作者和 GitHub 链接
- **离线阅读**：依赖本地数据库存储，支持离线浏览

## 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **架构**：MVVM + Repository Pattern
- **数据库**：Room (SQLite)
- **网络与解析**：OkHttp4、Jsoup
- **导航**：Navigation Compose
- **配置存储**：DataStore Preferences + SharedPreferences
- **构建系统**：Gradle (Kotlin DSL)

## 项目结构

```text
app/src/main/java/com/lengyuefenghua/newsreader/
├── MainActivity.kt                    # 主入口
├── NewsReaderApplication.kt           # Application 入口
├── core/
│   ├── di/                            # Koin 模块
│   └── navigation/                    # 路由定义
├── data/
│   ├── AppDatabase.kt                 # Room 数据库
│   ├── Article.kt                     # 文章实体
│   ├── ArticleDao.kt                  # 文章数据访问
│   ├── Source.kt                      # 订阅源实体
│   ├── SourceDao.kt                   # 订阅源数据访问
│   ├── NewsRepository.kt              # 核心仓库
│   └── UserPreferencesRepository.kt   # 用户偏好仓库
├── ui/
│   ├── components/
│   │   └── ArticleCard.kt             # 文章卡片组件
│   ├── screens/
│   │   ├── TimelineScreen.kt          # 时间线页面
│   │   ├── SourceManagerScreen.kt     # 订阅源管理页面
│   │   ├── DiscoverScreen.kt          # 发现页入口
│   │   ├── FeedPreviewScreen.kt       # 通用订阅预览页
│   │   ├── PlinkMarketScreen.kt       # 目录型发现源市场页
│   │   ├── PlinkFeedPreviewScreen.kt  # 发现源预览页
│   │   ├── QiReaderMarketScreen.kt    # WebView 发现源市场页
│   │   ├── ArticleScreen.kt           # 文章详情页面
│   │   ├── EditSourceScreen.kt        # 编辑订阅源页面
│   │   ├── FavoritesScreen.kt         # 收藏页面
│   │   ├── ProfileScreen.kt           # 个人中心与关于页面
│   │   ├── SettingsScreen.kt          # 设置页面
│   │   ├── StatsScreen.kt             # 统计页面
│   │   └── DebugConsoleScreen.kt      # 调试控制台页面
├── util/
│   ├── BackupFileNameUtils.kt         # 备份文件名规范
│   └── SettingsManager.kt             # 本地设置管理
├── utils/
│   ├── RssParser.kt                   # RSS 解析器
│   ├── HtmlParser.kt                  # HTML 解析器
│   ├── AutoExtractor.kt               # 自动内容提取器
│   ├── WebViewManager.kt              # WebView 管理
│   └── DebugHelper.kt                 # 调试辅助工具
├── viewmodel/
│   ├── TimelineViewModel.kt           # 时间线 ViewModel
│   ├── SourceViewModel.kt             # 订阅源 ViewModel
│   ├── DiscoverViewModel.kt           # 发现源 ViewModel
│   ├── FeedPreviewViewModel.kt        # 通用预览 ViewModel
│   ├── PlinkFeedPreviewViewModel.kt   # 发现源预览 ViewModel
│   ├── EditSourceViewModel.kt         # 编辑订阅源 ViewModel
│   ├── FavoritesViewModel.kt          # 收藏 ViewModel
│   ├── SettingsViewModel.kt           # 设置 ViewModel
│   ├── StatsViewModel.kt              # 统计 ViewModel
│   └── ProfileViewModel.kt            # 个人中心 ViewModel
```

## 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11
- Android SDK 28 (Min) - 36 (Target)
- Gradle 8.0+

### 构建步骤

1. 克隆项目

```bash
git clone <repository-url>
cd NewsReader
```

2. 用 Android Studio 打开并导入项目

3. 同步 Gradle 依赖并验证构建

```bash
./gradlew build
```

4. 连接 Android 设备或启动模拟器

5. 一键安装并启动应用（推荐）

```bash
./run-debug.sh
```

说明：需要确保 `adb` 在 `PATH` 中。脚本会执行 `installDebug -> am force-stop -> monkey` 来完成安装和启动。

如只需安装 APK，可执行：

```bash
./gradlew installDebug
```

## 使用说明

### 添加订阅源

1. 进入“订阅”页面
2. 点击“添加订阅”按钮
3. 输入 RSS 源 URL，或配置自定义抓取规则

### 发现并预览订阅源

1. 进入“发现”页面
2. 选择目录型市场或 QiReader WebView 市场
3. 打开预览页查看文章样例
4. 直接保存订阅，或跳到“高级编辑”补充配置

### 自定义解析规则

对于非标准 RSS 网站，可以配置 CSS 选择器规则：

- **规则列表**：文章列表容器选择器
- **规则标题**：文章标题选择器
- **规则链接**：文章链接选择器
- **规则摘要**：文章摘要选择器
- **规则图片**：文章图片选择器
- **规则正文**：文章正文选择器

### 阅读统计

应用会自动记录：
- 阅读时长
- 阅读文章数量
- 收藏文章数量

## 主要依赖

- `androidx.core:core-ktx`：Android KTX 扩展
- `androidx.lifecycle:*`：生命周期组件
- `androidx.compose:*`：Jetpack Compose UI
- `androidx.navigation:navigation-compose`：导航
- `androidx.room:*`：Room 数据库
- `androidx.datastore:datastore-preferences`：DataStore 配置
- `com.squareup.okhttp3:okhttp:4.12.0`：HTTP 客户端
- `org.jsoup:jsoup:1.17.2`：HTML 解析
- `com.google.code.gson:gson:2.10.1`：JSON 解析

## 贡献

欢迎提交 Issue 和 Pull Request。

## 联系方式

- 作者：lengyuefenghua
- 项目链接：https://github.com/lengyuefenghua/NewsReader
