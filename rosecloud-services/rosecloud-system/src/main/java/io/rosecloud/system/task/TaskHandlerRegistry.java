package io.rosecloud.system.task;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.error.SystemErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/** Indexes {@link TaskHandler} beans by type for runtime lookup. */
@Component
public class TaskHandlerRegistry {

    private final Map<String, TaskHandler> handlers;

    public TaskHandlerRegistry(List<TaskHandler> handlers) {
        this.handlers = handlers.stream().collect(toMap(TaskHandler::type, Function.identity()));
    }

    public TaskHandler get(String type) {
        TaskHandler handler = handlers.get(type);
        if (handler == null) {
            throw new BizException(SystemErrorCode.TASK_HANDLER_NOT_FOUND);
        }
        return handler;
    }
}
