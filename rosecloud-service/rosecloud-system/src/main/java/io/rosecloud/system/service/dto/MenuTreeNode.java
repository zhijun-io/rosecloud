package io.rosecloud.system.service.dto;

import io.rosecloud.system.domain.Menu;

import java.util.List;

/** Tree node for menu responses: a menu plus its children. */
public record MenuTreeNode(Menu menu, List<MenuTreeNode> children) {
}
