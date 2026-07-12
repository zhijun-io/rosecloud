package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code sys_menu}; confined to infrastructure. */
@TableName("sys_menu")
@Getter
@Setter
@NoArgsConstructor
public class MenuEntity extends BaseEntity {

    private Long parentId;
    private String name;
    private Integer type;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sort;
    private Integer status;
    private Integer visible;
}
