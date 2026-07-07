package io.rosecloud.system.service.dto;

public record DictDataRequest(String dictCode, String label, String value, Integer sort,
                              Integer status, String remark) {
}
