#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

APP_NAME="gcs-flx"
PORT=18087
CONTEXT_PATH="/gcs-flx"

echo "===== ${APP_NAME} 启动脚本 ====="

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

echo "启动 ${APP_NAME} (端口: ${PORT}, 上下文: ${CONTEXT_PATH}) ..."

mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8"
