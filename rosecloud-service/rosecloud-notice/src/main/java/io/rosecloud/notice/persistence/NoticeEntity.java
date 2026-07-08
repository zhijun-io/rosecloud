package io.rosecloud.notice.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

/** MyBatis-Plus persistent entity for {@code sys_notice}; confined to infrastructure. */
@TableName("sys_notice")
public class NoticeEntity extends BaseEntity {

    private String title;
    private String content;
    private Integer targetType;
    private Long targetTenantId;
    private String targetRoleCode;
    private Integer publishType;
    private LocalDateTime publishTime;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private Integer status;
    private Boolean needConfirm;
    private Long senderId;
    private Long tenantId;
    private Integer channels;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getTargetType() { return targetType; }
    public void setTargetType(Integer targetType) { this.targetType = targetType; }
    public Long getTargetTenantId() { return targetTenantId; }
    public void setTargetTenantId(Long targetTenantId) { this.targetTenantId = targetTenantId; }
    public String getTargetRoleCode() { return targetRoleCode; }
    public void setTargetRoleCode(String targetRoleCode) { this.targetRoleCode = targetRoleCode; }
    public Integer getPublishType() { return publishType; }
    public void setPublishType(Integer publishType) { this.publishType = publishType; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    public LocalDateTime getEffectiveTime() { return effectiveTime; }
    public void setEffectiveTime(LocalDateTime effectiveTime) { this.effectiveTime = effectiveTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Boolean getNeedConfirm() { return needConfirm; }
    public void setNeedConfirm(Boolean needConfirm) { this.needConfirm = needConfirm; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Integer getChannels() { return channels; }
    public void setChannels(Integer channels) { this.channels = channels; }
}
