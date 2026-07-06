package io.rosecloud.starter.tenant.async;

import io.rosecloud.starter.tenant.core.TenantContext;
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
        Long tenantId = TenantContext.getTenantId();
        return () -> {
            try {
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }
                runnable.run();
            } finally {
                TenantContext.clear();
            }
        };
    }
}
