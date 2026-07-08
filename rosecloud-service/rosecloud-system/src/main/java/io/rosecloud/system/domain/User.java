package io.rosecloud.system.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.rosecloud.common.core.model.BaseDataWithAdditionalInfo;
import io.rosecloud.common.core.model.HasAdditionalInfo;
import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasStatus;
import io.rosecloud.common.core.model.HasTenantId;

import java.util.Objects;

/** Domain view of a user. ORM-free; the persistence layer maps to/from {@code sys_user}. */
public final class User extends BaseDataWithAdditionalInfo implements HasAdditionalInfo, HasId, HasStatus<Integer>, HasTenantId {

    private final Long id;
    private final String username;
    private final String nickname;
    private final Integer status;
    private final Long tenantId;
    public User(Long id, String username, String nickname, Integer status, Long tenantId, JsonNode additionalInfo) {
        super(additionalInfo);
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.status = status;
        this.tenantId = tenantId;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public Integer getStatus() { return status; }
    public Long getTenantId() { return tenantId; }

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
                && Objects.equals(getAdditionalInfo(), user.getAdditionalInfo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, nickname, status, tenantId, getAdditionalInfo());
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
                ']';
    }
}
