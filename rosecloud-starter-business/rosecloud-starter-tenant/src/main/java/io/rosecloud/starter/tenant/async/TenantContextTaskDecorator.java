package io.rosecloud.starter.tenant.async;

import io.rosecloud.starter.tenant.core.TenantContextHolder;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/**
 * Propagates the current tenant id onto async/thread-pool work by capturing it
 * at submission time and restoring it on the worker thread.
 */
public class TenantContextTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        String tenantId = TenantContextHolder.getTenantId();
        return () -> {
            try {
                if (tenantId != null) {
                    TenantContextHolder.setTenantId(tenantId);
                }
                runnable.run();
            } finally {
                TenantContextHolder.clear();
            }
        };
    }
}
