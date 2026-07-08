package io.rosecloud.system.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** Domain view of a per-user setting. */
public final class UserSetting {

    private final Long userId;
    private final String key;
    private final String value;
    private final LocalDateTime updatedAt;
    private final Long updatedBy;

    public UserSetting(Long userId, String key, String value, LocalDateTime updatedAt, Long updatedBy) {
        this.userId = userId;
        this.key = key;
        this.value = value;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getUserId() { return userId; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSetting that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(key, that.key)
                && Objects.equals(value, that.value) && Objects.equals(updatedAt, that.updatedAt)
                && Objects.equals(updatedBy, that.updatedBy);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, key, value, updatedAt, updatedBy); }

    @Override
    public String toString() {
        return "UserSetting[" + "userId=" + userId + ", key=" + key + ", value=" + value
                + ", updatedAt=" + updatedAt + ", updatedBy=" + updatedBy + ']';
    }
}
