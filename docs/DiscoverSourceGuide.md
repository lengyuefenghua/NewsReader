# 发现源接入手册

## 目的

这份手册用于指导后续新增“发现”页里的订阅市场插件，也就是把一个公开订阅目录站点接入到应用内，让用户可以：

1. 在“发现”页看到入口卡片。
2. 打开市场页浏览分组后的订阅源。
3. 点击任意订阅源进入预览页。
4. 从预览页直接订阅，或跳转到高级编辑页继续配置。

这里说的“发现源”不是普通手动添加的 RSS 源，而是一个“订阅源目录”。它通常提供很多可直接订阅的公开 RSS/Atom 链接。

## 现有实现概览

当前仓库已经接入过以下发现源，可作为参考样例：

- `Plink`
- `Awesome RSSHub Routes`
- `Top RSS List`
- `Wechat2RSS`

核心调用链如下：

```text
DiscoverScreen
  -> MainActivity / NavRoutes
  -> XxxMarketScreen
  -> DiscoverViewModel.loadXxxFeeds()
  -> NewsRepository.fetchXxxFeedGroups()
  -> parseXxxFeedGroups()
  -> DiscoverUiState.groups
  -> FeedCatalogMarketScreen
  -> PlinkFeedPreviewScreen
  -> SourceViewModel / EditSourceScreen
```

## 相关文件职责

- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/DiscoverScreen.kt`
  - 发现页卡片入口。
- `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt`
  - 发现页点击回调、市场页导航、预览页路由、编辑后返回路径。
- `app/src/main/java/com/lengyuefenghua/newsreader/core/navigation/NavRoutes.kt`
  - 市场页和预览页的路由定义。
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/DiscoverViewModel.kt`
  - 各发现源的加载状态与刷新入口。
- `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`
  - 远程页面抓取、目录解析、统一错误处理。
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/PlinkMarketScreen.kt`
  - 市场页包装函数与通用 `FeedCatalogMarketScreen`。
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/PlinkFeedPreviewScreen.kt`
  - 订阅预览页。当前多个发现源都复用它。

## 接入前先判断什么

在动手前，先确认目标站点属于哪一类：

1. OPML / XML 目录
   - 适合像 `Awesome RSSHub Routes` 一样用 `Parser.xmlParser()` 解析。
2. Markdown 文本目录
   - 适合像 `Top RSS List` 一样按行扫描标题、表格和链接。
3. 静态 HTML 页面
   - 适合像 `Plink`、`Wechat2RSS` 一样用 Jsoup 解析。
4. 依赖前端运行时渲染的动态页面
   - 先确认服务端是否也输出目录内容；如果不是，不能直接照搬当前实现。

额外确认项：

- 页面是否能直接抓到真实 feed 链接，而不是跳转页。
- 分组标题是否稳定存在。
- 同一分组下是否可能出现重名或重复 URL。
- 页面是否需要特殊 UA。当前发现源抓取默认用 `UA_ANDROID`。

## 标准接入步骤

### 1. 在 `NewsRepository.kt` 增加源常量

为新目录增加公开入口 URL 常量，例如：

```kotlin
const val EXAMPLE_SOURCE_URL = "https://example.com/feeds"
```

如果需要原始地址和展示地址分离，也一起定义，例如 README 地址和 raw 地址。

### 2. 增加抓取入口方法

统一通过 `fetchFeedCatalogGroups()` 包装，保持错误信息和网络行为一致：

```kotlin
suspend fun fetchExampleFeedGroups(): List<FeedCatalogGroup> = fetchFeedCatalogGroups(
    requestUrl = EXAMPLE_SOURCE_URL,
    sourceName = "Example",
    parser = ::parseExampleFeedGroups
)
```

### 3. 实现解析函数

解析函数目标是返回：

```kotlin
List<FeedCatalogGroup>
```

每个分组都包含：

- `name`
- `feeds: List<FeedCatalogSource>`

实现原则：

- 只提取真实 feed 链接。
- 丢弃空标题、空 URL、空分组。
- 尽量限定正文容器，避免抓到导航栏、页脚、GitHub 链接等噪音。
- 名称为空时可复用 `normalizeCatalogSourceName()` 做兜底。

### 4. 在 `DiscoverViewModel.kt` 增加状态和加载方法

需要补两部分：

1. 一个新的 `MutableStateFlow<DiscoverUiState>`
2. 一个 `loadExampleFeeds()` 方法，复用 `loadCatalog()`

例如：

```kotlin
private val _exampleState = MutableStateFlow(DiscoverUiState())
val exampleState: StateFlow<DiscoverUiState> = _exampleState.asStateFlow()

fun loadExampleFeeds(forceRefresh: Boolean = false) {
    loadCatalog(
        stateFlow = _exampleState,
        forceRefresh = forceRefresh,
        loader = repository::fetchExampleFeedGroups,
        defaultErrorMessage = "加载 Example 订阅市场失败"
    )
}
```

### 5. 在 `DiscoverScreen.kt` 增加入口卡片

新增一个 `DiscoverPlugin`：

- `title`
- `subtitle`
- `description`
- `onClick`

