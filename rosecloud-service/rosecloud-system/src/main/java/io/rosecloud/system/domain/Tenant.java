package io.rosecloud.system.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.rosecloud.common.core.model.BaseDataWithAdditionalInfo;
import io.rosecloud.common.core.model.HasAdditionalInfo;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Domain view of a tenant. ORM-free; the persistence layer maps to/from {@code sys_tenant}. */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
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
}
