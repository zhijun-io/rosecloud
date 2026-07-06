package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.Task;
import io.rosecloud.system.domain.TaskRepository;
import io.rosecloud.system.domain.TaskStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.TaskService;
import io.rosecloud.system.service.dto.TaskCreateRequest;
import io.rosecloud.system.task.TaskExecutor;
import io.rosecloud.system.task.TaskHandlerRegistry;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService {

    private static final int DEFAULT_MAX_RETRY = 3;

    private final TaskRepository taskRepository;
    private final TaskExecutor taskExecutor;
    private final TaskHandlerRegistry handlerRegistry;

    public TaskServiceImpl(TaskRepository taskRepository, TaskExecutor taskExecutor,
                           TaskHandlerRegistry handlerRegistry) {
        this.taskRepository = taskRepository;
        this.taskExecutor = taskExecutor;
        this.handlerRegistry = handlerRegistry;
    }

    @AuditLog(action = "task-create", description = "创建任务")
    @Override
    public Long create(TaskCreateRequest request) {
        handlerRegistry.get(request.type());
        Long taskTenantId = request.tenantId() != null ? request.tenantId() : scopeTenantId();
        Task task = new Task(null, request.name(), request.type(), TaskStatus.PENDING, taskTenantId,
                request.payload(), null, null, 0, DEFAULT_MAX_RETRY, null, null, null);
        Long id = taskRepository.insert(task);
        taskExecutor.execute(id);
        return id;
    }

    @AuditLog(action = "task-retry", description = "重试任务")
    @Override
    public void retry(Long id) {
        Task task = loadOwned(id);
        if (task.status() != TaskStatus.FAILED) {
            throw new BizException(SystemErrorCode.TASK_STATUS_INVALID);
        }
        if (task.retryCount() >= task.maxRetry()) {
            throw new BizException(SystemErrorCode.TASK_RETRY_EXCEEDED);
        }
        taskRepository.resetForRetry(id, task.retryCount() + 1);
        taskExecutor.execute(id);
    }

    @Override
    public Task get(Long id) {
        return loadOwned(id);
    }

    @Override
    public PageResult<Task> page(long current, long size, String type, TaskStatus status) {
        return taskRepository.page(current, size, type, status, scopeTenantId());
    }

    private Task loadOwned(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TASK_NOT_FOUND));
        Long scopeTenantId = scopeTenantId();
        if (scopeTenantId != null && !scopeTenantId.equals(task.tenantId())) {
            throw new BizException(SystemErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }

    /** Returns the caller's tenant id, or null for platform admins (who see all tasks). */
    private static Long scopeTenantId() {
        CurrentUser user = UserContext.get();
        return user == null ? null : user.tenantId();
    }
}
