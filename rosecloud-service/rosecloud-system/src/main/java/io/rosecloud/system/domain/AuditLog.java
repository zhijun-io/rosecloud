package io.rosecloud.system.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** Domain view of a persisted audit entry. ORM-free; mapped to/from {@code sys_audit_log}. */
public final class AuditLog {

    private final Long id;
    private final String action;
    private final String description;
    private final String principal;
    private final Long tenantId;
    private final String target;
    private final long elapsedMillis;
    private final boolean success;
    private final String error;
    private final LocalDateTime createTime;

    public AuditLog(Long id, String action, String description, String principal, Long tenantId, String target,
                    long elapsedMillis, boolean success, String error, LocalDateTime createTime) {
        this.id = id;
        this.action = action;
        this.description = description;
        this.principal = principal;
        this.tenantId = tenantId;
        this.target = target;
        this.elapsedMillis = elapsedMillis;
        this.success = success;
        this.error = error;
        this.createTime = createTime;
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
    public String getPrincipal() { return principal; }
    public Long getTenantId() { return tenantId; }
    public String getTarget() { return target; }
    public long getElapsedMillis() { return elapsedMillis; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public LocalDateTime getCreateTime() { return createTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLog that)) return false;
        return elapsedMillis == that.elapsedMillis && success == that.success
                && Objects.equals(id, that.id) && Objects.equals(action, that.action)
                && Objects.equals(description, that.description) && Objects.equals(principal, that.principal)
                && Objects.equals(tenantId, that.tenantId) && Objects.equals(target, that.target)
                && Objects.equals(error, that.error) && Objects.equals(createTime, that.createTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, action, description, principal, tenantId, target, elapsedMillis, success, error, createTime);
    }

    @Override
    public String toString() {
        return "AuditLog[" +
                "id=" + id +
                ", action=" + action +
                ", description=" + description +
                ", principal=" + principal +
                ", tenantId=" + tenantId +
                ", target=" + target +
                ", elapsedMillis=" + elapsedMillis +
                ", success=" + success +
                ", error=" + error +
                ", createTime=" + createTime +
                ']';
    }
}
