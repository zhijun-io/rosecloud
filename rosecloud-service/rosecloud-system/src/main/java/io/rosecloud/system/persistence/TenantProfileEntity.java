package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent entity for {@code sys_tenant_profile}. */
@Getter
@Setter
@NoArgsConstructor
@TableName("sys_tenant_profile")
public class TenantProfileEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String name;
    private String description;
    @TableField("profile_data")
    private String additionalInfo;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isDefault;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    public boolean getIsDefault() { return isDefault; }

    public void setIsDefault(boolean isDefault) { this.isDefault = isDefault; }
}
