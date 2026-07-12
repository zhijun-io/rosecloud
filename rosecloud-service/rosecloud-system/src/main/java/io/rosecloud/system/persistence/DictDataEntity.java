package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code sys_dict_data}; confined to infrastructure. */
@TableName("sys_dict_data")
@Getter
@Setter
@NoArgsConstructor
public class DictDataEntity extends BaseEntity {

    private String dictCode;
    private String label;
    private String value;
    private Integer sort;
    private Integer status;
    private String remark;
}
