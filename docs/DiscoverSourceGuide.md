# 发现源接入手册

## 目的

这份手册用于指导后续新增“发现”页里的订阅市场插件。

当前仓库里的“发现源”已经不是单一模式，而是两种接入方式：

1. 直连解析模式
   - 应用直接请求目录页，再把页面解析成分组后的订阅源列表。
2. WebView 借壳模式
   - 应用不自己重绘目录列表，而是借用第三方网站已有的登录、标签、搜索和筛选能力，在网页内拦截“订阅”动作，再跳到应用内预览页。

无论是哪种模式，用户最终都应该能：

1. 在“发现”页看到入口卡片。
2. 打开对应市场页或网页搜索页。
3. 进入订阅源预览页查看文章样例。
4. 从预览页直接订阅，或跳转到高级编辑页继续配置。

这里说的“发现源”不是普通手动添加的 RSS 源，而是一个“帮助用户发现标准 RSS/Atom 链接”的入口。

## 现有实现概览

当前仓库已经接入过以下发现源，可作为参考样例：

- `Plink`
- `Awesome RSSHub Routes`
- `Top RSS List`
- `Wechat2RSS`
- `QiReader`

### 模式 A：直连解析模式

适用样例：

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

### 模式 B：WebView 借壳模式

适用样例：

- `QiReader`

核心调用链如下：

