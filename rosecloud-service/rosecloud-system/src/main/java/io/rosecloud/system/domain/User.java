package io.rosecloud.system.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.rosecloud.common.core.model.BaseDataWithAdditionalInfo;
import io.rosecloud.common.core.model.HasAdditionalInfo;
import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasStatus;
import io.rosecloud.common.core.model.HasTenantId;

import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Domain view of a user. ORM-free; the persistence layer maps to/from {@code sys_user}.
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class User extends BaseDataWithAdditionalInfo implements HasAdditionalInfo, HasId, HasStatus<Integer>, HasTenantId {

    private final Long id;
    private final String username;
    private final String nickname;
    private final Integer status;
    private final String tenantId;
    private final LocalDateTime createTime;
    private final Long createBy;
    private final LocalDateTime updateTime;
    private final Long updateBy;

    public User(Long id, String username, String nickname, Integer status, String tenantId, JsonNode additionalInfo) {
        this(id, username, nickname, status, tenantId, additionalInfo, null, null, null, null);
    }

    public User(Long id, String username, String nickname, Integer status, String tenantId, JsonNode additionalInfo,
                LocalDateTime createTime, Long createBy, LocalDateTime updateTime, Long updateBy) {
        super(additionalInfo);
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.status = status;
        this.tenantId = tenantId;
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
    }
}
