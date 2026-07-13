package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
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

@RequiredArgsConstructor
@Service
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final EntityCache<Long, Dept> deptCache;
    private final EntityCache<String, List<Dept>> deptListCache;
    private final EntityEventPublisher eventPublisher;
    @AuditLog(action = "dept-create", description = "创建部门")
    @Override
    public Long create(DeptRequest request) {
        DeptEntity po = new DeptEntity().toEntity(toDept(null, request));
        po.setId(null);
        deptMapper.insert(po);
        deptListCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.created(
                EntityCacheNames.DEPT, po.getId(), null, null));
        return po.getId();
    }

    @AuditLog(action = "dept-update", description = "修改部门")
    @Override
    public void update(Long id, DeptRequest request) {
        findById(id).orElseThrow(() -> new BizException(SystemErrorCode.DEPT_NOT_FOUND));
        deptMapper.updateById(new DeptEntity().toEntity(toDept(id, request)));
        deptCache.evict(id);
        deptListCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.DEPT, id, null, null, null));
    }

    @AuditLog(action = "dept-delete", description = "删除部门")
    @Override
    public void delete(Long id) {
        if (deptMapper.exists(new LambdaQueryWrapper<DeptEntity>().eq(DeptEntity::getParentId, id))) {
            throw new BizException(SystemErrorCode.DEPT_HAS_CHILDREN);
        }
        deptMapper.deleteById(id);
        deptCache.evict(id);
        deptListCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.deleted(
                EntityCacheNames.DEPT, id, null, null));
    }

    @Override
    public List<Dept> list() {
        return deptListCache.getOrLoad("__all__", () ->
                deptMapper.selectList(new LambdaQueryWrapper<DeptEntity>()
                                .orderByAsc(DeptEntity::getSort)
                                .orderByAsc(DeptEntity::getId))
                        .stream().map(DeptEntity::toData).toList()
        );
    }

    @Override
    public List<DeptTreeNode> tree() {
        List<Dept> depts = list();
        Map<Long, List<Dept>> byParent = depts.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        return buildChildren(byParent, 0L);
    }

    private Optional<Dept> findById(Long id) {
        Dept cached = deptCache.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        return Optional.ofNullable(deptMapper.selectById(id)).map(po -> {
            Dept d = po.toData();
            deptCache.put(id, d);
            return d;
        });
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
        return Dept.of(id, parentId, request.name(), sort, status, request.leader(), request.phone());
    }

}
