#!/usr/bin/env bash
set -euo pipefail

up() {
  xh --ignore-stdin -q -b --timeout 3 GET "$1/actuator/health/liveness" >/dev/null 2>&1
}

post_json() {
  local token=$1 url=$2 body=$3
  xh --ignore-stdin -b -A bearer -a "$token" POST "$url" --raw "$body" 'Content-Type: application/json'
}

put_json() {
  local token=$1 url=$2 body=$3
  xh --ignore-stdin -b -A bearer -a "$token" PUT "$url" --raw "$body" 'Content-Type: application/json'
}

expect_401() {
  if "$@" >/dev/null 2>&1; then
    echo "expected 401 but request succeeded: $*" >&2
    exit 1
  fi
}

expect_fail() {
  local resp
  resp=$("$@" 2>/dev/null || true)
  jq -e '.success == false' <<<"$resp" >/dev/null
}

# monolith 与 gateway 同为宿主 8080（profile 互斥），无法用端口区分，改用 ROSECLOUD_MODE
# 取值：monolith | microservice（默认 microservice）
is_monolith() {
  [[ "${ROSECLOUD_MODE:-microservice}" == "monolith" ]]
}


login_token() {
  local user=$1 pass=$2
  local resp
  resp=$(xh --ignore-stdin -b POST "$BASE_URL/api/auth/login" \
    --raw "$(jq -nc --arg username "$user" --arg password "$pass" '{username:$username,password:$password}')" \
    'Content-Type: application/json')
  jq -e '.success == true' <<<"$resp" >/dev/null
  jq -r '.data.accessToken' <<<"$resp"
}

# 两种模式均监听宿主 8080，先确认实例在跑，再按 ROSECLOUD_MODE 区分
if ! up "http://127.0.0.1:8080"; then
  echo "no RoseCloud instance detected on :8080 (start monolith/microservice, set ROSECLOUD_MODE=monolith|microservice)" >&2
  exit 1
fi
BASE_URL="http://127.0.0.1:8080"
echo "mode=${ROSECLOUD_MODE:-microservice}"

echo "BASE_URL=$BASE_URL"

now=$(date +%s)
future=$(date -v+30d +%F 2>/dev/null || date -d '+30 days' +%F)
user_email="u${now}@test.local"
tenant_admin_email="tenant${now}@test.local"

login=$(xh --ignore-stdin -b POST "$BASE_URL/api/auth/login" \
  --raw "$(jq -nc --arg username 'admin@rosecloud.local' --arg password 'admin123' '{username:$username,password:$password}')" \
  'Content-Type: application/json')
admin_token=$(jq -e -r '.data.accessToken' <<<"$login")
admin_refresh=$(jq -e -r '.data.refreshToken' <<<"$login")
jq -e '.success == true' <<<"$(xh --ignore-stdin -b POST "$BASE_URL/api/auth/refresh" --raw "$(jq -nc --arg rt "$admin_refresh" '{refreshToken:$rt}')" 'Content-Type: application/json')" >/dev/null
jq -e '.success == true and .data.user.username == "admin@rosecloud.local"' <<<"$(xh --ignore-stdin -b -A bearer -a "$admin_token" GET "$BASE_URL/api/system/users/me")" >/dev/null
expect_fail xh --ignore-stdin -b POST "$BASE_URL/api/auth/login" \
  --raw "$(jq -nc --arg username 'admin@rosecloud.local' --arg password 'bad' '{username:$username,password:$password}')" \
  'Content-Type: application/json'

api() {
  xh --ignore-stdin -b -A bearer -a "$1" "${@:2}"
}

INTERNAL_KEY="${ROSECLOUD_INTERNAL_API_KEY:-rosecloud-dev-internal-key}"
internal_api() {
  local method=$1 url=$2; shift 2
  xh --ignore-stdin -b "$method" "$url" "$@" "X-RoseCloud-Internal-Key:$INTERNAL_KEY"
}

