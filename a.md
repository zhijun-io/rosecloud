
参考、借鉴 thingsboard 源码（本地已克隆）从零重写 rosecloud-starter-tech/rosecloud-starter-security，达成高内聚低耦合、以 UserDetailsService 为唯一必需接入点、接口注入、函数式扩展、内建安全增强、实现 RBAC。本期不实现 OAuth2/MFA（仅预留扩展点）。
Starter 为 servlet 版 Spring Security 自动装配，供所有 servlet 消费者（auth / system / notice / monolith）使用。不得从 git 历史读取文件并作为设计的参考依据，可以对已有的代码进行改造，
交付标准：该模块单元测试通过。单体应用集成该模块，可以登录、登出等等。