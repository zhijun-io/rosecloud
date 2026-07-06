package io.rosecloud.system.domain;

/** Domain view of a dictionary item. ORM-free; mapped to/from {@code sys_dict_data}. */
public record DictData(Long id, String dictCode, String label, String value, Integer sort,
                       Integer status, String remark) {
}
