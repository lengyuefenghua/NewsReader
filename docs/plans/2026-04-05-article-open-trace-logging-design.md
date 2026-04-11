# 文章打开性能日志设计

## 背景

当前文章详情页相关调试信息分散在 `logcat` 中，阶段耗时不连续，且不方便在用户完成操作后快速回收现场。需要一个专用、可持久化到应用私有目录的文章打开链路日志，用于排查“打开不流畅/首次慢”的问题。

## 目标

- 将文章打开链路的调试日志统一写入单独文件，不再依赖 `logcat`。
- 应用每次启动时清空旧日志，保证一次排查只看本次会话。
- 你完成操作后，可直接通过 `adb` 读取日志文件进行分析。

## 范围

- 覆盖模块：`ArticleScreen`、`WebViewContentExtractor`、`WebViewManager`
- 存储位置：应用私有缓存目录 `cache/article_open_trace.log`
- 启动行为：`Application.onCreate()` 时清空日志文件
- 不改正文提取算法，不增加设置项，不做 UI 导出入口

## 方案对比

### 方案 A：继续使用 `logcat`

- 优点：无需额外文件管理
- 缺点：噪音大、会话难聚合、操作后回收不稳定

### 方案 B：单文件 trace 日志（采用）

- 每次应用启动清空固定文件
- 每次文章打开创建 `sessionId`
- 所有关键阶段按统一格式追加写入同一个文件
- 优点：实现最小、adb 获取最快、方便按 session 分析

### 方案 C：每篇文章单独一个文件

- 优点：单次会话隔离更强
- 缺点：文件管理和 adb 拉取更麻烦，当前排查收益不高

## 日志格式

每行一条，包含：

- 绝对时间
- `sessionId`
- 相对首事件耗时
- 组件名
- 阶段名
- 详情字段

示例：

```text
2026-04-05 21:30:11.245 | session=3 | +842ms | ArticleScreen | onPageFinished | url=https://example.com/a
```

## 关键阶段

- `ArticleScreen`
  - 进入文章页
  - 切换模式
  - 开始 `loadUrl`
  - `onPageStarted`
  - `onPageFinished`
  - 开始提取
  - 提取完成/失败
  - 遮罩隐藏
- `WebViewContentExtractor`
  - 计时重置
  - 注入 `loader.js`
  - 注入各提取脚本
  - 执行提取命令
- `WebViewManager`
  - 初始化后台 `WebView`
  - 引擎预热完成/失败

## 实现方式

- 新增 `ArticleOpenTraceLogger`
  - 负责 `init(context)`、`clear()`、`startSession(url)`、`log(...)`
  - 内部维护当前 `sessionId` 和会话起始时间
  - 日志写入使用应用私有缓存目录文件追加写入
- 保留最小的纯 Kotlin 格式化逻辑，便于 JVM 单测
- 将现有相关 `Log.d/w/e` 替换为 trace 文件写入

## adb 获取方式

```bash
adb shell run-as com.lengyuefenghua.newsreader cat cache/article_open_trace.log
```

## 验证

- JVM 测试验证日志行格式与相对耗时计算
- `./run-debug.sh` 验证安装启动
- 人工打开文章后，通过 `adb` 读取 trace 文件检查阶段顺序和耗时
