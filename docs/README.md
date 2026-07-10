# RoseCloud 文档索引

`rosecloud` 当前按 `adr/`、`plan/`、`prd/` 三类目录组织正文文档，分工如下：

1. [product-requirements.md](/Users/zhijunio/github/rosecloud/docs/prd/product-requirements.md)
   合并后的需求说明，包含产品定位、需求基线、平台边界、角色、业务场景和核心用户用例。
2. [technical-requirements.md](/Users/zhijunio/github/rosecloud/docs/prd/technical-requirements.md)
   技术栈基线说明，包含运行时、框架、通信、安全、数据、消息和交付基线。
3. [development-plan.md](/Users/zhijunio/github/rosecloud/docs/plan/development-plan.md)
   当前开发计划，包含后端闭环的阶段划分、验收点和推荐执行顺序。
4. [configuration-model-decision.md](/Users/zhijunio/github/rosecloud/docs/adr/configuration-model-decision.md)
   配置模型决策，定义 `setting_key`、`system_setting` / `user_setting` / `tenant_profile` 的最小配置模型。
5. [id-and-tenant-model-decision.md](/Users/zhijunio/github/rosecloud/docs/adr/id-and-tenant-model-decision.md)
   主键与租户标识决策，定义雪花 ID 与字符串 `tenantId` 的分工，以及租户 ID 规则（字母开头、仅含字母和数字、最长 10 位、默认大写、系统租户为 `ROOT`）。
6. [postman/README.md](/Users/zhijunio/github/rosecloud/docs/postman/README.md)
   Postman 脚本说明，包含登录、退出、当前用户、用户查询和 token 撤销的导入与执行顺序。

参考资料：

1. [thingsboard-reference.md](/Users/zhijunio/github/rosecloud/docs/adr/thingsboard-reference.md)
   ThingsBoard 平台能力参考，用于对照多租户、认证、MFA、通知等实现思路。

精简原则：

- 目录上分为 `adr/`、`plan/`、`prd/` 三类正文目录
- 文件名优先使用语义命名，不再使用数字前缀
- 需要补充能长期查阅的决策内容时，优先新增独立正文文档，并在索引中登记
- 不再保留 ADR、蓝图拆分稿、运维样例等中间产物
- 后续如果继续补文档，优先追加到对应目录中，而不是重新扩散根目录
