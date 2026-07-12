package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code sys_dict_type}; confined to infrastructure. */
@TableName("sys_dict_type")
@Getter
@Setter
@NoArgsConstructor
public class DictTypeEntity extends BaseEntity {

    private String code;
    private String name;
    private Integer status;
    private String remark;
}
