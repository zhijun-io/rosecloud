#!/usr/bin/env bash
#
# 以微服务模式启动 RoseCloud（auth:9120 / system:9130 / notice:9150 / gateway:9110）。
# 前置：deploy/init.sh 已执行（MySQL/Nacos/Redis + 共享配置 + DB 数据）。
# 网关经 Nacos 发现路由各服务，并校验 JWT、注入身份头。
# 启动后会轮询网关健康与后端路由就绪，就绪后打印登录信息；Ctrl-C 停止全部。
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

declare -A JARS=(
  [system]="rosecloud-services/rosecloud-system/target/rosecloud-system-0.1.0-SNAPSHOT-exec.jar"
  [notice]="rosecloud-services/rosecloud-notice/target/rosecloud-notice-0.1.0-SNAPSHOT-exec.jar"
  [gateway]="rosecloud-services/rosecloud-gateway/target/rosecloud-gateway-0.1.0-SNAPSHOT.jar"
  [auth]="rosecloud-services/rosecloud-auth/target/rosecloud-auth-0.1.0-SNAPSHOT-exec.jar"
)
need=0; for j in "${JARS[@]}"; do [ -f "$j" ] || need=1; done
[ "$need" -eq 0 ] || { echo "==> 构建服务..."; mvn -q install -DskipTests; }

if [ -z "${ROSECLOUD_JWT_SECRET:-}" ]; then
  export ROSECLOUD_JWT_SECRET="rosecloud-dev-secret-please-change-me-0123456789"
  echo "⚠️  未设置 ROSECLOUD_JWT_SECRET，使用开发默认值（auth 与 gateway 必须一致；生产务必设置 ≥32 字节密钥）"
fi

GW=9110
BASE="http://127.0.0.1:${GW}"
echo "==> 启动微服务（auth:9120 system:9130 notice:9150 gateway:${GW}）..."
for name in "${!JARS[@]}"; do
  java -jar "${JARS[$name]}" > "/tmp/rosecloud-$name.log" 2>&1 &
  echo "    $name -> /tmp/rosecloud-$name.log (pid $!)"
done
trap 'kill 0' INT TERM

# 轮询网关健康
echo "==> 等待网关 :${GW} 就绪..."
gw_ready=0
for i in $(seq 1 60); do
  if curl -fsS -o /dev/null "${BASE}/actuator/health" 2>/dev/null; then
    gw_ready=1; echo "    网关就绪（第 ${i} 次轮询）"; break
  fi
  sleep 2
done
if [ "$gw_ready" -ne 1 ]; then echo "❌ 网关未在超时内就绪，查看 /tmp/rosecloud-gateway.log"; exit 1; fi

# 轮询后端路由就绪：auth 路由存在（GET /auth/login 非 404）；system 路由存在且鉴权生效（无令牌 401）
echo "==> 等待后端路由就绪（网关定期刷新动态路由，约数秒）..."
routes_ready=0
for i in $(seq 1 45); do
  auth_code=$(curl -s -o /dev/null -w "%{http_code}" "${BASE}/api/v1/auth/login" 2>/dev/null || echo 000)
  sys_code=$(curl -s -o /dev/null -w "%{http_code}" "${BASE}/api/v1/system/depts/tree" 2>/dev/null || echo 000)
  if [ "$auth_code" != "404" ] && [ "$sys_code" = "401" ]; then
    routes_ready=1; echo "    路由就绪（第 ${i} 次轮询）"; break
  fi
  sleep 2
done
if [ "$routes_ready" -ne 1 ]; then
  echo "⚠️  后端路由未在超时内就绪（auth=$auth_code system=$sys_code）；服务可能仍在注册，稍后重试或查看 /tmp/rosecloud-*.log"
fi

cat <<INFO

==> 微服务已启动，网关入口 :${GW}
    登录：POST ${BASE}/api/v1/auth/login
         Content-Type: application/json
         {"username":"admin","password":"admin123"}
    管理员账号：admin / admin123
    登出：POST ${BASE}/api/v1/auth/logout  （带 Authorization: Bearer <token>）
    日志：/tmp/rosecloud-{auth,system,notice,gateway}.log
    Ctrl-C 停止全部服务

INFO
wait
