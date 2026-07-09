package io.rosecloud.system.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.rosecloud.common.core.model.BaseDataWithAdditionalInfo;
import io.rosecloud.common.core.model.HasAdditionalInfo;
import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasStatus;
import io.rosecloud.common.core.model.HasTenantId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain view of a user. ORM-free; the persistence layer maps to/from {@code sys_user}.
 */
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

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public Integer getStatus() {
        return status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return Objects.equals(id, user.id)
                && Objects.equals(username, user.username)
                && Objects.equals(nickname, user.nickname)
                && Objects.equals(status, user.status)
                && Objects.equals(tenantId, user.tenantId)
                && Objects.equals(getAdditionalInfo(), user.getAdditionalInfo())
                && Objects.equals(createTime, user.createTime)
                && Objects.equals(createBy, user.createBy)
                && Objects.equals(updateTime, user.updateTime)
                && Objects.equals(updateBy, user.updateBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, nickname, status, tenantId, getAdditionalInfo(), createTime, createBy,
                updateTime, updateBy);
    }

    @Override
    public String toString() {
        return "User[" +
                "id=" + id +
                ", username=" + username +
                ", nickname=" + nickname +
                ", status=" + status +
                ", tenantId=" + tenantId +
                ", additionalInfo=" + getAdditionalInfo() +
                ", createTime=" + createTime +
                ", createBy=" + createBy +
                ", updateTime=" + updateTime +
                ", updateBy=" + updateBy +
                ']';
    }
}
