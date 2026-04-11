# 详情页上下拉连续阅读 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为文章详情页增加顶部下拉切上一篇、底部上拉切下一篇的连续阅读能力，并显示左右贴边、接近半圆的弧形提示区域。

**Architecture:** 先补齐“详情页阅读上下文”，让时间线、单源列表和收藏页进入详情时都能携带当前列表顺序与当前位置。再在 `ArticleScreen` 外层增加边界拖拽状态机和弧形提示层，仅在 `WebView` 已到顶部或底部时接管拖拽，并在达阈值释放后切换到上一篇或下一篇。

**Tech Stack:** Kotlin、Jetpack Compose Material3、Navigation Compose、Android WebView、JUnit4 JVM tests。

---

### Task 1: 提取可测试的阅读上下文与相邻文章计算逻辑

**Files:**
- Create: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleReadingContext.kt`
- Create: `app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/ArticleReadingContextTest.kt`

**Step 1: Write the failing test**

```kotlin
@Test
fun `current article in middle should resolve previous and next ids`() {
    val context = ArticleReadingContext(
        articleUrls = listOf("u1", "u2", "u3"),
        currentUrl = "u2",
    )

    assertEquals("u1", context.previousUrl)
    assertEquals("u3", context.nextUrl)
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleReadingContextTest"`
Expected: FAIL，因为 `ArticleReadingContext` 尚不存在。

**Step 3: Write minimal implementation**

```kotlin
data class ArticleReadingContext(
    val articleUrls: List<String>,
    val currentUrl: String,
) {
    private val currentIndex = articleUrls.indexOf(currentUrl)

    val previousUrl: String?
        get() = articleUrls.getOrNull(currentIndex - 1)

    val nextUrl: String?
        get() = articleUrls.getOrNull(currentIndex + 1)
}
```

**Step 4: Expand tests for edge cases**

```kotlin
@Test
fun `first article should have no previous`() { /* ... */ }

@Test
fun `last article should have no next`() { /* ... */ }

@Test
fun `missing current article should expose no neighbors`() { /* ... */ }
```

**Step 5: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleReadingContextTest"`
Expected: PASS

**Step 6: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleReadingContext.kt app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/ArticleReadingContextTest.kt
git commit -m "feat: add article reading context model"
```

### Task 2: 升级导航参数，支持详情页携带阅读上下文

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/core/navigation/NavRoutes.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt:183-208`
- Test: `app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/ArticleReadingContextTest.kt`

**Step 1: Write the failing test for route encoding helper**

```kotlin
@Test
fun `route builder should keep current url and reading list payload`() {
    val route = NavRoutes.article(
        url = "https://example.com/a",
        readingList = listOf("https://example.com/a", "https://example.com/b"),
    )

    assertTrue(route.contains("article/"))
    assertTrue(route.contains("readingList="))
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleReadingContextTest"`
Expected: FAIL，因为 `NavRoutes.article` 还不支持阅读上下文参数。

**Step 3: Write minimal implementation**

```kotlin
const val ARTICLE_LIST_ARG = "readingList"

fun article(url: String, readingList: List<String>): String {
    val encodedUrl = encode(url)
    val encodedList = encode(readingList.joinToString("\n"))
    return "article/$encodedUrl?readingList=$encodedList"
}
```

**Step 4: Wire `MainActivity` to decode route payload**

```kotlin
val readingListPayload = backStackEntry.arguments?.getString(NavRoutes.ARTICLE_LIST_ARG).orEmpty()
val readingList = decodeReadingList(readingListPayload)
val readingContext = ArticleReadingContext(readingList, url)
```

**Step 5: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleReadingContextTest"`
Expected: PASS

**Step 6: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/core/navigation/NavRoutes.kt app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/ArticleReadingContextTest.kt
git commit -m "feat: pass article reading context through navigation"
```

### Task 3: 让列表入口统一传递阅读上下文

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/TimelineScreen.kt:51-205`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/FavoritesScreen.kt:49-199`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt:145-176`

**Step 1: Refactor callback signatures to carry current list**

```kotlin
onArticleClick: (currentUrl: String, readingList: List<String>) -> Unit
```

**Step 2: Build the list from visible data source**

```kotlin
val readingList = articles.map { it.url }
ArticleCard(
    article = article,
    onClick = { onArticleClick(article.url, readingList) }
)
```

**Step 3: Update `MainActivity` callers**

```kotlin
onArticleClick = { url, readingList ->
    navController.navigate(NavRoutes.article(url, readingList))
}
```

**Step 4: Smoke-check compile-sensitive sites**

Inspect every `TimelineScreen(` and `FavoritesScreen(` call site to ensure callback signature is updated consistently.

**Step 5: Run unit tests to verify no regression in touched helpers**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleReadingContextTest" --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleScreenModeTest"`
Expected: PASS

**Step 6: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/TimelineScreen.kt app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/FavoritesScreen.kt app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt
git commit -m "feat: pass reading order from article lists"
```

### Task 4: 提取可测试的边界状态与拖拽阈值逻辑

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt`
- Test: `app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreenPullNavigationTest.kt`

**Step 1: Write failing tests for pure helper functions**

```kotlin
@Test
fun `top edge should become ready when drag exceeds threshold`() {
    val state = evaluatePullNavigationState(
        edge = PullEdge.Top,
        dragOffset = 140f,
        triggerThreshold = 120f,
        hasTarget = true,
    )

    assertEquals(PullNavigationState.ReadyToNavigatePrevious, state)
}
```

**Step 2: Add more tests for bottom edge and empty target**

```kotlin
@Test
fun `bottom edge without next article should stay in boundary state`() { /* ... */ }

@Test
fun `drag below threshold should stay pulling`() { /* ... */ }
```

**Step 3: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleScreenPullNavigationTest"`
Expected: FAIL，因为状态枚举和 helper 尚不存在。

**Step 4: Write minimal implementation in `ArticleScreen.kt` or nearby file**

```kotlin
internal enum class PullEdge { Top, Bottom }

internal enum class PullNavigationState {
    Idle,
    PullingPrevious,
    PullingNext,
    ReadyToNavigatePrevious,
    ReadyToNavigateNext,
    BoundaryOnly,
}
```

**Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleScreenPullNavigationTest"`
Expected: PASS

**Step 6: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreenPullNavigationTest.kt
git commit -m "test: add pull navigation state helpers"
```

### Task 5: 在详情页接入阅读上下文与相邻文章解析

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt:183-208`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt:83-220`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/TimelineViewModel.kt:174-223`

**Step 1: Thread `ArticleReadingContext` into `ArticleScreen`**

```kotlin
ArticleScreen(
    article = article,
    readingContext = readingContext,
    onOpenArticle = { targetUrl -> navController.navigate(...) },
    ...
)
```

**Step 2: Resolve previous and next article flows from URLs**

```kotlin
val previousArticle by timelineViewModel.getArticleFlow(previousUrl).collectAsState(initial = ...)
val nextArticle by timelineViewModel.getArticleFlow(nextUrl).collectAsState(initial = ...)
```

**Step 3: Keep fallback behavior safe**

If `readingContext` is empty or the current URL is missing from the list, disable pull navigation UI and preserve existing article reading behavior.

**Step 4: Run focused unit tests**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleReadingContextTest" --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleScreenModeTest" --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleScreenPullNavigationTest"`
Expected: PASS

**Step 5: Manual compile sanity check**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/TimelineViewModel.kt
git commit -m "feat: resolve adjacent articles in detail screen"
```

### Task 6: 实现顶部/底部边界拖拽与释放切换

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt:83-638`
- Test: `app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreenPullNavigationTest.kt`

**Step 1: Add boundary detection plumbing for WebView**

```kotlin
var isAtTop by remember { mutableStateOf(true) }
var isAtBottom by remember { mutableStateOf(false) }

fun updateWebViewBoundary(webView: WebView) {
    isAtTop = webView.scrollY == 0
    isAtBottom = webView.contentHeight * webView.scale - webView.height - webView.scrollY <= 1f
}
```

**Step 2: Add drag state and release threshold**

```kotlin
var dragOffset by remember { mutableFloatStateOf(0f) }
val triggerThreshold = 120.dp
```

**Step 3: Wrap the content area with vertical drag handling**

```kotlin
Modifier.pointerInput(isAtTop, isAtBottom) {
    detectVerticalDragGestures(
        onVerticalDrag = { _, dragAmount -> /* edge-aware consume */ },
        onDragEnd = { /* navigate or rebound */ },
    )
}
```

**Step 4: Navigate on release only when ready**

```kotlin
if (pullState == PullNavigationState.ReadyToNavigatePrevious) {
    onOpenArticle(previousArticle.url)
}
```

**Step 5: Run targeted tests**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleScreenPullNavigationTest"`
Expected: PASS

**Step 6: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreenPullNavigationTest.kt
git commit -m "feat: add edge pull navigation gestures"
```

### Task 7: 绘制贴边半圆弧提示区并接入文案状态

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt:83-638`
- Test: `app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreenPullNavigationTest.kt`

**Step 1: Extract preview text selection helpers for testability**

```kotlin
internal fun topPreviewText(previousTitle: String?): String =
    previousTitle ?: "已经是第一条"

internal fun bottomPreviewText(nextTitle: String?): String =
    nextTitle ?: "已经是最后一条"
```

**Step 2: Add tests for preview text and release labels**

```kotlin
@Test
fun `missing previous article should show first boundary text`() { /* ... */ }
```

**Step 3: Draw arc-shaped overlays**

```kotlin
Canvas(modifier = Modifier.fillMaxWidth()) {
    val path = Path().apply {
        // top / bottom half-circle-like edge path
    }
    drawPath(path = path, color = backgroundColor)
}
```

**Step 4: Layer title and action text on top of the arc**

```kotlin
Text(text = previewTitle, maxLines = 1)
Text(text = releaseHint)
```

**Step 5: Run tests**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleScreenPullNavigationTest"`
Expected: PASS

**Step 6: Commit**

```bash
git add app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreenPullNavigationTest.kt
git commit -m "feat: add pull navigation arc overlays"
```

### Task 8: 完整验证并执行仓库要求的最终检查

**Files:**
- Verify only

**Step 1: Run focused unit tests**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleReadingContextTest" --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleScreenModeTest" --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleScreenPullNavigationTest"`
Expected: PASS

**Step 2: Run debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Run repository final verification script**

Run: `bash ./run-debug.sh`
Expected: debug 安装与启动流程完成；如无设备，至少应给出明确失败原因。

**Step 4: Manual validation checklist**

1. 从时间线进入任意中间文章，顶部下拉能预览上一篇标题并在松手后切换
2. 底部上拉能预览下一篇标题并在松手后切换
3. 第一条顶部下拉显示“已经是第一条”并回弹
4. 最后一条底部上拉显示“已经是最后一条”并回弹
5. 收藏页入口连续阅读顺序正确
6. 单订阅源入口连续阅读顺序正确

**Step 5: Commit**

```bash
git add .
git commit -m "feat: add pull navigation between article details"
```
