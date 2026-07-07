package io.rosecloud.system.domain;

/** Domain view of a department/org node. ORM-free; mapped to/from {@code sys_dept}. */
public record Dept(Long id, Long parentId, String name, Integer sort, Integer status,
                   String leader, String phone) {
}
