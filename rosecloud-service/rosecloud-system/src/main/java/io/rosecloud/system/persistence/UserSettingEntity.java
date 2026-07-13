package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import io.rosecloud.common.core.model.ToData;
import io.rosecloud.common.core.model.ToEntity;
import io.rosecloud.system.domain.UserSetting;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@TableName("sys_user_setting")
@Getter
@Setter
@NoArgsConstructor
public class UserSettingEntity implements ToData<UserSetting>, ToEntity<UserSetting, UserSettingEntity> {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("setting_key")
    private String settingKey;

    private String value;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("updated_by")
    private Long updatedBy;

    @Override
    public UserSetting toData() {
        return new UserSetting(userId, settingKey, value, updatedAt, updatedBy);
    }

    @Override
    public UserSettingEntity toEntity(UserSetting setting) {
        setUserId(setting.getUserId());
        setSettingKey(setting.getKey());
        setValue(setting.getValue());
        setUpdatedAt(setting.getUpdatedAt());
        setUpdatedBy(setting.getUpdatedBy());
        return this;
    }
}
