package io.rosecloud.system.service.dto;

import java.util.Collections;
import java.util.List;

/** Current user's navigable menu tree plus the permission codes they hold. */
public record UserMenuResult(List<MenuTreeNode> menus, List<String> permissions) {

    public UserMenuResult {
        menus = menus == null ? Collections.emptyList() : List.copyOf(menus);
        permissions = permissions == null ? Collections.emptyList() : List.copyOf(permissions);
    }

    public static UserMenuResult empty() {
        return new UserMenuResult(Collections.emptyList(), Collections.emptyList());
    }
}
