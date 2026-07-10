package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.Dept;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.DeptEntity;
import io.rosecloud.system.persistence.DeptMapper;
import io.rosecloud.system.service.DeptService;
import io.rosecloud.system.service.dto.DeptRequest;
import io.rosecloud.system.service.dto.DeptTreeNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;

    public DeptServiceImpl(DeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    @AuditLog(action = "dept-create", description = "创建部门")
    @Override
    public Long create(DeptRequest request) {
        DeptEntity po = toEntity(toDept(null, request));
        po.setId(null);
        deptMapper.insert(po);
        return po.getId();
    }

    @AuditLog(action = "dept-update", description = "修改部门")
    @Override
    public void update(Long id, DeptRequest request) {
        findById(id).orElseThrow(() -> new BizException(SystemErrorCode.DEPT_NOT_FOUND));
        deptMapper.updateById(toEntity(toDept(id, request)));
    }

    @AuditLog(action = "dept-delete", description = "删除部门")
    @Override
    public void delete(Long id) {
        if (deptMapper.exists(new LambdaQueryWrapper<DeptEntity>().eq(DeptEntity::getParentId, id))) {
            throw new BizException(SystemErrorCode.DEPT_HAS_CHILDREN);
        }
        deptMapper.deleteById(id);
    }

    @Override
    public List<Dept> list() {
        return deptMapper.selectList(new LambdaQueryWrapper<DeptEntity>()
                        .orderByAsc(DeptEntity::getSort)
                        .orderByAsc(DeptEntity::getId)).stream().map(this::toDomain).toList();
    }

    @Override
    public List<DeptTreeNode> tree() {
        List<Dept> depts = list();
        Map<Long, List<Dept>> byParent = depts.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        return buildChildren(byParent, 0L);
    }

    private Optional<Dept> findById(Long id) {
        return Optional.ofNullable(deptMapper.selectById(id)).map(this::toDomain);
    }

    private List<DeptTreeNode> buildChildren(Map<Long, List<Dept>> byParent, Long parentId) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(d -> new DeptTreeNode(d, buildChildren(byParent, d.getId())))
                .toList();
    }

    private Dept toDept(Long id, DeptRequest request) {
        long parentId = request.parentId() == null ? 0L : request.parentId();
        int sort = request.sort() == null ? 0 : request.sort();
        int status = request.status() == null ? 1 : request.status();
        return new Dept(id, parentId, request.name(), sort, status, request.leader(), request.phone());
    }

    private Dept toDomain(DeptEntity po) {
        return new Dept(po.getId(), po.getParentId(), po.getName(), po.getSort(),
                po.getStatus(), po.getLeader(), po.getPhone(), po.getCreateTime(), po.getCreateBy(),
                po.getUpdateTime(), po.getUpdateBy());
    }

    private DeptEntity toEntity(Dept d) {
        DeptEntity po = new DeptEntity();
        po.setId(d.getId());
        po.setParentId(d.getParentId());
        po.setName(d.getName());
        po.setSort(d.getSort());
        po.setStatus(d.getStatus());
        po.setLeader(d.getLeader());
        po.setPhone(d.getPhone());
        po.setCreateTime(d.getCreateTime());
        po.setCreateBy(d.getCreateBy());
        po.setUpdateTime(d.getUpdateTime());
        po.setUpdateBy(d.getUpdateBy());
        return po;
    }
}
