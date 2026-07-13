package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.common.core.model.ToData;
import io.rosecloud.common.core.model.ToEntity;
import io.rosecloud.starter.data.BaseEntity;
import io.rosecloud.system.domain.Menu;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code sys_menu}; confined to infrastructure. */
@TableName("sys_menu")
@Getter
@Setter
@NoArgsConstructor
public class MenuEntity extends BaseEntity implements ToData<Menu>, ToEntity<Menu, MenuEntity> {

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

    @Override
    public Menu toData() {
        return new Menu(getId(), parentId, name, type, path, component, perms, icon, sort, status,
                visible, getCreateTime(), getCreateBy(), getUpdateTime(), getUpdateBy());
    }

    @Override
    public MenuEntity toEntity(Menu m) {
        setId(m.getId());
        setParentId(m.getParentId());
        setName(m.getName());
        setType(m.getType());
        setPath(m.getPath());
        setComponent(m.getComponent());
        setPerms(m.getPerms());
        setIcon(m.getIcon());
        setSort(m.getSort());
        setStatus(m.getStatus());
        setVisible(m.getVisible());
        setCreateTime(m.getCreateTime());
        setCreateBy(m.getCreateBy());
        setUpdateTime(m.getUpdateTime());
        setUpdateBy(m.getUpdateBy());
        return this;
    }
}
