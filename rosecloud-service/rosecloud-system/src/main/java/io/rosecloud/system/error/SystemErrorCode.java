package io.rosecloud.system.error;

import io.rosecloud.common.core.error.ErrorCode;

/** System-service error codes (module prefix {@code SYS}). */
public enum SystemErrorCode implements ErrorCode {

    TENANT_CODE_EXISTS("SYSA001", "租户编码已存在"),
    TENANT_NOT_FOUND("SYSA002", "租户不存在"),
    TENANT_STATUS_INVALID("SYSA003", "租户当前状态不允许该操作"),
    USERNAME_EXISTS("SYSA004", "用户名已存在"),
    USER_NOT_FOUND("SYSA005", "用户不存在"),
    ROLE_CODE_EXISTS("SYSA006", "角色编码已存在"),
    MENU_NOT_FOUND("SYSA007", "菜单不存在"),
    MENU_HAS_CHILDREN("SYSA008", "存在子菜单，无法删除"),
    ROLE_NOT_FOUND("SYSA009", "角色不存在"),
    CONFIG_KEY_EXISTS("SYSA010", "参数键已存在"),
    CONFIG_NOT_FOUND("SYSA011", "参数配置不存在"),
    TASK_NOT_FOUND("SYSA012", "任务不存在"),
    TASK_HANDLER_NOT_FOUND("SYSA013", "未找到任务处理器"),
    TASK_STATUS_INVALID("SYSA014", "任务当前状态不允许该操作"),
    TASK_RETRY_EXCEEDED("SYSA015", "任务重试次数已达上限"),
    DICT_TYPE_CODE_EXISTS("SYSA016", "字典编码已存在"),
    DICT_TYPE_NOT_FOUND("SYSA017", "字典类型不存在"),
    DICT_DATA_NOT_FOUND("SYSA018", "字典项不存在"),
    DEPT_NOT_FOUND("SYSA019", "部门不存在"),
    DEPT_HAS_CHILDREN("SYSA020", "存在子部门，无法删除"),
    SESSION_NOT_FOUND("SYSA021", "会话不存在");

    private final String code;
    private final String message;

    SystemErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
