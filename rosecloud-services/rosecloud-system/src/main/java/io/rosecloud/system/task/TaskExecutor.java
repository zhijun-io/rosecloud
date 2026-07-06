package io.rosecloud.system.task;

import io.rosecloud.system.domain.Task;
import io.rosecloud.system.domain.TaskRepository;
import io.rosecloud.system.domain.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs a task asynchronously: PENDING -> RUNNING -> SUCCESS/FAILED. Failures are
 * terminal until an explicit retry; the handler runs without a request context,
 * so handlers must not rely on {@code UserContext} (system-initiated work).
 */
@Component
public class TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutor.class);

    private final TaskRepository taskRepository;
    private final TaskHandlerRegistry handlerRegistry;

    public TaskExecutor(TaskRepository taskRepository, TaskHandlerRegistry handlerRegistry) {
        this.taskRepository = taskRepository;
        this.handlerRegistry = handlerRegistry;
    }

    @Async("rosecloudTaskExecutor")
    public void execute(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null || task.status() != TaskStatus.PENDING) {
            return;
        }
        taskRepository.markRunning(taskId);
        try {
            TaskHandler handler = handlerRegistry.get(task.type());
            String result = handler.execute(task.payload());
            taskRepository.markSuccess(taskId, result == null ? "" : result);
        } catch (Exception e) {
            log.warn("task {} (type={}) failed", taskId, task.type(), e);
            taskRepository.markFailed(taskId, e.getMessage());
        }
    }
}
