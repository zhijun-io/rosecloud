package io.rosecloud.notice.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code sys_notice}; confined to infrastructure. */
@Getter
@Setter
@NoArgsConstructor
@TableName("sys_notice")
public class NoticeEntity extends BaseEntity {

    private String title;
    private String content;
    private Integer targetType;
    private String targetTenantId;
    private String targetRoleCode;
    private String targetUsername;
    private Integer publishType;
    private LocalDateTime publishTime;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private Integer status;
    private Boolean needConfirm;
    private Long senderId;
    private String tenantId;
    private Integer channels;
    private String recipientSnapshot;
}
