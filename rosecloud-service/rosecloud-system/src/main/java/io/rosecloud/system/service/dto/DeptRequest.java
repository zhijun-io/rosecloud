package io.rosecloud.system.service.dto;

public record DeptRequest(Long parentId, String name, Integer sort, Integer status,
                          String leader, String phone) {
}
