# RoseCloud 配置模型决策

## 1. 结论

RoseCloud 的配置管理采用最小可用模型：

- `setting_key`
- `system_setting`
- `user_setting`
- `tenant_profile`

其中：

- `setting_key` 负责系统设置和用户设置的 key 定义
- `system_setting` 负责平台级设置
- `user_setting` 负责用户级设置
- `tenant_profile` 负责租户套餐、配额和能力画像
- `customer` 暂不独立设置

实体的扩展信息仍然放在实体自身的 `extra` 字段中，不新增独立扩展模型。

## 2. 表结构

### 2.1 `system_setting`

- `key`
- `value`
- `updated_at`
- `updated_by`

### 2.2 `user_setting`

- `user_id`
- `key`
- `value`
- `updated_at`
- `updated_by`

### 2.3 `tenant_profile`

`tenant_profile` 不纳入键值型设置表，继续作为独立的租户画像模型。

它承载：

- 套餐
- 配额
- 限流
- 默认行为
- 租户能力边界

## 3. 设计原则

### 3.1 表就是作用域

`system_setting` 和 `user_setting` 不需要 `scope` 字段。  
表本身已经表达了作用域，重复字段只会增加歧义。

### 3.2 key 负责命名空间

`setting_key` 负责定义系统设置和用户设置的 key。  
由于没有 `type` 和 `scope`，`key` 需要自己表达分类和语义，例如：

- `ui.language`
- `ui.theme`
- `notify.email.enabled`
- `auth.password.policy`

### 3.3 value 统一承载值

`value` 统一作为值载体：

- 简单值：字符串化存储
- 复杂值：JSON 字符串存储

这样可以保持表结构稳定，避免字段膨胀。

### 3.4 extra 只做扩展

`extra` 只放低频、兼容性、附加元数据，不承载：

- 继承
- 覆盖
- 权限
- 解析规则

## 4. 为什么这样设计

### 4.1 高内聚

`setting_key`、系统设置、用户设置、租户画像各自负责自己的职责，没有一个通用配置模型去承包所有语义。

### 4.2 低耦合

不做统一覆盖链，不做统一求值器，不要求不同域共享相同的访问方式。

### 4.3 不过度设计

只保留当前确实需要的三个结构，避免为了“可能会用到”提前造复杂抽象。

### 4.4 可扩展

如果未来真的需要 tenant 级键值设置，可以按同样形状补一张 `tenant_setting`，并决定是否纳入新的 key 集合。  
但当前不预设这层。

## 5. 不做项

本决策明确不做：

- `setting_key` 的统一覆盖链
- 通用 `SettingResolver`
- `Customer` 独立设置实体
- 配置继承链
- 配置规则引擎

## 6. 落地建议

### 6.1 平台设置

平台配置通过 `setting_key` 定义，再通过 `system_setting` 读写。

### 6.2 用户偏好

用户偏好通过 `setting_key` 定义，再通过 `user_setting` 读写。

### 6.3 租户能力

租户能力、配额和套餐继续走 `tenant_profile`。

### 6.4 实体扩展

实体扩展信息继续留在实体自己的 `extra` 字段。
