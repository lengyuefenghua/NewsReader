# NewsReader

本地 Android RSS 阅读器，支持时间线阅读、自定义网页解析和内容抓取。

## 功能特性

- **时间线阅读** - 聚合所有订阅源文章，支持按已读/未读筛选，可配置默认筛选条件（全部/未读/已读）
- **RSS 订阅** - 支持 RSS/Atom 订阅源管理
- **自定义解析** - CSS 选择器规则配置，适配任意网页
- **动态网页支持** - WebView 方式抓取，支持反爬网站
- **文章收藏** - 收藏喜欢的文章，方便后续阅读
- **阅读统计** - 记录阅读时长和统计信息
- **智能刷新** - 增量刷新策略，实时显示每个订阅源的刷新进度和新增文章数
- **数据备份** - 完整备份订阅源、文章、设置，支持跨版本恢复
- **离线阅读** - 本地数据库存储，支持离线浏览

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构**: MVVM + Repository Pattern
- **数据库**: Room (SQLite)
- **网络**: OkHttp4, Jsoup (HTML 解析)
- **导航**: Navigation Compose
- **配置**: DataStore Preferences
- **构建**: Gradle (Kotlin DSL)

## 项目结构

```
app/src/main/java/com/lengyuefenghua/newsreader/
├── MainActivity.kt                    # 主入口
├── NewsReaderApplication.kt          # 应用程序入口
├── data/
│   ├── AppDatabase.kt                # Room 数据库
│   ├── Article.kt                    # 文章实体
│   ├── ArticleDao.kt                 # 文章数据访问
│   ├── Source.kt                     # 订阅源实体
│   ├── SourceDao.kt                  # 订阅源数据访问
│   ├── NewsRepository.kt             # 新闻仓库
│   └── UserPreferencesRepository.kt  # 用户配置仓库
├── ui/
│   ├── components/
│   │   └── ArticleCard.kt            # 文章卡片组件
│   ├── screens/
│   │   ├── TimelineScreen.kt         # 时间线页面
│   │   ├── SourceManagerScreen.kt   # 订阅源管理页面
│   │   ├── ArticleScreen.kt         # 文章详情页面
│   │   ├── EditSourceScreen.kt       # 编辑订阅源页面
│   │   ├── FavoritesScreen.kt       # 收藏页面
│   │   ├── SettingsScreen.kt        # 设置页面
│   │   ├── StatsScreen.kt           # 统计页面
│   │   ├── ProfileScreen.kt         # 个人中心页面
│   │   └── DebugConsoleScreen.kt    # 调试控制台页面
│   └── navigation/                   # 导航配置
├── viewmodel/
│   ├── TimelineViewModel.kt          # 时间线视图模型
│   ├── SourceViewModel.kt           # 订阅源视图模型
│   ├── EditSourceViewModel.kt       # 编辑订阅源视图模型
│   ├── FavoritesViewModel.kt         # 收藏视图模型
│   ├── SettingsViewModel.kt         # 设置视图模型
│   ├── StatsViewModel.kt            # 统计视图模型
│   └── ProfileViewModel.kt          # 个人中心视图模型
└── utils/
    ├── RssParser.kt                 # RSS 解析器
    ├── HtmlParser.kt                # HTML 解析器
    ├── AutoExtractor.kt             # 自动内容提取器
    ├── WebViewManager.kt           # WebView 管理
    └── DebugHelper.kt              # 调试辅助工具
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

2. 打开 Android Studio 并导入项目

3. 同步 Gradle 依赖
```bash
./gradlew build
```

4. 连接 Android 设备或启动模拟器

5. 运行应用
```bash
./gradlew installDebug
```

## 使用说明

### 添加订阅源

1. 进入"订阅"页面
2. 点击"添加订阅"按钮
3. 输入 RSS 源 URL 或配置自定义抓取规则

### 自定义解析规则

对于非标准 RSS 网站可以使用 CSS 选择器规则：

- **规则列表** - 文章列表容器的选择器
- **规则标题** - 文章标题的选择器
- **规则链接** - 文章链接的选择器
- **规则摘要** - 文章摘要的选择器
- **规则图片** - 文章图片的选择器
- **规则正文** - 文章正文的选择器

### 阅读统计

应用会自动记录：
- 阅读时长
- 阅读的文章数量
- 收藏的文章数量

## 依赖项

主要依赖版本：

- `androidx.core:core-ktx` - Android KTX 扩展
- `androidx.lifecycle:*` - 生命周期组件
- `androidx.compose:*` - Jetpack Compose UI
- `androidx.navigation:navigation-compose` - 导航
- `androidx.room:*` - Room 数据库
- `androidx.datastore:datastore-preferences` - DataStore 配置
- `com.squareup.okhttp3:okhttp:4.12.0` - HTTP 客户端
- `org.jsoup:jsoup:1.17.2` - HTML 解析
- `com.google.code.gson:gson:2.10.1` - JSON 解析

## 许可证

[License]

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

- 作者: lengyuefenghua
- 项目链接: [GitHub Repository]
