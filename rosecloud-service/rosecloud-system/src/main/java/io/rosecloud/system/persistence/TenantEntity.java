package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code sys_tenant}; confined to infrastructure. */
@Getter
@Setter
@NoArgsConstructor
@TableName("sys_tenant")
public class TenantEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String name;
    private Integer status;
    private String contactUser;
    private String contactPhone;
    private LocalDate expireTime;
    private String remark;
    private String tenantProfileId;
    private String extra;
    private String adminUsername;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableLogic
    private Integer deleted;
}
