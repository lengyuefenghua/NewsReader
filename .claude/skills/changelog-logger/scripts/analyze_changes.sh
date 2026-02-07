#!/bin/bash

# 分析 git diff 并输出修改摘要
# 用法: ./scripts/analyze_changes.sh [commit-range]

if [ -z "$1" ]; then
    echo "使用默认范围：未暂存的修改"
    git diff --name-status
else
    echo "使用提交范围：$1"
    git diff --name-status "$1"
fi

echo ""
echo "详细的修改内容："
echo "---"

if [ -z "$1" ]; then
    git diff
else
    git diff "$1"
fi
