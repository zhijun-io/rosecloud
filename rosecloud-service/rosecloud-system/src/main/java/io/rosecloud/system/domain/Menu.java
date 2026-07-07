package io.rosecloud.system.domain;

/** Domain view of a menu/permission node. ORM-free; mapped to/from {@code sys_menu}. */
public record Menu(Long id, Long parentId, String name, Integer type, String path,
                   String component, String perms, String icon, Integer sort,
                   Integer status, Integer visible) {
}
