# 订阅源搜索预览过渡阶段实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在订阅源管理的新增流程中加入“搜索订阅源并预览文章，再决定是否正式订阅”的过渡阶段。

**Architecture:** 保持现有订阅源管理、自定义配置和文章详情结构不变，在新增订阅入口前插入一个仅驻留内存的预览会话。搜索阶段通过仓库层抓取标题与文章列表，临时文章列表页和预填充添加弹窗都读取这份预览状态，最终只有点击保存时才写入数据库。

**Tech Stack:** Kotlin、Jetpack Compose、Navigation Compose、Android ViewModel、Room、现有 NewsRepository / RssParser。

---

## 文件范围

**Modify**
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SourceManagerScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/core/navigation/NavRoutes.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/utils/RssParser.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/EditSourceScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/EditSourceViewModel.kt`

**Create**
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/FeedPreviewScreen.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/FeedPreviewViewModel.kt`
- `app/src/test/java/com/lengyuefenghua/newsreader/viewmodel/FeedPreviewViewModelTest.kt`

## 实施任务

### Task 1: 预览会话与测试

**Files:**
- Create: `app/src/test/java/com/lengyuefenghua/newsreader/viewmodel/FeedPreviewViewModelTest.kt`
- Create: `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/FeedPreviewViewModel.kt`

- [ ] 先写失败测试，覆盖标题回退、预览结果保存与清空行为。
- [ ] 运行对应 JVM 测试，确认先失败。
- [ ] 最小实现 `FeedPreviewViewModel` 与预览数据模型。
- [ ] 再次运行测试，确认通过。

### Task 2: 仓库层搜索预览能力

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/utils/RssParser.kt`

- [ ] 扩展 RSS 解析结果，支持返回订阅源标题。
- [ ] 新增仓库层“搜索预览”方法，返回标题、图标和文章列表。
- [ ] 保持现有同步刷新逻辑行为不变。

### Task 3: 添加弹窗双模式

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SourceManagerScreen.kt`

- [ ] 将现有添加弹窗参数化为“搜索模式”和“保存模式”。
- [ ] 搜索模式只显示地址，按钮为“搜索”，保留导入与自定义。
- [ ] 保存模式显示名称与地址，按钮为“保存”，隐藏导入并支持预填充。

### Task 4: 临时文章列表页与导航

**Files:**
- Create: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/FeedPreviewScreen.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/core/navigation/NavRoutes.kt`

- [ ] 新增预览路由。
- [ ] 新建临时文章列表页，显示订阅源标题、地址、文章数量和文章列表。
- [ ] 接入文章详情跳转。
- [ ] 左上角返回订阅源管理，右上角触发预填充添加弹窗。

### Task 5: 预填充添加与自定义页透传

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SourceManagerScreen.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/EditSourceScreen.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/EditSourceViewModel.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt`

- [ ] 点击“添加订阅”弹出预填充保存模式弹窗。
- [ ] 直接保存时执行去重检查，成功后回到订阅源管理。
- [ ] 点击自定义时将标题与地址带入 `EditSourceScreen`。
- [ ] 自定义页保存后回到订阅源管理。

### Task 6: 验证

**Files:**
- Verify only

- [ ] 运行新增/修改的 JVM 测试。
- [ ] 运行 `./run-debug.sh` 做最终检查。
- [ ] 记录自动验证结果和需要人工确认的交互路径。
