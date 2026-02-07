# Changelog

本文件记录 NewsReader 项目的所有重要修改。

### 2026-02-07 v0.0.2 新增功能 & 性能优化

**修改原因：**
1. 用户希望在重装应用后能够恢复订阅源、阅读数据等所有数据，即使数据库版本升级也能无损导入
2. 用户反馈启用正文提取算法后加载速度慢，影响使用体验
3. 用户反馈原始网页会闪现，影响视觉体验

**修改内容：**
- **新增完整数据备份恢复功能**：
  - 在设置页面添加"数据备份"区域
  - 备份功能：导出所有订阅源、文章、用户设置为 JSON 格式
  - 恢复功能：从备份文件导入数据（全部覆盖策略）
  - 备份文件名：`NewsReader_Full_Backup_[时间戳].json`
  - 支持跨版本升级恢复，数据库版本变更时也能无损导入
- **正文提取性能大幅提升**：
  - 优化前：1500-1700ms（包含 PageLoader 等待）
  - 优化后：200-400ms（跳过等待，直接提取）
  - 性能提升约 75-85%
  - 修改策略：在 `onPageFinished` 后立即提取，跳过 `hasContent()` 检查
- **资源拦截优化**：
  - 拦截广告脚本（`popup.js`, `adsbygoogle.js` 等）
  - 拦截字体文件（`.woff`, `.ttf` 等）
  - 拦截视频文件（`.mp4`, `.webm` 等）
  - 保留所有图片（正文图片正常显示）
  - 进一步提升加载速度
- **遮罩层显示优化**：
  - 白色遮罩层覆盖原始网页（完全无闪现）
  - 顶部进度条实时反馈
  - 提取完成后延迟 300ms 隐藏（等待 DOM 更新）
- **模式切换功能修复**：
  - 添加 `lastViewMode` 状态检测模式切换
  - 模式切换时强制重新加载 URL
  - 修复标准 RSS 无法切换到网页模式的问题
- **图标显示逻辑修正**：
  - viewMode = 0（内容模式）→ 📄 文档图标
  - viewMode = 1（网页模式）→ 🌍 地球图标
  - 所有文章默认网页模式，统一用户体验
- **切换按钮启用条件优化**：
  - 纯网页文章（无内容、无算法、无CSS）：禁用切换按钮
  - 其他情况（至少有一种可切换）：启用切换按钮
- **数据模型扩展**：
  - 新增 `BackupData.kt`：完整数据备份模型
  - 新增 `BackupSettings.kt`：用户设置备份数据
  - 扩展 `ArticleDao` 和 `SourceDao`：添加 `getAllArticles()`、`deleteAllArticles()` 等方法

**影响范围：**
- app/build.gradle.kts (版本号更新至 v0.0.2)
- app/src/main/java/com/lengyuefenghua/newsreader/data/BackupData.kt (新增)
- app/src/main/java/com/lengyuefenghua/newsreader/data/ArticleDao.kt
- app/src/main/java/com/lengyuefenghua/newsreader/data/SourceDao.kt
- app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/SettingsViewModel.kt
- app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/ArticleScreen.kt
- app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreen.kt
- app/src/main/java/com/lengyuefenghua/newsreader/utils/WebViewContentExtractor.kt
- gradlew.bat (新增 Windows 批处理脚本)

**相关提交：**
- feat: 添加完整数据备份恢复功能
- perf: 大幅优化正文提取性能（提升 75-85%）
- perf: 添加资源拦截功能（广告、字体、视频）
- fix: 修复原始网页闪现问题
- fix: 修复模式切换功能
- fix: 修正图标显示逻辑

---
