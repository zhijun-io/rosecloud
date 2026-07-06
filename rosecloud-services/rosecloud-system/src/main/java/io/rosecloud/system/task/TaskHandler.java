package io.rosecloud.system.task;

/**
 * SPI for executing a task type. Implementations register as Spring beans; the
 * {@link TaskHandlerRegistry} indexes them by {@link #type()}. Add a new task
 * type by implementing this and declaring a matching {@code TaskTypes} constant.
 */
public interface TaskHandler {

    /** Type code this handler serves; must match a {@code TaskTypes} constant. */
    String type();

    /**
     * Executes the task. The {@code payload} is the raw JSON string stored on
     * the task row.
     *
     * @return a short result description recorded on the task; never {@code null}
     * @throws Exception on failure; the task is marked failed and becomes retryable
     */
    String execute(String payload) throws Exception;
}
