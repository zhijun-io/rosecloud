package io.rosecloud.system.domain;

/** Domain view of a dictionary type. ORM-free; mapped to/from {@code sys_dict_type}. */
public record DictType(Long id, String code, String name, Integer status, String remark) {
}
