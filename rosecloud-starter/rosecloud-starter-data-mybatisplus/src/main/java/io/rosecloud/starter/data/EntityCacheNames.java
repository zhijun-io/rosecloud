package io.rosecloud.starter.data;

/**
 * 全局缓存名称常量。
 * 用于 {@code EntityCache} 的 cacheName 标识和各模块事件的 entityType 匹配。
 */
public final class EntityCacheNames {

    private EntityCacheNames() {}

    public static final String USER_SECURITY = "user.security";
    public static final String USER_ROLES = "user.roles";
    public static final String USER_PERMS = "user.perms";
    public static final String ROLE_MENU_IDS = "role.menuIds";
    public static final String TENANT = "tenant";
    public static final String MENU = "menu";
    public static final String MENU_LIST = "menu.list";
   public static final String DICT_DATA_BY_CODE = "dict.data.byCode";

    /** 字典类型缓存（dictTypeId → DictType）。 */
    public static final String DICT_TYPE = "dict.type";

    /** 部门缓存（deptId → Dept）。 */
    public static final String DEPT = "dept";

    /** 部门列表缓存（按不同查询参数的部门列表）。 */
    public static final String DEPT_LIST = "dept.list";

}
