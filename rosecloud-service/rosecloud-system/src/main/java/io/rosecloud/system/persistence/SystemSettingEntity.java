package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@TableName("sys_system_setting")
@Getter
@Setter
@NoArgsConstructor
public class SystemSettingEntity {

    @TableId(value = "setting_key", type = IdType.INPUT)
    private String settingKey;

    private String value;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("updated_by")
    private Long updatedBy;
}
