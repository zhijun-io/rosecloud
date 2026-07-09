package io.rosecloud.system.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.rosecloud.common.core.model.BaseDataWithAdditionalInfo;
import io.rosecloud.common.core.model.HasAdditionalInfo;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Domain view of a tenant. ORM-free; the persistence layer maps to/from {@code sys_tenant}. */
public final class Tenant extends BaseDataWithAdditionalInfo implements HasAdditionalInfo, HasName, HasStatus<TenantStatus> {

    private final String id;
    private final String name;
    private final TenantStatus status;
    private final String contactUser;
    private final String contactPhone;
    private final LocalDate expireTime;
    private final String remark;
    private final String tenantProfileId;
    private final LocalDateTime createTime;
    private final Long createBy;
    private final LocalDateTime updateTime;
    private final Long updateBy;

    public Tenant(String id, String name, TenantStatus status, String contactUser,
                  String contactPhone, LocalDate expireTime, String remark, JsonNode additionalInfo) {
        this(id, name, status, contactUser, contactPhone, expireTime, remark, null, additionalInfo,
                null, null, null, null);
    }

    public Tenant(String id, String name, TenantStatus status, String contactUser,
                  String contactPhone, LocalDate expireTime, String remark, String tenantProfileId,
                  JsonNode additionalInfo) {
        this(id, name, status, contactUser, contactPhone, expireTime, remark, tenantProfileId, additionalInfo,
                null, null, null, null);
    }

    public Tenant(String id, String name, TenantStatus status, String contactUser,
                  String contactPhone, LocalDate expireTime, String remark, String tenantProfileId,
                  JsonNode additionalInfo, LocalDateTime createTime, Long createBy,
                  LocalDateTime updateTime, Long updateBy) {
        super(additionalInfo);
        this.id = id;
        this.name = name;
        this.status = status;
        this.contactUser = contactUser;
        this.contactPhone = contactPhone;
        this.expireTime = expireTime;
        this.remark = remark;
        this.tenantProfileId = tenantProfileId;
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public TenantStatus getStatus() { return status; }
    public String getContactUser() { return contactUser; }
    public String getContactPhone() { return contactPhone; }
    public LocalDate getExpireTime() { return expireTime; }
    public String getRemark() { return remark; }
    public String getTenantProfileId() { return tenantProfileId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public Long getCreateBy() { return createBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Long getUpdateBy() { return updateBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tenant tenant)) {
            return false;
        }
        return Objects.equals(id, tenant.id)
                && Objects.equals(name, tenant.name)
                && status == tenant.status
                && Objects.equals(contactUser, tenant.contactUser)
                && Objects.equals(contactPhone, tenant.contactPhone)
                && Objects.equals(expireTime, tenant.expireTime)
                && Objects.equals(remark, tenant.remark)
                && Objects.equals(tenantProfileId, tenant.tenantProfileId)
                && Objects.equals(getAdditionalInfo(), tenant.getAdditionalInfo())
                && Objects.equals(createTime, tenant.createTime)
                && Objects.equals(createBy, tenant.createBy)
                && Objects.equals(updateTime, tenant.updateTime)
                && Objects.equals(updateBy, tenant.updateBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, status, contactUser, contactPhone, expireTime, remark, tenantProfileId,
                getAdditionalInfo(), createTime, createBy, updateTime, updateBy);
    }

    @Override
    public String toString() {
        return "Tenant[" +
                "id=" + id +
                ", name=" + name +
                ", status=" + status +
                ", contactUser=" + contactUser +
                ", contactPhone=" + contactPhone +
                ", expireTime=" + expireTime +
                ", remark=" + remark +
                ", tenantProfileId=" + tenantProfileId +
                ", additionalInfo=" + getAdditionalInfo() +
                ", createTime=" + createTime +
                ", createBy=" + createBy +
                ", updateTime=" + updateTime +
                ", updateBy=" + updateBy +
                ']';
    }
}
