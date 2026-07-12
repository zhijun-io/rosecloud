package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@TableName("sys_setting_key")
@Getter
@Setter
@NoArgsConstructor
public class SettingKeyEntity extends BaseEntity {

    private String key;
    private String name;
    private String remark;
}
