package io.rosecloud.notice.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code sys_notice_record}; confined to infrastructure. */
@Getter
@Setter
@NoArgsConstructor
@TableName("sys_notice_record")
public class NoticeRecordEntity extends BaseEntity {

    private Long noticeId;
    private Long userId;
    private String tenantId;
    private LocalDateTime readTime;
    private LocalDateTime confirmTime;
}
