package io.rosecloud.system.service.dto;

public record MenuRequest(Long parentId, String name, Integer type, String path, String component,
                          String perms, String icon, Integer sort, Integer status, Integer visible) {
}
