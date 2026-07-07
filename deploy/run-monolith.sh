#!/usr/bin/env bash
#
# 以单体模式启动 RoseCloud（单进程，9160，聚合 auth/system/notice）。
# 前置：deploy/init.sh 已执行（MySQL/Nacos/Redis + 共享配置 + DB 数据）。
# 启动后会轮询健康端点，就绪后打印登录信息；Ctrl-C 停止。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"

# Java 21（优先 JAVA_HOME，否则用 sdkman）
if ! (java -version 2>&1 | grep -q '"21'); then
  if [ -s "${SDKMAN_DIR:-$HOME/.sdkman}/bin/sdkman-init.sh" ]; then
    set +u
    # shellcheck disable=SC1091
    source "${SDKMAN_DIR:-$HOME/.sdkman}/bin/sdkman-init.sh"
    sdk use java 21-tem >/dev/null
    set -u
  fi
fi
java -version 2>&1 | head -1 || { echo "需要 Java 21（建议 sdkman: sdk install java 21-tem）"; exit 1; }

JAR="rosecloud-monolith/target/rosecloud-monolith-0.1.0-SNAPSHOT.jar"
[ -f "$JAR" ] || { echo "==> 构建单体..."; mvn -q -pl rosecloud-monolith -am install -DskipTests; }

if [ -z "${ROSECLOUD_JWT_SECRET:-}" ]; then
  export ROSECLOUD_JWT_SECRET="rosecloud-dev-secret-please-change-me-0123456789"
  echo "⚠️  未设置 ROSECLOUD_JWT_SECRET，使用开发默认值（生产务必设置 ≥32 字节密钥）"
fi

PORT=9160
BASE="http://127.0.0.1:${PORT}"
echo "==> 启动单体 :${PORT}..."
java -jar "$JAR" > /tmp/rosecloud-monolith.log 2>&1 &
APP_PID=$!
cleanup() { kill -9 "$APP_PID" 2>/dev/null || true; }
trap cleanup INT TERM EXIT

echo "==> 等待单体就绪（健康端点）..."
ready=0
for i in $(seq 1 60); do
  if ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "❌ 单体进程已退出，查看 /tmp/rosecloud-monolith.log 排查"; exit 1
  fi
  if curl -fsS -o /dev/null "${BASE}/actuator/health" 2>/dev/null; then
    ready=1; echo "    就绪（第 ${i} 次轮询）"; break
  fi
  sleep 2
done
if [ "$ready" -ne 1 ]; then echo "❌ 单体未在超时内就绪，查看 /tmp/rosecloud-monolith.log 排查"; exit 1; fi

cat <<INFO

==> 单体已就绪 :${PORT}
    登录：POST ${BASE}/api/v1/auth/login
         Content-Type: application/json
         {"username":"admin","password":"admin123"}
    管理员账号：admin / admin123
    登出：POST ${BASE}/api/v1/auth/logout  （带 Authorization: Bearer <token>）
    日志：/tmp/rosecloud-monolith.log
    Ctrl-C 停止

INFO
wait "$APP_PID"
