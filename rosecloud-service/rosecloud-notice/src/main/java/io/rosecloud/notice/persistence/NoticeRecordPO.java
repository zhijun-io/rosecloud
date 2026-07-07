package io.rosecloud.notice.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

/** MyBatis-Plus persistent object for {@code sys_notice_record}; confined to infrastructure. */
@TableName("sys_notice_record")
public class NoticeRecordPO extends BaseEntity {

    private Long noticeId;
    private Long userId;
    private Long tenantId;
    private LocalDateTime readTime;
    private LocalDateTime confirmTime;

    public Long getNoticeId() { return noticeId; }
    public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getReadTime() { return readTime; }
    public void setReadTime(LocalDateTime readTime) { this.readTime = readTime; }
    public LocalDateTime getConfirmTime() { return confirmTime; }
    public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }
}
