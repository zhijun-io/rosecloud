package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Task;
import io.rosecloud.system.domain.TaskStatus;
import io.rosecloud.system.service.dto.TaskCreateRequest;

public interface TaskService {

    Long create(TaskCreateRequest request);

    void retry(Long id);

    Task get(Long id);

    PageResult<Task> page(long current, long size, String type, TaskStatus status);
}