echo "users"
user_id=$(api "$admin_token" POST "$BASE_URL/api/system/users" username="$user_email" password=Test@1234 nickname="u" | jq -r '.data')
expect_fail api "$admin_token" POST "$BASE_URL/api/system/users" username="$user_email" password=Test@1234 nickname="u"
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/users/$user_id")" >/dev/null
put_json "$admin_token" "$BASE_URL/api/system/users/$user_id/roles" "$(jq -nc '{roleIds:[1]}')" >/dev/null
jq -e '.success == true and .data[0] == 1' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/users/$user_id/roles")" >/dev/null
api "$admin_token" DELETE "$BASE_URL/api/system/users/$user_id" >/dev/null

echo "roles / menus / depts"
role_id=$(api "$admin_token" POST "$BASE_URL/api/system/roles" code="r$now" name="r$now" | jq -r '.data')
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/roles/$role_id")" >/dev/null
put_json "$admin_token" "$BASE_URL/api/system/roles/$role_id/menus" "$(jq -nc '{menuIds:[1]}')" >/dev/null
jq -e '.success == true and .data[0] == 1' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/roles/$role_id/menus")" >/dev/null
menu_id=$(api "$admin_token" POST "$BASE_URL/api/system/menus" parentId=2 name="m$now" type=2 sort=99 status=1 visible=1 perms="system:m:$now" | jq -r '.data')
jq -e '.success == true' <<<"$(api "$admin_token" PUT "$BASE_URL/api/system/menus/$menu_id" parentId=2 name="m${now}x" type=2 sort=98 status=1 visible=1 perms="system:m:$now")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/menus")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/menus/tree")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/menus/me")" >/dev/null
api "$admin_token" DELETE "$BASE_URL/api/system/menus/$menu_id" >/dev/null
dept_id=$(api "$admin_token" POST "$BASE_URL/api/system/depts" parentId=1 name="d$now" sort=9 status=1 | jq -r '.data')
jq -e '.success == true' <<<"$(api "$admin_token" PUT "$BASE_URL/api/system/depts/$dept_id" parentId=1 name="d${now}x" sort=8 status=1)" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/depts")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/depts/tree")" >/dev/null
api "$admin_token" DELETE "$BASE_URL/api/system/depts/$dept_id" >/dev/null

echo "dict"
dict_type_id=$(api "$admin_token" POST "$BASE_URL/api/system/dict-types" code="dt$now" name="dt$now" status=1 remark=foo | jq -r '.data')
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/dict-types/$dict_type_id")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/dict-types?current=1&size=10")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" PUT "$BASE_URL/api/system/dict-types/$dict_type_id" code="dt$now" name="dt${now}x" status=0 remark=bar)" >/dev/null
dict_data_id=$(api "$admin_token" POST "$BASE_URL/api/system/dict-data" dictCode="dt$now" label=foo value=bar sort=1 status=1 remark=hello | jq -r '.data')
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/dict-data/$dict_data_id")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/dict-data/by-code/dt$now")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/dict-data?current=1&size=10&dictCode=dt$now")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" PUT "$BASE_URL/api/system/dict-data/$dict_data_id" dictCode="dt$now" label=foo2 value=bar2 sort=2 status=0 remark=bye)" >/dev/null
api "$admin_token" DELETE "$BASE_URL/api/system/dict-data/$dict_data_id" >/dev/null
api "$admin_token" DELETE "$BASE_URL/api/system/dict-types/$dict_type_id" >/dev/null

echo "tenant"
tenant_id=$(api "$admin_token" POST "$BASE_URL/api/system/tenants" name="Tenant $now" contactUser=owner contactPhone=13800000000 expireTime="$future" remark=remark adminUsername="$tenant_admin_email" | jq -r '.data')
for _ in 1 2 3 4 5 6 7 8 9 10; do
  if jq -e --arg tenant_id "$tenant_id" '.success == true and any(.data.records[]?; .id == $tenant_id and .status == "ENABLED")' \
      <<<"$(api "$admin_token" GET "$BASE_URL/api/system/tenants?current=1&size=10")" >/dev/null; then
    break
  fi
  sleep 1
