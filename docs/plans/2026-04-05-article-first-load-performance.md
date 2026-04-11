# 文章首开性能优化 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 降低文章详情页首次打开，尤其是正文模式下的等待时间，同时保持现有阅读行为不变。

**Architecture:** 通过两处最小改动覆盖首开慢的主要来源。`WebViewContentExtractor` 增加进程内脚本缓存，避免重复读取 `assets`；`WebViewManager` 在应用启动时补一次轻量引擎预热，让首次详情页 WebView 更接近热启动状态。

**Tech Stack:** Kotlin、Android WebView、JUnit4 JVM tests、Gradle。

---

### Task 1: 脚本缓存测试

**Files:**
- Create: `app/src/test/java/com/lengyuefenghua/newsreader/utils/WebViewScriptCacheTest.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/utils/WebViewContentExtractor.kt`

**Step 1: Write the failing test**

- 为脚本缓存补一个“同一路径只加载一次”的用例。
- 为脚本缓存补一个“不同路径分别缓存”的用例。

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.utils.WebViewScriptCacheTest"`

Expected: FAIL，因为缓存实现尚不存在。

**Step 3: Write minimal implementation**

- 在 `WebViewContentExtractor.kt` 内新增可复用的脚本缓存对象。
- 将脚本读取改为“先查缓存，未命中再加载并写入缓存”。

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.utils.WebViewScriptCacheTest"`

Expected: PASS。

### Task 2: 轻量 WebView 预热

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/utils/WebViewManager.kt`

**Step 1: Add minimal implementation**

- 在初始化后台 `WebView` 后执行一次轻量预热，不改变业务抓取流程。
- 预热应避免网络请求，仅触发 WebView/JS 引擎启动。

**Step 2: Keep behavior isolated**

- 预热逻辑只在初始化阶段运行。
- 不影响 `fetchHtml()` 互斥、超时和清理行为。

### Task 3: 验证

**Files:**
- Modify: 无

**Step 1: Run focused test**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.utils.WebViewScriptCacheTest"`

**Step 2: Run app install and launch verification**

Run: `./run-debug.sh`

**Step 3: Manual check**

- 首次打开一篇启用自动提取的文章。
- 观察首次加载遮罩消失时间是否比之前更短。
- 再打开第二篇文章，确认无功能回归。
