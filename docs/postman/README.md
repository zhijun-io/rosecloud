# Postman 脚本

这里提供 RoseCloud 当前登录、退出、获取当前登录用户、查询用户和撤销 token 的 Postman collection。

## 文件

- [rosecloud-auth-system.postman_collection.json](/Users/zhijunio/github/rosecloud/docs/postman/rosecloud-auth-system.postman_collection.json)
- [rosecloud-local.postman_environment.json](/Users/zhijunio/github/rosecloud/docs/postman/rosecloud-local.postman_environment.json)

## 使用方式

1. 先启动本地服务，默认网关地址是 `http://localhost:8080`。
2. 导入 collection 和 environment。
3. 先执行 `认证 / 登录成功 - 会话A`，它会写入 `accessTokenA` 和 `refreshTokenA`。
4. 再按顺序执行用户查询和会话查询请求，`获取当前登录用户成功` 会自动写入 `currentUserId`，`查询在线会话成功` 会自动写入 `sessionIdA`。
5. 执行 `撤销 token 成功` 之后，`撤销 token 后再次访问失败` 会验证旧 token 失效。
6. 执行 `认证 / 登录成功 - 会话B` 后，再跑 `退出登录成功` 和 `退出登录后再次访问失败`。
7. `撤销 token 失败 - 缺少 sessionId` 目前会返回 `common.internal_error`，这是 Spring 参数缺失被全局异常处理后的真实行为。

## 覆盖范围

- 登录成功
- 登录失败：密码错误、缺少密码
- 退出登录成功
- 当前登录用户成功、未携带 token 失败
- 用户列表成功、未携带 token 失败
- 用户详情成功、用户不存在失败
- 在线会话成功
- 撤销 token 成功、缺少 `sessionId` 失败、撤销后访问失败
