package io.rosecloud.notice.domain;

import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasTenantId;
import io.rosecloud.common.core.model.HasUserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Per-user read/confirm state for a notice. Created lazily on first interaction
 * (read or confirm); absence means the user has not yet opened it.
 */
public final class NoticeRecord implements HasId, HasUserId, HasTenantId {

    private final Long id;
    private final Long noticeId;
    private final Long userId;
    private final String tenantId;
    private final LocalDateTime readTime;
    private final LocalDateTime confirmTime;
    private final LocalDateTime createTime;
    private final Long createBy;
    private final LocalDateTime updateTime;
    private final Long updateBy;

    public NoticeRecord(Long id, Long noticeId, Long userId, String tenantId,
                        LocalDateTime readTime, LocalDateTime confirmTime) {
        this(id, noticeId, userId, tenantId, readTime, confirmTime, null, null, null, null);
    }

    public NoticeRecord(Long id, Long noticeId, Long userId, String tenantId,
                        LocalDateTime readTime, LocalDateTime confirmTime,
                        LocalDateTime createTime, Long createBy, LocalDateTime updateTime, Long updateBy) {
        this.id = id;
        this.noticeId = noticeId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.readTime = readTime;
        this.confirmTime = confirmTime;
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
    }

    public Long getId() { return id; }
    public Long getNoticeId() { return noticeId; }
    public Long getUserId() { return userId; }
    public String getTenantId() { return tenantId; }
    public LocalDateTime getReadTime() { return readTime; }
    public LocalDateTime getConfirmTime() { return confirmTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public Long getCreateBy() { return createBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Long getUpdateBy() { return updateBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NoticeRecord that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(noticeId, that.noticeId)
                && Objects.equals(userId, that.userId) && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(readTime, that.readTime) && Objects.equals(confirmTime, that.confirmTime)
                && Objects.equals(createTime, that.createTime) && Objects.equals(createBy, that.createBy)
                && Objects.equals(updateTime, that.updateTime) && Objects.equals(updateBy, that.updateBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, noticeId, userId, tenantId, readTime, confirmTime, createTime, createBy, updateTime,
                updateBy);
    }

    @Override
    public String toString() {
        return "NoticeRecord[" +
                "id=" + id +
                ", noticeId=" + noticeId +
                ", userId=" + userId +
                ", tenantId=" + tenantId +
                ", readTime=" + readTime +
                ", confirmTime=" + confirmTime +
                ", createTime=" + createTime +
                ", createBy=" + createBy +
                ", updateTime=" + updateTime +
                ", updateBy=" + updateBy +
                ']';
    }
}
