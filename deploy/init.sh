#!/usr/bin/env bash
#
# RoseCloud 本地基础设施初始化（克隆后执行一次即可）：
#   1. 启动 docker-compose（MySQL / Nacos / Redis / RabbitMQ）
#   2. 等待 MySQL、Nacos 就绪
#   3. 向 Nacos 发布共享配置 rosecloud-common.yaml（含数据源、日志格式）
#   4. 导入 system / notice 的建表与种子数据
#
# 依赖：docker、docker compose。Nacos 本地默认关闭鉴权（NACOS_AUTH_ENABLE=false），
# 配置发布走 v3 admin API + serverIdentity 头。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

NACOS_IDENTITY_KEY="${NACOS_AUTH_IDENTITY_KEY:-serverIdentity}"
NACOS_IDENTITY_VALUE="${NACOS_AUTH_IDENTITY_VALUE:-security}"
MYSQL_USER="${MYSQL_USER:-rosecloud}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-rosecloud123}"
MYSQL_DB="${MYSQL_DATABASE:-rosecloud}"

echo "==> 启动基础设施（mysql / nacos / redis / rabbitmq）..."
docker compose up -d mysql nacos redis rabbitmq

echo "==> 等待 MySQL 就绪..."
until docker exec rosecloud-mysql mysqladmin ping -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --silent >/dev/null 2>&1; do
  echo "    mysql 未就绪，重试..."; sleep 3
done
echo "    mysql 就绪"

echo "==> 等待 Nacos 就绪..."
until curl -fsS -o /dev/null http://127.0.0.1:8848/nacos/ 2>/dev/null; do
  echo "    nacos 未就绪，重试..."; sleep 3
done
echo "    nacos 就绪"

echo "==> 发布 Nacos 共享配置 rosecloud-common.yaml..."
curl -fsS -X POST 'http://127.0.0.1:8848/nacos/v3/admin/cs/config' \
  -H "${NACOS_IDENTITY_KEY}: ${NACOS_IDENTITY_VALUE}" \
  --data-urlencode 'dataId=rosecloud-common.yaml' \
  --data-urlencode 'groupName=DEFAULT_GROUP' \
  --data-urlencode 'namespaceId=public' \
  --data-urlencode 'type=yaml' \
  --data-urlencode "content@deploy/nacos/rosecloud-common.yaml" >/dev/null
echo "    已发布"

echo "==> 导入 DB 建表与种子数据..."
for f in \
  rosecloud-services/rosecloud-system/src/main/resources/db/schema.sql \
  rosecloud-services/rosecloud-system/src/main/resources/db/data.sql \
  rosecloud-services/rosecloud-notice/src/main/resources/db/schema.sql \
  rosecloud-services/rosecloud-notice/src/main/resources/db/data.sql; do
  echo "    应用 $f"
  docker exec -i rosecloud-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" < "$f"
done

echo ""
echo "==> 基础设施就绪。"
echo "    MySQL: 127.0.0.1:3306/$MYSQL_DB  Nacos: 127.0.0.1:8848  Redis: 127.0.0.1:6379"
echo "    下一步：构建（mvn clean install，需 Java 21）后运行 deploy/run-monolith.sh 或 deploy/run-microservice.sh"
