# Run Debug Script Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为仓库新增原生 `run-debug.sh` 和 `run-debug.bat`，在 Git Bash 和 CMD 中一键完成 Debug 安装并启动应用。

**Architecture:** 保留现有 `run-debug.ps1` 不变，新增两个独立入口脚本。`run-debug.sh` 使用 Bash 原生命令处理环境检查、设备选择和 Gradle 调用；`run-debug.bat` 使用批处理原生命令完成等价流程。两者共享同一包名和启动策略，但不共享脚本实现，以保证各自环境中的直接可用性。

**Tech Stack:** Git Bash, Windows batch, adb, Gradle Wrapper, Android debug install flow

---

### Task 1: 补充脚本需求文档

**Files:**
- Create: `docs/plans/2026-04-04-run-debug-script-design.md`
- Create: `docs/plans/2026-04-04-run-debug-script.md`

**Step 1: Write the implementation notes**

写明脚本目标、入口文件、执行流程、错误处理和不做事项，避免后续误把需求扩展为交互式设备选择或网络配对工具。

**Step 2: Verify paths and naming**

确认文档路径位于 `docs/plans/`，文件名按日期和主题命名。

**Step 3: Commit**

```bash
git add docs/plans/2026-04-04-run-debug-script-design.md docs/plans/2026-04-04-run-debug-script.md
git commit -m "docs: add run debug script design and plan"
```

---

### Task 2: 新增 Git Bash 原生入口

**Files:**
- Create: `run-debug.sh`

**Step 1: Write the script skeleton**

```bash
#!/usr/bin/env bash
set -euo pipefail
```

**Step 2: Add environment and device checks**

- 检查 `adb` 命令是否存在
- 运行 `adb devices`
- 提取第一台状态为 `device` 的序列号
- 无设备时输出错误并退出

**Step 3: Add install and launch flow**

- 进入脚本所在目录
- 执行 `./gradlew installDebug`
- 执行 `adb -s <serial> shell am force-stop <package>`
- 执行 `adb -s <serial> shell monkey -p <package> -c android.intent.category.LAUNCHER 1`

**Step 4: Manual verification**

Run: `bash ./run-debug.sh`
Expected: 在已连接设备的前提下完成安装并启动应用；无设备时明确失败

---

### Task 3: 新增 CMD 原生入口

**Files:**
- Create: `run-debug.bat`

**Step 1: Write the script skeleton**

```bat
@echo off
setlocal enableextensions enabledelayedexpansion
```

**Step 2: Add environment and device checks**

- 使用 `where adb` 校验命令存在
- 解析 `adb devices` 输出
- 选取第一台状态为 `device` 的序列号
- 无设备时输出错误并退出

**Step 3: Add install and launch flow**

- 切换到脚本所在目录
- 执行 `call gradlew.bat installDebug`
- 执行 `adb -s <serial> shell am force-stop <package>`
- 执行 `adb -s <serial> shell monkey -p <package> -c android.intent.category.LAUNCHER 1`

**Step 4: Manual verification**

Run: `run-debug.bat`
Expected: 在已连接设备的前提下完成安装并启动应用；无设备时明确失败
