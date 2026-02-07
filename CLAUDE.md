# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码库中工作时提供指导。

---

# ⚠️⚠️⚠️ 重要：工作流程规范 ⚠️⚠️⚠️

## 🚨 禁止行为

**在未经用户明确确认前，绝对禁止执行以下操作：**

- ❌ 修改任何代码文件（使用 Edit、Write 工具）
- ❌ 修改配置文件（build.gradle, settings.json 等）
- ❌ 添加/删除/升级依赖库
- ❌ 修改数据库 schema
- ❌ 执行 Git 操作（commit, push, merge）

## 📋 强制检查清单

**在执行任何代码修改前，必须回答以下问题：**

```
□ 我是否先读取了文件内容？
□ 我是否向用户说明了修改方案？
□ 我是否使用了 AskUserQuestion 工具？
□ 用户是否明确选择了"确认执行"？
□ 如果用户选择"其他"，我是否重新确认了？
```

**如果任何一个问题的答案是"否"，立即停止，先完成确认流程！**

## ✅ 无需确认的操作

- ✅ 读取文件（Read 工具）
- ✅ 搜索代码（Grep, Glob 工具）
- ✅ 查看项目结构
- ✅ 编译/安装（用户明确要求时）
- ✅ 运行测试（用户明确要求时）
- ✅ 回答技术问题

## 🔄 标准工作流程

```
用户请求 → 分析需求 → 设计方案 → AskUserQuestion 确认 → 执行修改
```

**方案设计必须包含：** 文件路径、修改内容、原因、预期效果、影响范围、风险评估

**用户确认：** 使用 `AskUserQuestion` 工具，提供 2-4 个选项（必须包含"其他"），未经明确确认不得执行修改

## 💡 思维模式

**错误思维：** "这只是小修改，应该没问题" | "用户会理解的" | "我先把代码改好再说"

**正确思维：** "这是用户的代码，我不应该擅自修改" | "即使用户可能同意，我也要先询问" | "用户确认前的任何修改都是违规"

**在每次准备修改代码时，在心里默问一遍：** > "用户明确同意这个修改了吗？"

---

## 项目概述

**NewsReader** - Kotlin + Jetpack Compose 构建的 Android RSS 阅读器，支持 RSS/Atom 订阅、CSS 选择器自定义解析、本地 SQLite 离线阅读、自动正文提取（GNE/Readability）。

**架构：** MVVM + Repository | **UI：** Compose (Material 3) | **数据库：** Room v10 + KSP 2.0.21-1.0.28 | **异步：** Coroutines + Flow | **构建：** Gradle 8.13.2 + Kotlin 2.0.21 | **依赖注入：** Koin 3.5.6

**当前版本：** v0.0.2 (versionCode: 2)

## 常用命令

```bash
./gradlew build                    # 构建所有变体
./gradlew assembleDebug            # 构建 Debug APK
./gradlew installDebug             # 安装并验证（必须）
adb shell am start -n com.lengyuefenghua.newsreader/.MainActivity  # 安装后启动主页面（必须）
./gradlew test                     # 单元测试
./gradlew connectedAndroidTest      # 设备测试
```

**依赖管理：** 核心版本在 `gradle/libs.versions.toml`，添加新依赖时优先检查版本目录

**构建配置：**
- **签名配置：** Debug 和 Release 均使用 debug keystore 签名（便于开发测试）
- **代码混淆：** Release 启用 R8 混淆和资源压缩（`isMinifyEnabled = true`）
- **APK 输出：** 自定义输出文件名为 `NewsReader.apk`
- **Lint 检查：** 已禁用以避免构建问题（`abortOnError = false`）

**MCP 集成：** Android MCP（ADB、截图、UI 布局）、Context7（文档）、Web Reader、4.5v MCP（图像）

## 架构模式

**分层结构：** UI → ViewModel (StateFlow + Channel) → Repository (数据单一来源) → Data (Room + Network)

**核心决策：**
- **状态管理：** ViewModels 使用 `_state` + `state` 模式，`Channel` 处理一次性事件
- **Repository：** `NewsRepository` 处理所有数据操作，数据库是单一数据源，所有 DB 操作使用 `Dispatchers.IO`
- **导航：**
  - **底部 3 标签页：** Timeline（时间线）、Sources（订阅）、Profile（我的）
  - **URL 参数编码：** 使用 `URLEncoder.encode(url, "UTF-8")` 编码导航参数
  - **单源查看模式：** Timeline 支持点击订阅源图标进入单源查看模式
  - **状态保存：** 使用 `popUpTo` + `saveState = true` 保存标签页状态
- **内容解析：** RSS/Atom 或 CSS 选择器自定义解析，WebView 集成支持动态内容
- **自动正文提取：** GNE（文本密度）、Readability（Mozilla 算法）、CSS 选择器

## 关键文件

**数据层：**
- `data/AppDatabase.kt` - 数据库配置（v10，fallbackToDestructiveMigration）
- `data/NewsRepository.kt` - 数据操作单一来源
- `data/Source.kt` - 订阅源实体（extractionAlgorithm + @SerializedName）

**工具类：**
- `utils/RssParser.kt` - RSS/Atom 解析器（支持标准 RSS 格式）
- `utils/HtmlParser.kt` - HTML 解析器（使用 Jsoup + CSS 选择器）
- `utils/WebViewContentExtractor.kt` - WebView 正文提取（PageLoader + WebExtractor 模式）
- `utils/WebViewManager.kt` - WebView 管理（处理加载超时、WAF 检测）
- `utils/DebugHelper.kt` - 调试辅助工具（生成结构化调试日志）
- `utils/LogUtils.kt` - 统一日志工具
- `utils/AutoExtractor.kt` - 自动内容提取器接口（当前为占位实现）