建议描述写清楚“这个目录的来源”和“用户能做什么”，不要只写站点名。

### 6. 在 `PlinkMarketScreen.kt` 增加市场页包装函数

当前市场页已抽成通用 `FeedCatalogMarketScreen()`，新增源通常只需要再包一层：

```kotlin
@Composable
fun ExampleMarketScreen(...) {
    val state by discoverViewModel.exampleState.collectAsState()
    LaunchedEffect(Unit) {
        discoverViewModel.loadExampleFeeds()
    }

    FeedCatalogMarketScreen(
        title = "Example",
        sourceUrl = NewsRepository.EXAMPLE_SOURCE_URL,
        state = state,
        onBack = onBack,
        onRefresh = { discoverViewModel.loadExampleFeeds(forceRefresh = true) },
        onOpenFeedPreview = onOpenFeedPreview
    )
}
```

如果只是普通目录列表，优先复用这个通用市场页，不要再单独造一套 UI。

### 7. 在 `NavRoutes.kt` 增加路由

通常需要：

1. 市场页路由
2. 预览页路由
3. `name` 参数
4. `url` 参数
5. 一个构造预览路由的帮助方法

命名建议保持与现有风格一致，例如：

- `EXAMPLE_MARKET`
- `EXAMPLE_FEED_PREVIEW`
- `exampleFeedPreview(name, url)`

### 8. 在 `MainActivity.kt` 接入导航

这里通常有四处要改：

1. `DiscoverScreen(...)` 传入新的点击回调。
2. 新增 `composable(NavRoutes.EXAMPLE_MARKET)`。
3. 新增 `composable(route = NavRoutes.EXAMPLE_FEED_PREVIEW, ...)`。
4. 在从预览页跳到高级编辑页并保存后，补上返回对应市场页的分支。

第 4 点非常容易漏。当前逻辑在 `EditSourceScreen` 的 `onSave` 回退分支里，根据上一层预览路由决定返回哪个市场页。

### 9. 预览页一般复用 `PlinkFeedPreviewScreen`

只要新发现源输出的是标准 RSS/Atom URL，就通常不需要新建预览页，可以继续复用：

- `PlinkFeedPreviewScreen`
- `PlinkFeedPreviewViewModel`

如果后续某个目录需要特殊预览行为，再单独拆页面。

## `Wechat2RSS` 接入经验

`Wechat2RSS` 的页面是静态 HTML，不是 OPML，也不是 Markdown。

它的稳定结构特征是：

- 正文根节点在 `.vp-doc._list_all > div`
- 分类标题是 `h2`
- 每个分类下面有多个 `feed/*.xml` 链接

因此解析策略是：

1. 先定位正文根节点。
2. 顺序遍历直属子节点。
3. 遇到 `h2` 就开始新分组。
4. 在下一个 `h2` 前，只提取 `https://wechat2rss.xlab.app/feed/*.xml` 链接。

这个例子说明：

- 发现源页面只要结构稳定，不必要求它是 OPML。
- 关键是先缩小正文范围，再提取真实 feed 链接。

## 验证清单

接入完成后，至少执行以下检查：

1. 编译检查

```bash
./gradlew :app:compileDebugKotlin
```

2. 单元测试

```bash
./gradlew testDebugUnitTest
```

3. 最终安装启动检查

```bash
./run-debug.sh
```

4. 人工验证

- “发现”页能看到新增卡片。
- 市场页能正常加载分组列表。
- 点击条目能进入预览页。
- 预览页可直接订阅。
- 从预览页进入“高级编辑”并保存后，能回到正确市场页。
- 市场页刷新按钮可正常重试。

## 常见坑

### 忘记补编辑保存后的回退逻辑

表现：

- 从预览页进入高级编辑后，保存时跳回了“订阅”页，而不是原市场页。

处理：

- 在 `MainActivity.kt` 的 `EditSourceScreen.onSave` 分支里，补对应预览路由判断。

### 解析器抓到了页面噪音链接

表现：

- 市场页出现 GitHub、返回顶部、导航菜单等错误链接。

处理：

- 不要直接全页抓 `a[href]`。
- 先限定正文容器，再限定 feed 链接模式。

### 分组标题解析不稳定

表现：

- 所有源都落到空分组，或分组名里混入锚点字符。

处理：

- 优先用更贴近正文文本的提取方式，例如 `ownText()`。
- 必要时清理零宽字符和锚点文本。

### 页面能打开，但不是服务端渲染

表现：

- 浏览器里有内容，抓取结果却为空。

处理：

- 先确认网络返回 HTML 本身是否包含列表。
- 如果内容依赖前端脚本运行，当前这套静态抓取方案不能直接复用。

## 新增发现源时的最小检查表

- 已新增 URL 常量
- 已新增 `fetchXxxFeedGroups()`
- 已新增 `parseXxxFeedGroups()`
- 已新增 ViewModel 状态和加载方法
- 已新增发现页卡片
- 已新增市场页包装函数
- 已新增路由常量和预览路由构造方法
- 已在 `MainActivity.kt` 接好市场页和预览页导航
- 已补上高级编辑保存后的回退分支
- 已完成编译、测试、运行验证
