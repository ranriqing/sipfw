#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

APP_NAME="wvflx-antd"
PORT=3001

echo "===== ${APP_NAME} 启动脚本 ====="

export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
nvm use v12.22.12

# 停止已有进程（仅停止监听端口的进程）
PIDS=$(lsof -ti:${PORT} -sTCP:LISTEN 2>/dev/null || true)
if [ -n "$PIDS" ]; then
    echo "端口 ${PORT} 已被占用，正在停止进程(监听) $PIDS ..."
    echo "$PIDS" | xargs kill 2>/dev/null || true
    sleep 2
    PIDS=$(lsof -ti:${PORT} -sTCP:LISTEN 2>/dev/null || true)
    if [ -n "$PIDS" ]; then
        echo "$PIDS" | xargs kill -9 2>/dev/null || true
        sleep 1
    fi
fi

echo "启动 ${APP_NAME} (端口: ${PORT}) ..."

yarn serve
