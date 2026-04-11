# 一键编译安装启动脚本设计

- 日期：2026-04-04
- 项目：NewsReader
- 范围：为当前项目新增 Git Bash 和 CMD 双入口调试脚本
- 目标：一条命令完成设备检查、调试安装、强停旧进程和启动应用

## 1. 背景

当前仓库已有 `run-debug.ps1`，能够完成 Android 调试安装与启动流程。但用户希望直接在 Git Bash 和 `cmd` 环境中使用原生脚本入口，而不是间接依赖 PowerShell。

## 2. 需求

新增两个脚本入口：

1. `run-debug.sh`：面向 Git Bash
2. `run-debug.bat`：面向 `cmd` 或资源管理器双击

两个脚本都需要独立完成以下流程：

1. 检查 `adb` 是否可用
2. 查找第一台状态为 `device` 的 Android 设备
3. 执行 Debug 安装
4. 强停旧进程
5. 启动应用

## 3. 方案对比

### 方案 A：双入口转调 PowerShell
- 优点：复用现有 `run-debug.ps1`，改动最小
- 缺点：最终仍依赖 PowerShell，不是原生 `sh`/`bat`

### 方案 B：`sh` 和 `bat` 各自原生实现（推荐）
- 优点：入口直观，适合当前用户的实际终端环境
- 缺点：有少量重复逻辑，后续维护需要同步

### 方案 C：仅保留 Shell 主实现，`bat` 转调 Git Bash
- 优点：维护逻辑更集中
- 缺点：依赖 Git Bash 安装路径，不适合普通 `cmd` 使用

### 推荐结论
采用方案 B。该方案最符合“Git Bash 可直接运行 `.sh`，`cmd` 可直接运行 `.bat`”的使用预期。

## 4. 设计

### 4.1 新增文件
- `run-debug.sh`
- `run-debug.bat`

### 4.2 固定配置
- 包名：`com.lengyuefenghua.newsreader`
- 安装命令：
  - `run-debug.sh` 使用 `./gradlew installDebug`
  - `run-debug.bat` 使用 `gradlew.bat installDebug`

### 4.3 设备选择
- 通过 `adb devices` 获取设备列表
- 仅接受状态为 `device` 的条目
- 默认选第一台可用设备
- 未找到设备时直接失败退出

### 4.4 启动方式
- 先执行 `adb shell am force-stop <package>`
- 再执行 `adb shell monkey -p <package> -c android.intent.category.LAUNCHER 1`

这样无需额外维护启动 Activity 名称，兼容性更稳定。

## 5. 错误处理

脚本在以下场景直接失败退出并输出错误：

1. `adb` 不存在
2. 没有可用设备
3. Gradle 安装失败
4. `adb` 强停或启动失败

## 6. 明确不做

1. 不修改现有 `run-debug.ps1`
2. 不增加多设备交互选择
3. 不增加自动 `adb connect` 到指定 IP 的功能
4. 不改应用代码、Gradle 配置或包名

## 7. 验证建议

实施后建议人工验证：

1. Git Bash 执行 `bash ./run-debug.sh`
2. `cmd` 执行 `run-debug.bat`
3. 已连接设备时可完成安装和启动
4. 无设备时脚本能明确报错
