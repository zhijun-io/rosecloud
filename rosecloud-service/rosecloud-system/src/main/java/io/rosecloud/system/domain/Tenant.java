package io.rosecloud.system.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.rosecloud.common.core.model.BaseDataWithAdditionalInfo;
import io.rosecloud.common.core.model.HasCode;
import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasAdditionalInfo;
import io.rosecloud.common.core.model.HasStatus;

import java.time.LocalDate;
import java.util.Objects;

/** Domain view of a tenant. ORM-free; the persistence layer maps to/from {@code sys_tenant}. */
public final class Tenant extends BaseDataWithAdditionalInfo implements HasAdditionalInfo, HasId, HasName, HasCode, HasStatus<TenantStatus> {

    private final Long id;
    private final String name;
    private final String code;
    private final TenantStatus status;
    private final String contactUser;
    private final String contactPhone;
    private final LocalDate expireTime;
    private final String remark;

    public Tenant(Long id, String name, String code, TenantStatus status, String contactUser,
                  String contactPhone, LocalDate expireTime, String remark, JsonNode additionalInfo) {
        super(additionalInfo);
        this.id = id;
        this.name = name;
        this.code = code;
        this.status = status;
        this.contactUser = contactUser;
        this.contactPhone = contactPhone;
        this.expireTime = expireTime;
        this.remark = remark;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public TenantStatus getStatus() { return status; }
    public String getContactUser() { return contactUser; }
    public String getContactPhone() { return contactPhone; }
    public LocalDate getExpireTime() { return expireTime; }
    public String getRemark() { return remark; }

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
                && Objects.equals(code, tenant.code)
                && status == tenant.status
                && Objects.equals(contactUser, tenant.contactUser)
                && Objects.equals(contactPhone, tenant.contactPhone)
                && Objects.equals(expireTime, tenant.expireTime)
                && Objects.equals(remark, tenant.remark)
                && Objects.equals(getAdditionalInfo(), tenant.getAdditionalInfo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, code, status, contactUser, contactPhone, expireTime, remark, getAdditionalInfo());
    }

    @Override
    public String toString() {
        return "Tenant[" +
                "id=" + id +
                ", name=" + name +
                ", code=" + code +
                ", status=" + status +
                ", contactUser=" + contactUser +
                ", contactPhone=" + contactPhone +
                ", expireTime=" + expireTime +
                ", remark=" + remark +
                ", additionalInfo=" + getAdditionalInfo() +
                ']';
    }
}
