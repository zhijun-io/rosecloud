package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.JsonNode;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

import io.rosecloud.common.core.model.ToData;
import io.rosecloud.common.core.model.ToEntity;
import io.rosecloud.common.core.util.JacksonUtil;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.domain.TenantProfileData;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent entity for {@code sys_tenant_profile}. */
@Getter
@Setter
@NoArgsConstructor
@TableName("sys_tenant_profile")
public class TenantProfileEntity implements ToData<TenantProfile>, ToEntity<TenantProfile, TenantProfileEntity> {

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

    @Override
    public TenantProfile toData() {
        JsonNode info = (additionalInfo == null || additionalInfo.isBlank())
                ? JacksonUtil.valueToTree(TenantProfileData.defaults())
                : JacksonUtil.toJsonNode(additionalInfo);
        return new TenantProfile(getId(), name, description, getIsDefault(),
                info, createTime, createBy, updateTime, updateBy);
    }

    @Override
    public TenantProfileEntity toEntity(TenantProfile profile) {
        setId(profile.getId());
        setName(profile.getName());
        setDescription(profile.getDescription());
        setAdditionalInfo(JacksonUtil.writeString(profile.getAdditionalInfo()));
        setIsDefault(profile.isDefault());
        setCreateTime(profile.getCreateTime());
        setCreateBy(profile.getCreateBy());
        setUpdateTime(profile.getUpdateTime());
        setUpdateBy(profile.getUpdateBy());
        return this;
    }
}