done
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/tenants?current=1&size=10")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" POST "$BASE_URL/api/system/tenants/$tenant_id/disable")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" POST "$BASE_URL/api/system/tenants/$tenant_id/enable")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/audit-logs?current=1&size=10")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/login-logs?current=1&size=10")" >/dev/null

echo "activate tenant admin"
activation_info=$(xh --ignore-stdin -b POST "$BASE_URL/api/noauth/activate/resend" \
  --raw "$(jq -nc --arg username "$tenant_admin_email" '{username:$username}')" \
  'Content-Type: application/json')
activate_token=$(jq -r '.data.activateToken' <<<"$activation_info")
[[ -n "$activate_token" ]]
jq -e '.success == true' <<<"$(xh --ignore-stdin -b POST "$BASE_URL/api/noauth/activate" --raw "$(jq -nc --arg at "$activate_token" '{activateToken:$at, password:"Tp@123456"}')" 'Content-Type: application/json')" >/dev/null

echo "notice"
notice_id=$(api "$admin_token" POST "$BASE_URL/api/notice/notices" title="n$now" content=hello targetType=0 needConfirm=true channels=1 | jq -r '.data')
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/notice/notices?current=1&size=10")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/notice/notices/me?current=1&size=10")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/notice/notices/me/$notice_id")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" POST "$BASE_URL/api/notice/notices/me/$notice_id/read")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" POST "$BASE_URL/api/notice/notices/me/$notice_id/confirm")" >/dev/null

echo "tenant login"
tenant_token=$(login_token "$tenant_admin_email" Tp@123456)
jq -e '.success == true and .data.user.tenantId != null and .data.roles[0] == "tenant-admin"' <<<"$(api "$tenant_token" GET "$BASE_URL/api/system/users/me")" >/dev/null
jq -e '.success == true' <<<"$(api "$tenant_token" GET "$BASE_URL/api/notice/notices/me?current=1&size=10")" >/dev/null

echo "session / revoke"
logout_token=$(login_token 'admin@rosecloud.local' admin123)
jq -e '.success == true' <<<"$(api "$logout_token" POST "$BASE_URL/api/auth/logout")" >/dev/null
expect_401 api "$logout_token" GET "$BASE_URL/api/system/users/me"

if is_monolith && [[ "${RUN_INTERNAL:-0}" == "1" ]]; then
  echo "internal"
  jq -e '.success == true and .data.username == "admin@rosecloud.local"' <<<"$(internal_api GET "$BASE_URL/internal/users/auth/admin@rosecloud.local")" >/dev/null
  jq -e '.success == true' <<<"$(internal_api POST "$BASE_URL/internal/login-logs" '{"username":"admin","success":false,"failReason":"bad"}')" >/dev/null
  jq -e '.success == true' <<<"$(internal_api POST "$BASE_URL/internal/notice/recipients" '{"targetType":0,"targetTenantId":null,"targetRoleCode":null}')" >/dev/null


fi

echo "kick / logout"
kick_token=$(login_token 'admin@rosecloud.local' admin123)
kick_id=$(api "$admin_token" GET "$BASE_URL/api/system/sessions/online?current=1&size=100" | jq -r --arg token "$kick_token" '.data.records[] | select(.token == $token) | .id' | head -n1)
[[ -n "$kick_id" ]]
api "$admin_token" DELETE "$BASE_URL/api/system/sessions?sessionId=$kick_id" >/dev/null
expect_401 api "$kick_token" GET "$BASE_URL/api/system/users/me"

logout_token=$(login_token 'admin@rosecloud.local' admin123)
jq -e '.success == true' <<<"$(api "$logout_token" POST "$BASE_URL/api/auth/logout")" >/dev/null
expect_401 api "$logout_token" GET "$BASE_URL/api/system/users/me"

echo "done"
