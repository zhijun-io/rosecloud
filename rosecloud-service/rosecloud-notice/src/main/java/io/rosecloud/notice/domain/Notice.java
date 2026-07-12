package io.rosecloud.notice.domain;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasStatus;
import io.rosecloud.common.core.model.HasTenantId;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain view of a notice/announcement. ORM-free; mapped to/from {@code sys_notice}.
 * Targeting is resolved from the caller context at read time, so no per-user
 * delivery rows are pre-generated. {@code channels} is a {@link io.rosecloud.notice.domain.NoticeChannel}
 * bitmask controlling push delivery (station is the default pull feed).
 */
@Value
public class Notice implements HasId, HasStatus<Integer>, HasTenantId {

    Long id;
    String title;
    String content;
    Integer targetType;
    String targetTenantId;
    String targetRoleCode;
    String targetUsername;
    Integer publishType;
    LocalDateTime publishTime;
    LocalDateTime effectiveTime;
    LocalDateTime expireTime;
    Integer status;
    Boolean needConfirm;
    Long senderId;
    String tenantId;
    Integer channels;
    List<NoticeRecipient> recipients;
    LocalDateTime createTime;
    Long createBy;
    LocalDateTime updateTime;
    Long updateBy;

    public Notice(Long id, String title, String content, Integer targetType, String targetTenantId,
                  String targetRoleCode, String targetUsername, Integer publishType, LocalDateTime publishTime,
                  LocalDateTime effectiveTime, LocalDateTime expireTime, Integer status,
                  Boolean needConfirm, Long senderId, String tenantId, Integer channels,
                  List<NoticeRecipient> recipients, LocalDateTime createTime, Long createBy,
                  LocalDateTime updateTime, Long updateBy) {
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
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
    }

    /** Copy with the persisted id set (for dispatch after insert). */
    public Notice withId(Long id) {
        return new Notice(id, title, content, targetType, targetTenantId, targetRoleCode, targetUsername, publishType,
                publishTime, effectiveTime, expireTime, status, needConfirm, senderId, tenantId, channels, recipients,
                createTime, createBy, updateTime, updateBy);
    }
}
