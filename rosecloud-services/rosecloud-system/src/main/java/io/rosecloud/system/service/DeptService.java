package io.rosecloud.system.service;

import io.rosecloud.system.domain.Dept;
import io.rosecloud.system.service.dto.DeptRequest;
import io.rosecloud.system.service.dto.DeptTreeNode;

import java.util.List;

public interface DeptService {

    Long create(DeptRequest request);

    void update(Long id, DeptRequest request);

    void delete(Long id);

    List<Dept> list();

    List<DeptTreeNode> tree();
}
