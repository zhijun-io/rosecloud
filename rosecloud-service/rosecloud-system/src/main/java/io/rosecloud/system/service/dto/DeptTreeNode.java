package io.rosecloud.system.service.dto;

import io.rosecloud.system.domain.Dept;

import java.util.List;

/** Tree node for department responses: a dept plus its children. */
public record DeptTreeNode(Dept dept, List<DeptTreeNode> children) {
}