```text
DiscoverScreen
  -> MainActivity / NavRoutes
  -> XxxMarketScreen (visible WebView)
  -> CookieManager keeps login state
  -> inject JavaScript bridge after page load
  -> intercept subscribe button inside webpage
  -> extract name + feed url from current card DOM
  -> NavRoutes.XxxFeedPreview(name, url)
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
  - 直连解析模式的加载状态与刷新入口。
- `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`
  - 直连解析模式的远程页面抓取、目录解析、统一错误处理。
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/PlinkMarketScreen.kt`
  - 直连解析模式的市场页包装函数与通用 `FeedCatalogMarketScreen`。
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/QiReaderMarketScreen.kt`
  - WebView 借壳模式的市场页样例，负责可见 `WebView`、登录态保持、JavaScript bridge 和网页内订阅按钮拦截。
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/PlinkFeedPreviewScreen.kt`
  - 订阅预览页。当前两种模式都复用它。
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/FeedPreviewScreen.kt`
  - “订阅”页内的通用预览页，不作为发现源接入模板，但可以参考它的保存与高级编辑交互。

## 接入前先判断模式

优先判断目标站点该走哪种模式，而不是一上来就写解析器。

### 什么时候优先选直连解析模式

满足下面大部分条件时，优先走直连解析模式：

- 不需要登录注册就能浏览目录。
- 应用通过普通 HTTP 请求就能拿到包含目录内容的响应。
- 页面里的真实 feed 链接可以直接提取。
- 目录结构能稳定转换成 `List<FeedCatalogGroup>`。

常见页面类型：

1. OPML / XML 目录
   - 适合像 `Awesome RSSHub Routes` 一样用 `Parser.xmlParser()` 解析。
2. Markdown 文本目录
   - 适合像 `Top RSS List` 一样按行扫描标题、表格和链接。
3. 静态 HTML 页面
   - 适合像 `Plink`、`Wechat2RSS` 一样用 Jsoup 解析。
4. 服务端已输出主要目录内容的动态页面
   - 先确认网络返回 HTML 本身是否已经有可解析内容；如果有，仍然可以继续走直连解析模式。

### 什么时候考虑 WebView 借壳模式

满足下面任一特征时，优先评估 WebView 借壳模式：

- 网站必须登录注册后才能使用搜索、标签或筛选功能。
- 目录内容主要依赖前端运行时渲染，普通 HTTP 抓取拿不到结果。
- 你真正想复用的是网站已有的搜索体验，而不是自己重建一套列表 UI。
- 页面渲染完成后，卡片 DOM 里已经能拿到真实 feed 链接，或者点击“订阅”时能在网页层稳定拿到该链接。

额外确认项：

- 网站是否允许在 Android `WebView` 中登录。
- 登录后是否只影响“发现”阶段，而不会影响后续 RSS 刷新。
- 页面里是否存在稳定的语义锚点，例如按钮文字、`title` 属性、标题标签、相对层级关系。
- 是否能不依赖混淆 class 名完成定位。

### 选型建议

- 两种模式都能做时，优先选直连解析模式。
- 只有当站点必须登录、必须依赖站内搜索，或页面内容无法通过静态抓取稳定拿到时，再选 WebView 借壳模式。

## 模式 A：直连解析模式标准接入步骤

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

1. 一个新的 `MutableStateFlow<DiscoverUiState>`。
2. 一个 `loadExampleFeeds()` 方法，复用 `loadCatalog()`。

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

1. 市场页路由。
2. 预览页路由。
3. `name` 参数。
4. `url` 参数。
5. 一个构造预览路由的帮助方法。

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

### 9. 发现源预览页一般复用 `PlinkFeedPreviewScreen`

只要新发现源输出的是标准 RSS/Atom URL，就通常不需要新建预览页，可以继续复用：

- `PlinkFeedPreviewScreen`
- `PlinkFeedPreviewViewModel`

仓库里另有 `FeedPreviewScreen`，它用于“订阅”页内的通用预览流，不是新增发现源时的默认模板。

如果后续某个目录需要特殊预览行为，再单独拆页面。

## 模式 B：WebView 借壳模式标准接入步骤

### 1. 在 `NewsRepository.kt` 增加入口 URL 常量

即使不走 `NewsRepository.fetchXxxFeedGroups()`，也建议把入口地址常量放在这里统一管理，例如：

```kotlin
const val EXAMPLE_DISCOVER_URL = "https://example.com/discover"
```

### 2. 在 `DiscoverScreen.kt` 增加入口卡片

卡片描述要明确说明这是“登录后搜索/筛选订阅源”的入口，而不是静态目录。

### 3. 新增专用市场页，例如 `XxxMarketScreen.kt`

这个页面通常不再依赖 `DiscoverViewModel`，而是直接承载一个可见 `WebView`。

最小职责包括：

- 用 `AndroidView` 挂载 `WebView`。
- 开启 JavaScript 和 DOM Storage。
- 配置 `CookieManager` 接受 cookie。
- 提供 `刷新` 和 `清登录` 操作。
- 页面销毁时释放 `WebView`。

### 4. 用 `CookieManager` 保持登录态

这是 WebView 借壳模式的关键边界：

- 登录态只服务“发现”阶段。
- 不要把 Cookie 塞进 `Source` 或订阅源刷新链路。
- 页面加载完成后记得 `flush()`。
- 最好提供“清登录”入口，便于切账号和排查问题。

### 5. 注入 JavaScript bridge，拦截网页里的“订阅”动作

推荐流程：

1. `addJavascriptInterface(...)` 暴露桥接对象。
2. `onPageFinished()` 后注入脚本。
3. 脚本里监听网页点击事件。
4. 命中“订阅”按钮后 `preventDefault()`。
5. 从当前卡片 DOM 提取 `name + feedUrl`。
6. 回调到 Android，导航到应用内预览页。

### 6. 定位 DOM 时优先用语义锚点和相对结构

不要优先依赖构建产物生成的混淆 class 名，例如 `cj-xx` 这种样式类。

更稳的做法通常是：

- 按钮文字，例如 `订阅`。
- 按钮属性，例如 `button[title*="举报"]`。
- 标题标签，例如 `h4 a[href]`。
- 卡片内相对层级关系。

如果页面结构允许，脚本应尽量做到：

- 先从“订阅”按钮向上找卡片根节点。
- 再从卡片里提取标题和 feed 链接。

### 7. 在 `NavRoutes.kt` 增加市场页和预览页路由

WebView 借壳模式同样需要：

- 市场页路由。
- 预览页路由。
- `name` 参数。
- `url` 参数。
- 一个构造预览路由的方法。

### 8. 在 `MainActivity.kt` 接入导航

至少要补三处：

1. `DiscoverScreen(...)` 的点击回调。
2. `composable(NavRoutes.EXAMPLE_MARKET)`。
3. `composable(route = NavRoutes.EXAMPLE_FEED_PREVIEW, ...)`。

如果从预览页还能跳高级编辑，就还要补保存后的回退分支。

### 9. 发现源预览页仍然优先复用 `PlinkFeedPreviewScreen`

网页内拦截到的结果，只要已经是标准 RSS/Atom URL，就继续复用现有预览页：

- 先预览。
- 再直接订阅或进入高级编辑。

不要在网页内“订阅”时直接跳高级编辑页，除非用户需求非常明确。

## `Wechat2RSS` 接入经验

`Wechat2RSS` 属于直连解析模式。

它的页面是静态 HTML，不是 OPML，也不是 Markdown。

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

## `QiReader` 接入经验

`QiReader` 属于 WebView 借壳模式。

这个站点的关键特点是：

- 登录后才能使用标签、搜索和筛选能力。
- 我们真正想复用的是它的网站壳和搜索体验，而不是它的账号体系。
- 页面渲染完成后，单个卡片 DOM 里已经存在标题、原站链接、feed 链接和“订阅”按钮。

因此接入策略是：

1. 进入专用 `WebView` 页面，而不是 `DiscoverViewModel + groups`。
2. 用 `CookieManager` 保持登录态。
3. 在 `onPageFinished()` 后注入 JavaScript。
4. 用按钮文案和相对结构定位卡片，不依赖混淆 class 名。
5. 点击网页内“订阅”时，拦截默认行为并跳到应用内预览页。
6. 后续订阅和高级编辑仍然复用现有流程。

这个例子说明：

- 有些发现源的价值是“借用第三方网站的搜索能力”，而不是“把第三方网站静态解析成自己的列表 UI”。
- 只要边界控制在“发现阶段”，就不需要把登录 Cookie 带进正式订阅源刷新逻辑。

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

通用项：

- “发现”页能看到新增卡片。
- 点击入口后能进入正确页面。
- 点击订阅源后能进入预览页。
- 预览页可直接订阅。
- 从预览页进入“高级编辑”并保存后，能回到正确市场页。

直连解析模式额外检查：

- 市场页能正常加载分组列表。
- 市场页刷新按钮可正常重试。

WebView 借壳模式额外检查：

- 能在 `WebView` 内正常登录。
- 重新进入后登录态仍然可用。
- “清登录”后能回到未登录状态。
- 网页里的“订阅”按钮会跳应用内预览页，而不是网页自己的默认逻辑。

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

### 页面依赖前端渲染，却还在硬套静态抓取

表现：

- 浏览器里有内容，抓取结果却为空。

处理：

- 先确认网络返回 HTML 本身是否包含列表。
- 如果内容依赖前端脚本运行，改走 WebView 借壳模式，而不是继续堆 Jsoup 规则。

### WebView 模式里依赖了混淆 class 名

表现：

- 网站一改构建版本，注入脚本就失效。

处理：

- 优先依赖按钮文案、标题标签、`title` 属性和相对层级，不要先写死 `cj-*` 这类选择器。

### 网页内“订阅”直接跳到了高级编辑页

表现：

- 用户没法先预览内容，体验和现有发现源不一致。

处理：

- 网页内拦截到 feed URL 后，先跳应用内预览页，再从预览页选择直接订阅或高级编辑。

### 登录态没有明确边界

表现：

- 想把网页 Cookie 带进正式 RSS 刷新流程，导致模型复杂度快速膨胀。

处理：

- 如果第三方网站只用于发现 feed，就把登录态限制在 WebView 发现页内，不要扩散到 `Source` 模型和刷新链路。

## 新增发现源时的最小检查表

通用：

- 已新增 URL 常量。
- 已新增发现页卡片。
- 已新增路由常量和预览路由构造方法。
- 已在 `MainActivity.kt` 接好市场页和预览页导航。
- 已补上高级编辑保存后的回退分支。
- 已完成编译、测试、运行验证。

直连解析模式额外项：

- 已新增 `fetchXxxFeedGroups()`。
- 已新增 `parseXxxFeedGroups()`。
- 已新增 `DiscoverViewModel` 状态和加载方法。
- 已新增市场页包装函数。

WebView 借壳模式额外项：

- 已新增专用 `WebView` 市场页。
- 已接入 `CookieManager` 登录态保持。
- 已提供“清登录”入口。
- 已完成 JavaScript bridge 注入。
- 已完成网页内“订阅”按钮拦截与 `name + url` 提取。
- 已确认网页内“订阅”会跳到应用内预览页。
