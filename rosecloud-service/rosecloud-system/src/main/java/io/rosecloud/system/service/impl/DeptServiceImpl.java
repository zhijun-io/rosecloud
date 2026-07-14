package io.rosecloud.system.service.impl;

import lombok.RequiredArgsConstructor;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.Dept;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.DeptDao;
import io.rosecloud.system.service.DeptService;
import io.rosecloud.system.service.dto.DeptRequest;
import io.rosecloud.system.service.dto.DeptTreeNode;
import io.rosecloud.system.service.validator.DeptValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DeptServiceImpl implements DeptService {

    private final DeptDao deptDao;
    private final DeptValidator deptValidator;
    private final EntityCache<Long, Dept> deptCache;
    private final EntityCache<String, List<Dept>> deptListCache;
    private final EntityEventPublisher eventPublisher;

    @AuditLog(action = "dept-create", description = "创建部门")
    @Transactional
    @Override
    public Long create(DeptRequest request) {
        Dept dept = toDept(null, request);
        deptValidator.validateCreate(dept);
        Dept saved = deptDao.save(dept);
        deptListCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.created(
                EntityCacheNames.DEPT, saved.getId(), null, null));
        return saved.getId();
    }

    @AuditLog(action = "dept-update", description = "修改部门")
    @Transactional
    @Override
    public void update(Long id, DeptRequest request) {
        Dept existing = findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.DEPT_NOT_FOUND));
        Dept updated = toDept(id, request);
        deptValidator.validateUpdate(updated, Optional.of(existing));
        deptDao.save(updated);
        // 单实体缓存由 CacheEvictionListener 在事务提交后失效；列表缓存需显式清空。
        deptListCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.DEPT, id, null, null, null));
    }

    @AuditLog(action = "dept-delete", description = "删除部门")
    @Transactional
    @Override
    public void delete(Long id) {
        if (deptDao.existsByParentId(id)) {
            throw new BizException(SystemErrorCode.DEPT_HAS_CHILDREN);
        }
        deptDao.removeById(id);
        deptListCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.deleted(
                EntityCacheNames.DEPT, id, null, null));
    }

    @Override
    public List<Dept> list() {
        return deptListCache.getOrLoad("__all__", () ->
                deptDao.findAllOrderBySort()
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
        return Optional.ofNullable(deptCache.getOrLoadTransactional(id, () ->
                deptDao.findById(id).orElse(null)
        ));
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
