# AGENTS.md

## 用途
- 本文件只保留仓库特有的开发约束、验证流程和容易踩坑的事实。
- 功能入口、调用链和文件索引请看 `docs/DevelopmentGuide.md`；这里不重复。

## 开始前先读
1. `AGENTS.md`
2. `docs/DevelopmentGuide.md`
3. 你将要修改的目标文件

## 必须遵守的流程
1. 修改前先阅读目标文件。
2. 先给出明确修改方案，等待用户确认后再改代码或配置。
3. 如果范围变化，修改前重新确认。
4. UI 或布局调整，先给文字版 UI 草图。
5. 以下行为无需额外确认：读取文件、搜索代码、解释行为、调查问题。
6. 未经确认，不要修改依赖、数据库 schema 或 git 状态。

## 仓库事实
- 这是单模块 Android 工程，只包含 `:app`。
- 技术栈主线：Compose + ViewModel + Repository + Room/network，依赖注入使用 Koin。
- Room 使用 KSP，不是 kapt：见 `app/build.gradle.kts`。
- 数据库版本是 `11`，且启用了 `fallbackToDestructiveMigration()`：见 `AppDatabase.kt`。任何 schema 变更都要明确说明数据清空风险。
- `debug` 和 `release` 都使用 debug keystore；`release` 开启 R8 和资源压缩；`debug` APK 输出名固定为 `NewsReader.apk`，`release` APK 输出名为 `NewsReaderV<version>.apk`。
- lint 被配置为 `abortOnError = false` 且 `checkReleaseBuilds = false`；lint 通过不能当作正确性证明。

## 验证与命令
- 纯逻辑改动：优先跑最小范围 JVM 测试，命令：`./gradlew testDebugUnitTest --tests "全限定类名或方法名"`。
- 全量 JVM 测试：`./gradlew testDebugUnitTest`。
- instrumentation 测试：`./gradlew connectedDebugAndroidTest`。
- 逻辑、UI、导航、设备相关改动完成后，最终检查必须运行 `./run-debug.sh`。

## `run-debug.sh` 约束
- 依赖 `adb` 在 PATH 中。
- 脚本会选择 `adb devices` 中第一台状态为 `device` 的设备；多设备时不要想当然。
- 脚本实际行为是：`installDebug` -> `am force-stop` -> `monkey` 启动应用，不会替你做更细的人工验证。

## 交付要求
- 完成修改后，说明你实际运行过的验证命令及结果。
- 如果没有自动化覆盖，要明确写出需要人工验证的项目和预期现象。
- 不要把 `docs/DevelopmentGuide.md` 已覆盖的内容再抄回本文件。
