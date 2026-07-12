package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code sys_dept}; confined to infrastructure. */
@TableName("sys_dept")
@Getter
@Setter
@NoArgsConstructor
public class DeptEntity extends BaseEntity {

    private Long parentId;
    private String name;
    private Integer sort;
    private Integer status;
    private String leader;
    private String phone;
}
