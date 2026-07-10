package io.rosecloud.system.error;

import io.rosecloud.common.core.error.ErrorCode;

/** System-service error codes (module prefix {@code system}). */
public enum SystemErrorCode implements ErrorCode {

    TENANT_CODE_EXISTS("租户编码已存在"),
    TENANT_ID_INVALID("租户标识不合法"),
    TENANT_ID_RESERVED("系统租户标识已保留"),
    TENANT_NOT_FOUND("租户不存在"),
    TENANT_PROFILE_NOT_FOUND("租户套餐不存在"),
    TENANT_PROFILE_EXISTS("租户套餐已存在"),
    TENANT_PROFILE_ID_REQUIRED("租户套餐标识不能为空"),
    TENANT_PROFILE_IN_USE("租户套餐正在被使用，无法删除"),
    TENANT_PROFILE_DEFAULT_DELETE_FORBIDDEN("默认租户套餐不允许删除"),
    TENANT_STATUS_INVALID("租户当前状态不允许该操作"),
    USERNAME_EXISTS("用户名已存在"),
    USERNAME_INVALID("用户名必须是邮箱或手机号"),
    USER_NOT_FOUND("用户不存在"),
    USER_ACTIVATION_TOKEN_INVALID("激活链接无效"),
    USER_ACTIVATION_TOKEN_EXPIRED("激活链接已过期"),
    USER_ACTIVATION_TOKEN_USED("激活链接已使用"),
    PASSWORD_TOO_WEAK("密码不符合复杂度要求"),
    PASSWORD_SAME_AS_OLD("新密码不能与旧密码相同"),
    ROLE_CODE_EXISTS("角色编码已存在"),
    MENU_NOT_FOUND("菜单不存在"),
    MENU_HAS_CHILDREN("存在子菜单，无法删除"),
    ROLE_NOT_FOUND("角色不存在"),
    TASK_NOT_FOUND("任务不存在"),
    TASK_HANDLER_NOT_FOUND("未找到任务处理器"),
    TASK_STATUS_INVALID("任务当前状态不允许该操作"),
    TASK_RETRY_EXCEEDED("任务重试次数已达上限"),
    DICT_TYPE_CODE_EXISTS("字典编码已存在"),
    DICT_TYPE_NOT_FOUND("字典类型不存在"),
    DICT_DATA_NOT_FOUND("字典项不存在"),
    DEPT_NOT_FOUND("部门不存在"),
    DEPT_HAS_CHILDREN("存在子部门，无法删除"),
    SESSION_NOT_FOUND("会话不存在"),
    AUDIT_LOG_NOT_FOUND("审计日志不存在"),
    SETTING_KEY_EXISTS("配置键已存在"),
    SETTING_KEY_NOT_FOUND("配置键不存在"),
    SYSTEM_SETTING_NOT_FOUND("系统配置不存在"),
    USER_SETTING_NOT_FOUND("用户配置不存在");

    private final String message;

    SystemErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
