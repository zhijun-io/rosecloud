package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

import java.util.Optional;

/** Repository port for tasks. Implemented in the infrastructure layer; service depends only on this. */
public interface TaskRepository {

    Long insert(Task task);

    Optional<Task> findById(Long id);

    void markRunning(Long id);

    void markSuccess(Long id, String result);

    void markFailed(Long id, String error);

    void resetForRetry(Long id, int newRetryCount);

    PageResult<Task> page(long current, long size, String type, TaskStatus status, Long tenantId);
}
