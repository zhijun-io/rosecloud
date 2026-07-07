#!/usr/bin/env bash
set -euo pipefail

up() {
  xh --ignore-stdin -q -b --timeout 3 GET "$1/actuator/health" >/dev/null 2>&1
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

jti_of() {
  printf '%s' "$1" | jq -Rr 'split(".")[1] | gsub("-"; "+") | gsub("_"; "/") | @base64d | fromjson | .jti'
}

login_token() {
  local user=$1 pass=$2
  local resp
  resp=$(xh --ignore-stdin -b POST "$BASE_URL/api/auth/login" username="$user" password="$pass")
  jq -e '.success == true' <<<"$resp" >/dev/null
  jq -r '.data.accessToken' <<<"$resp"
}

pick_mode() {
  if up "http://127.0.0.1:9160"; then
    BASE_URL="http://127.0.0.1:9160"
    return
  fi

  if up "http://127.0.0.1:9110"; then
    BASE_URL="http://127.0.0.1:9110"
    return
  fi

  echo "cannot detect a running RoseCloud mode" >&2
  exit 1
}

pick_mode

echo "BASE_URL=$BASE_URL"

now=$(date +%s)
future=$(date -v+30d +%F 2>/dev/null || date -d '+30 days' +%F)

login=$(xh --ignore-stdin -b POST "$BASE_URL/api/auth/login" username=admin password=admin123)
admin_token=$(jq -e -r '.data.accessToken' <<<"$login")
admin_refresh=$(jq -e -r '.data.refreshToken' <<<"$login")
jq -e '.success == true' <<<"$(xh --ignore-stdin -b POST "$BASE_URL/api/auth/refresh" refreshToken="$admin_refresh")" >/dev/null
jq -e '.success == true and .data.user.username == "admin"' <<<"$(xh --ignore-stdin -b -A bearer -a "$admin_token" GET "$BASE_URL/api/system/users/me")" >/dev/null
expect_fail xh --ignore-stdin -b POST "$BASE_URL/api/auth/login" username=admin password=bad

api() {
  xh --ignore-stdin -b -A bearer -a "$1" "${@:2}"
}

echo "users"
user_id=$(api "$admin_token" POST "$BASE_URL/api/system/users" username="u$now" password=p123456 nickname="u$now" | jq -r '.data')
expect_fail api "$admin_token" POST "$BASE_URL/api/system/users" username="u$now" password=p123456 nickname="u$now"
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

echo "dict / config"
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
config_id=$(api "$admin_token" POST "$BASE_URL/api/system/configs" configKey="ck$now" configValue=vv description=desc | jq -r '.data')
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/configs/$config_id")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/configs/keys/ck$now")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/configs?current=1&size=10")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" PUT "$BASE_URL/api/system/configs/$config_id" configKey="ck$now" configValue=vv2 description=desc2)" >/dev/null
api "$admin_token" DELETE "$BASE_URL/api/system/configs/$config_id" >/dev/null

echo "tenant / task"
tenant_id=$(api "$admin_token" POST "$BASE_URL/api/system/tenants/apply" name="Tenant $now" code="t$now" contactUser=owner contactPhone=13800000000 expireTime="$future" remark=remark adminUsername="tenant$now" adminPassword=tp123456 | jq -r '.data')
expect_fail api "$admin_token" POST "$BASE_URL/api/system/tenants/apply" name="Tenant $now" code="t$now" contactUser=owner contactPhone=13800000000 expireTime="$future" remark=remark adminUsername="tenantx$now" adminPassword=tp123456
task_id=$(api "$admin_token" POST "$BASE_URL/api/system/tenants/$tenant_id/open" | jq -r '.data')
for _ in 1 2 3 4 5 6 7 8 9 10; do
  [[ $(api "$admin_token" GET "$BASE_URL/api/system/tasks/$task_id" | jq -r '.data.status') == SUCCESS ]] && break
  sleep 1
