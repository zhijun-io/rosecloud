package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Task;
import io.rosecloud.system.domain.TaskRepository;
import io.rosecloud.system.domain.TaskStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskMapper mapper;

    public TaskRepositoryImpl(TaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Long insert(Task task) {
        TaskPO po = toPO(task);
        po.setId(null);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public Optional<Task> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public void markRunning(Long id) {
        LocalDateTime now = LocalDateTime.now();
        mapper.update(null, new LambdaUpdateWrapper<TaskPO>()
                .eq(TaskPO::getId, id)
                .set(TaskPO::getStatus, TaskStatus.RUNNING.code())
                .set(TaskPO::getStartedAt, now)
                .set(TaskPO::getUpdateTime, now));
    }

    @Override
    public void markSuccess(Long id, String result) {
        LocalDateTime now = LocalDateTime.now();
        mapper.update(null, new LambdaUpdateWrapper<TaskPO>()
                .eq(TaskPO::getId, id)
                .set(TaskPO::getStatus, TaskStatus.SUCCESS.code())
                .set(TaskPO::getResult, result)
                .set(TaskPO::getFinishedAt, now)
                .set(TaskPO::getUpdateTime, now));
    }

    @Override
    public void markFailed(Long id, String error) {
        LocalDateTime now = LocalDateTime.now();
        mapper.update(null, new LambdaUpdateWrapper<TaskPO>()
                .eq(TaskPO::getId, id)
                .set(TaskPO::getStatus, TaskStatus.FAILED.code())
                .set(TaskPO::getError, error)
                .set(TaskPO::getFinishedAt, now)
                .set(TaskPO::getUpdateTime, now));
    }

    @Override
    public void resetForRetry(Long id, int newRetryCount) {
        LocalDateTime now = LocalDateTime.now();
        mapper.update(null, new LambdaUpdateWrapper<TaskPO>()
                .eq(TaskPO::getId, id)
                .set(TaskPO::getStatus, TaskStatus.PENDING.code())
                .set(TaskPO::getRetryCount, newRetryCount)
                .set(TaskPO::getStartedAt, null)
                .set(TaskPO::getFinishedAt, null)
                .set(TaskPO::getError, null)
                .set(TaskPO::getResult, null)
                .set(TaskPO::getUpdateTime, now));
    }

    @Override
    public PageResult<Task> page(long current, long size, String type, TaskStatus status, Long tenantId) {
        Page<TaskPO> page = new Page<>(current, size);
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(TaskPO::getType, type);
        }
        if (status != null) {
            wrapper.eq(TaskPO::getStatus, status.code());
        }
        if (tenantId != null) {
            wrapper.eq(TaskPO::getTenantId, tenantId);
        }
        wrapper.orderByDesc(TaskPO::getCreateTime);
        IPage<TaskPO> result = mapper.selectPage(page, wrapper);
        List<Task> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Task toDomain(TaskPO po) {
        return new Task(po.getId(), po.getName(), po.getType(),
                po.getStatus() == null ? null : TaskStatus.of(po.getStatus()),
                po.getTenantId(), po.getPayload(), po.getResult(), po.getError(),
                po.getRetryCount() == null ? 0 : po.getRetryCount(),
                po.getMaxRetry() == null ? 0 : po.getMaxRetry(),
                po.getStartedAt(), po.getFinishedAt(), po.getCreateTime());
    }

    private TaskPO toPO(Task t) {
        TaskPO po = new TaskPO();
        po.setId(t.id());
        po.setName(t.name());
        po.setType(t.type());
        po.setStatus(t.status().code());
        po.setTenantId(t.tenantId());
        po.setPayload(t.payload());
        po.setResult(t.result());
        po.setError(t.error());
        po.setRetryCount(t.retryCount());
        po.setMaxRetry(t.maxRetry());
        po.setStartedAt(t.startedAt());
        po.setFinishedAt(t.finishedAt());
        return po;
    }
}
