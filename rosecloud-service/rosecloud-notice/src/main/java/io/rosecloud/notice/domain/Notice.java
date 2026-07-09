package io.rosecloud.notice.domain;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasStatus;
import io.rosecloud.common.core.model.HasTenantId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Domain view of a notice/announcement. ORM-free; mapped to/from {@code sys_notice}.
 * Targeting is resolved from the caller context at read time, so no per-user
 * delivery rows are pre-generated. {@code channels} is a {@link NoticeChannel}
 * bitmask controlling push delivery (station is the default pull feed).
 */
public final class Notice implements HasId, HasStatus<Integer>, HasTenantId {

    private final Long id;
    private final String title;
    private final String content;
    private final Integer targetType;
    private final String targetTenantId;
    private final String targetRoleCode;
    private final String targetUsername;
    private final Integer publishType;
    private final LocalDateTime publishTime;
    private final LocalDateTime effectiveTime;
    private final LocalDateTime expireTime;
    private final Integer status;
    private final Boolean needConfirm;
    private final Long senderId;
    private final String tenantId;
    private final Integer channels;
    private final List<NoticeRecipient> recipients;

    public Notice(Long id, String title, String content, Integer targetType, String targetTenantId,
                  String targetRoleCode, String targetUsername, Integer publishType, LocalDateTime publishTime,
                  LocalDateTime effectiveTime, LocalDateTime expireTime, Integer status,
                  Boolean needConfirm, Long senderId, String tenantId, Integer channels,
                  List<NoticeRecipient> recipients) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.targetType = targetType;
        this.targetTenantId = targetTenantId;
        this.targetRoleCode = targetRoleCode;
        this.targetUsername = targetUsername;
        this.publishType = publishType;
        this.publishTime = publishTime;
        this.effectiveTime = effectiveTime;
        this.expireTime = expireTime;
        this.status = status;
        this.needConfirm = needConfirm;
        this.senderId = senderId;
        this.tenantId = tenantId;
        this.channels = channels;
        this.recipients = recipients == null ? List.of() : List.copyOf(recipients);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Integer getTargetType() { return targetType; }
    public String getTargetTenantId() { return targetTenantId; }
    public String getTargetRoleCode() { return targetRoleCode; }
    public String getTargetUsername() { return targetUsername; }
    public Integer getPublishType() { return publishType; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public LocalDateTime getEffectiveTime() { return effectiveTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public Integer getStatus() { return status; }
    public Boolean getNeedConfirm() { return needConfirm; }
    public Long getSenderId() { return senderId; }
    public String getTenantId() { return tenantId; }
    public Integer getChannels() { return channels; }
    public List<NoticeRecipient> getRecipients() { return recipients; }

    /** Copy with the persisted id set (for dispatch after insert). */
    public Notice withId(Long id) {
        return new Notice(id, title, content, targetType, targetTenantId, targetRoleCode, targetUsername, publishType,
                publishTime, effectiveTime, expireTime, status, needConfirm, senderId, tenantId, channels, recipients);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notice notice)) return false;
        return Objects.equals(id, notice.id) && Objects.equals(title, notice.title)
                && Objects.equals(content, notice.content) && Objects.equals(targetType, notice.targetType)
                && Objects.equals(targetTenantId, notice.targetTenantId)
                && Objects.equals(targetRoleCode, notice.targetRoleCode)
                && Objects.equals(targetUsername, notice.targetUsername)
                && Objects.equals(publishType, notice.publishType)
                && Objects.equals(publishTime, notice.publishTime)
                && Objects.equals(effectiveTime, notice.effectiveTime)
                && Objects.equals(expireTime, notice.expireTime)
                && Objects.equals(status, notice.status)
                && Objects.equals(needConfirm, notice.needConfirm)
                && Objects.equals(senderId, notice.senderId)
                && Objects.equals(tenantId, notice.tenantId)
                && Objects.equals(channels, notice.channels)
                && Objects.equals(recipients, notice.recipients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, targetType, targetTenantId, targetRoleCode, publishType,
                targetUsername, publishTime, effectiveTime, expireTime, status, needConfirm, senderId, tenantId,
                channels, recipients);
    }

    @Override
    public String toString() {
        return "Notice[" +
                "id=" + id +
                ", title=" + title +
                ", content=" + content +
                ", targetType=" + targetType +
                ", targetTenantId=" + targetTenantId +
                ", targetRoleCode=" + targetRoleCode +
                ", targetUsername=" + targetUsername +
                ", publishType=" + publishType +
                ", publishTime=" + publishTime +
                ", effectiveTime=" + effectiveTime +
                ", expireTime=" + expireTime +
                ", status=" + status +
                ", needConfirm=" + needConfirm +
                ", senderId=" + senderId +
                ", tenantId=" + tenantId +
                ", channels=" + channels +
                ", recipients=" + recipients +
                ']';
    }
}