**JavaScript 算法资源（algorithm 目录）：**
- `app/src/main/assets/algorithm/loader.js` - PageLoader 脚本加载器
- `app/src/main/assets/algorithm/main.js` - WebExtractor 统一提取接口
- `app/src/main/assets/algorithm/Readability-android.js` - Mozilla Readability 算法
- `app/src/main/assets/algorithm/gne-custom.js` - GNE 文本密度算法
- `app/src/main/assets/immersive_translate.js` - 沉浸式翻译（根目录）

**UI 通用组件：**
- `ui/common/UiEvent.kt` - 一次性事件封装（Channel 模式）
- `ui/common/TimelineUiState.kt` - Timeline UI 状态
- `ui/common/FilterType.kt` - 过滤器类型（全部/未读/已读）
- `ui/common/SyncState.kt` - 同步状态管理
- `ui/screens/` - 各页面 Compose 组件（Timeline, SourceManager, Article, Favorites 等）

**导航：**
- `MainActivity.kt` - 底部 3 标签页导航（Timeline、订阅、Profile）
- `core/navigation/NavRoutes.kt` - 导航路由常量定义

**Skills：**
- `.claude/skills/changelog-logger/` - Changelog 维护
- `.claude/skills/requirements-analyst/` - 需求澄清

## 核心实现细节

**Room 数据库：**
- **版本：** 10，`Article`（文章）和 `Source`（订阅源）实体
- **DAOs：** 返回 `Flow` 或 `suspend` 函数，支持响应式数据流
- **迁移策略：** 当前使用破坏性迁移（`fallbackToDestructiveMigration()`），**版本升级会清空所有数据**
- **Article 实体：** 包含阅读时长统计（`readDuration`、`readTimestamp`）
- **Source 实体：** 支持多种解析模式（RSS、自定义 CSS、自动提取算法）

**自动正文提取架构：**
- **触发条件：** `useAutoExtract = true` 时启用，`extractionAlgorithm` 决定算法（gne/readability-android/custom）
- **PageLoader 模式：** 在 `onPageStarted` 注入 `loader.js`，在 `onPageFinished` 启动监听，收到 "ready" 回调后注入算法脚本
- **WebExtractor 接口：** 统一的提取接口，支持 Readability、GNE、自定义 CSS 选择器
- **WebView 集成：** 使用 `addJavascriptInterface()` 回调，白色遮罩层防止闪现，`hasShownMask` 标志避免重复显示
- **算法目录：** 所有算法脚本位于 `app/src/main/assets/algorithm/`，通过 `loader.js` 动态加载

**沉浸式翻译：** 文章详情页支持双语对照阅读，通过 WebView 注入 `immersive_translate.js` 实现。

**Coroutines & Flow：**
```kotlin
viewModelScope.launch {
    withContext(Dispatchers.IO) { repository.operation() }
}
combine(flow1, flow2) { data1, data2 -> }
    .stateIn(viewModelScope, SharingStarted.Lazily, initialValue)
```

**网络与解析：** OkHttp 4.12.0 + Jsoup 1.17.2 + Gson 2.10.1 + Coil 2.6.0。`try-catch` 处理，优先用 `?.` 和 `?:` 而非 `!!`

**依赖注入：** 使用 Koin 3.5.6 进行依赖注入，在 `NewsReaderApplication` 中初始化

## 常见问题

1. **主线程数据库操作：** Room 务必使用 `Dispatchers.IO`
2. **URL 编码：** 导航参数需要 `URLEncoder.encode(url, "UTF-8")`
3. **状态提升：** 状态保存在 ViewModels
4. **KSP 版本匹配：** Room 需要 KSP 插件，版本格式为 `Kotlin版本-补丁版本`（当前：2.0.21-1.0.28），版本必须严格匹配
5. **数据库升级：** 当前使用破坏性迁移（`fallbackToDestructiveMigration()`），升级版本号会清空所有数据
6. **数据备份恢复（v0.0.2）：** 在设置页面可完整备份订阅源、文章、用户设置，支持跨版本恢复
7. **正文提取性能（v0.0.2）：** 跳过 PageLoader 等待，直接提取，加载时间从 1500ms 降至 200-400ms
8. **资源拦截（v0.0.2）：** 自动拦截广告、字体、视频，保留正文图片
9. **页面闪现（v0.0.2）：** 白色遮罩层 + 延迟 300ms 隐藏，完全无闪现
10. **ProGuard 混淆：** Release 模式下使用 R8 混淆，已配置 Gson 规则保护 `Source` 类字段名

## 测试与验证

**单元测试：** `app/src/test/`（业务逻辑、解析器）| **设备测试：** `app/src/androidTest/`（UI、数据库）

**Changelog 维护：** 用户说"测试通过"时使用 `changelog-logger` skill 更新 changelog.md 和 CLAUDE.md，版本号遵循语义化版本（Major.Minor.Patch）。

## 版本信息

**SDK：** compileSdk 36，minSdk 28，targetSdk 36 | **版本：** versionCode 16，versionName "1.3.4" | **Application ID：** com.lengyuefenghua.newsreader

## 相关文档

- `README.md` - 项目概述、功能特性、快速入门
- `changelog.md` - 版本历史和变更详情
- `REFACTORING_SUMMARY.md` - 重构总结和架构演进记录
