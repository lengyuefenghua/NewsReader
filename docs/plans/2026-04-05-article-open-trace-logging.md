# 文章打开性能日志 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为文章打开链路新增可落盘、可通过 adb 快速读取的专用性能日志，并替换现有相关 `logcat` 调试日志。

**Architecture:** 新增一个 `ArticleOpenTraceLogger` 统一管理日志文件、session 和格式化逻辑。`NewsReaderApplication` 负责应用启动时清空日志；`ArticleScreen`、`WebViewContentExtractor`、`WebViewManager` 将现有相关日志改为写入同一个 trace 文件。

**Tech Stack:** Kotlin、Android 文件 I/O、Android WebView、JUnit4 JVM tests、Gradle。

---

### Task 1: Trace 行格式测试

**Files:**
- Create: `app/src/test/java/com/lengyuefenghua/newsreader/utils/ArticleOpenTraceLoggerTest.kt`
- Create: `app/src/main/java/com/lengyuefenghua/newsreader/utils/ArticleOpenTraceLogger.kt`

**Step 1: Write the failing test**

- 为 trace 行格式补一个测试，验证包含 `sessionId`、相对耗时、组件和阶段。
- 为相对耗时补一个测试，验证第二条事件相对首事件正确递增。

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.utils.ArticleOpenTraceLoggerTest"`

Expected: FAIL，因为日志器尚不存在。

**Step 3: Write minimal implementation**

- 实现纯 Kotlin 的 trace 行格式化逻辑。
- 提供最小会话模型，支持基准时间和阶段输出。

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.utils.ArticleOpenTraceLoggerTest"`

Expected: PASS。

### Task 2: 文件日志器接入应用启动

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/NewsReaderApplication.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/utils/ArticleOpenTraceLogger.kt`

**Step 1: Implement file logger**

- 在 `cache/article_open_trace.log` 创建固定日志文件。
- 提供 `init(context)` 和 `clear()`，应用启动时清空。

**Step 2: Keep writes simple and safe**

- 使用同步追加写入，避免把这次排查做成复杂日志框架。
- 如果文件写入失败，吞掉异常，避免影响主流程。

### Task 3: 替换文章链路日志

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/utils/WebViewContentExtractor.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/utils/WebViewManager.kt`

**Step 1: Create article session**

- 在进入文章页或开始加载文章时创建 trace session。
- 后续事件共享同一 `sessionId`。

**Step 2: Replace existing debug logs**

- 将文章打开链路相关 `Log.d/w/e` 改为 `ArticleOpenTraceLogger.log(...)`。
- 保持关键阶段完整，不扩大到无关模块。

### Task 4: 验证

**Files:**
- Modify: 无

**Step 1: Run focused unit test**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.utils.ArticleOpenTraceLoggerTest"`

**Step 2: Run app startup verification**

Run: `./run-debug.sh`

**Step 3: Prepare adb retrieval command**

Run after manual reproduction:

```bash
adb shell run-as com.lengyuefenghua.newsreader cat cache/article_open_trace.log
```
