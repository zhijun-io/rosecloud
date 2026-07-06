package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.Task;
import io.rosecloud.system.domain.TaskStatus;
import io.rosecloud.system.service.TaskService;
import io.rosecloud.system.service.dto.TaskCreateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody TaskCreateRequest request) {
        return ApiResponse.ok(taskService.create(request));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<Void> retry(@PathVariable Long id) {
        taskService.retry(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}")
    public ApiResponse<Task> get(@PathVariable Long id) {
        return ApiResponse.ok(taskService.get(id));
    }

    @GetMapping
    public ApiResponse<PageResult<Task>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) TaskStatus status) {
        return ApiResponse.ok(taskService.page(current, size, type, status));
    }
}