done
jq -e '.success == true and .data.status == "SUCCESS"' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/tasks/$task_id")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/tenants?current=1&size=10")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" POST "$BASE_URL/api/system/tenants/$tenant_id/disable")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" POST "$BASE_URL/api/system/tenants/$tenant_id/enable")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/tasks?current=1&size=10&type=tenant-provisioning")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/audit-logs?current=1&size=10")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/system/login-logs?current=1&size=10")" >/dev/null

echo "notice"
notice_id=$(api "$admin_token" POST "$BASE_URL/api/notice/notices" title="n$now" content=hello targetType=0 needConfirm=true channels=1 | jq -r '.data')
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/notice/notices?current=1&size=10")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/notice/notices/me?current=1&size=10")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" GET "$BASE_URL/api/notice/notices/me/$notice_id")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" POST "$BASE_URL/api/notice/notices/me/$notice_id/read")" >/dev/null
jq -e '.success == true' <<<"$(api "$admin_token" POST "$BASE_URL/api/notice/notices/me/$notice_id/confirm")" >/dev/null

echo "tenant login"
tenant_token=$(login_token "tenant$now" tp123456)
jq -e '.success == true and .data.user.tenantId != null and .data.roles[0] == "tenant-admin"' <<<"$(api "$tenant_token" GET "$BASE_URL/api/system/users/me")" >/dev/null
jq -e '.success == true' <<<"$(api "$tenant_token" GET "$BASE_URL/api/notice/notices/me?current=1&size=10")" >/dev/null

echo "internal"
jq -e '.success == true and .data.username == "admin"' <<<"$(api "$admin_token" GET "$BASE_URL/internal/users/auth/admin")" >/dev/null
jq -e '.success == true' <<<"$(post_json "$admin_token" "$BASE_URL/internal/login-logs" '{"username":"admin","success":false,"failReason":"bad"}')" >/dev/null
jq -e '.success == true' <<<"$(post_json "$admin_token" "$BASE_URL/internal/notice/recipients" '{"targetType":0,"targetTenantId":null,"targetRoleCode":null}')" >/dev/null

echo "session / revoke"
session_token=$(login_token admin admin123)
record_jti="rec-$now"
jq -e '.success == true' <<<"$(post_json "$session_token" "$BASE_URL/internal/sessions" "$(jq -nc --arg jti "$record_jti" --argjson uid 1 --arg exp "${future}T23:59:59" '{jti:$jti,userId:$uid,username:"admin",tenantId:null,expireTime:$exp,ip:"127.0.0.1",userAgent:"xh"}')")" >/dev/null
jq -e '.success == true' <<<"$(api "$session_token" POST "$BASE_URL/internal/sessions/logout-by-jti?jti=$record_jti")" >/dev/null
access_jti=$(jti_of "$session_token")
jq -e '.success == true' <<<"$(post_json "$session_token" "$BASE_URL/internal/revoke" "$(jq -nc --arg jti "$access_jti" --arg exp "${future}T23:59:59" '{jti:$jti,expireTime:$exp}')")" >/dev/null
expect_401 api "$session_token" GET "$BASE_URL/api/system/users/me"

echo "kick / logout"
kick_token=$(login_token admin admin123)
kick_jti=$(jti_of "$kick_token")
kick_id=$(api "$admin_token" GET "$BASE_URL/api/system/sessions/online?current=1&size=100" | jq -r --arg jti "$kick_jti" '.data.records[] | select(.jti == $jti) | .id' | head -n1)
[[ -n "$kick_id" ]]
api "$admin_token" DELETE "$BASE_URL/api/system/sessions/$kick_id" >/dev/null
expect_401 api "$kick_token" GET "$BASE_URL/api/system/users/me"

logout_token=$(login_token admin admin123)
jq -e '.success == true' <<<"$(api "$logout_token" POST "$BASE_URL/api/auth/logout")" >/dev/null
expect_401 api "$logout_token" GET "$BASE_URL/api/system/users/me"

echo "done"
