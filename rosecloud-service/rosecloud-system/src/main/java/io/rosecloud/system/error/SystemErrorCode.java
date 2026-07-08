package io.rosecloud.system.error;

import io.rosecloud.common.core.error.ErrorCode;

/** System-service error codes (module prefix {@code system}). */
public enum SystemErrorCode implements ErrorCode {

    TENANT_CODE_EXISTS("租户编码已存在"),
    TENANT_NOT_FOUND("租户不存在"),
    TENANT_STATUS_INVALID("租户当前状态不允许该操作"),
    USERNAME_EXISTS("用户名已存在"),
    USER_NOT_FOUND("用户不存在"),
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
