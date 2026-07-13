package io.rosecloud.system.service;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.persistence.TenantEntity;
import io.rosecloud.system.persistence.TenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Periodically checks for tenants whose {@code expireTime} has passed and whose
 * status is still {@code ENABLED}, then transitions them to {@code EXPIRED}.
 * This ensures the persisted status reflects the business constraint documented
 * in the PRD: "租户到期后自动进入停用状态".
 *
 * <p>Write guard ({@link io.rosecloud.starter.security.web.TenantWriteGuardFilter})
 * and dynamic status resolution ({@link TenantStatus#resolve}) both
 * recognise {@code EXPIRED} as read-only, so expired tenants are blocked from
 * mutating operations even before this scheduler runs — but the scheduler closes
 * the gap between in-memory resolution and persistent state.
 */
@RequiredArgsConstructor
@Component
public class TenantExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(TenantExpiryScheduler.class);

    private final TenantMapper tenantMapper;
    /**
     * Runs every hour at the top of the minute. Scans for tenants whose
     * {@code expireTime} is before today and whose status is still
     * {@code ENABLED} (1), and updates them to {@code EXPIRED} (3).
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireOverdueTenants() {
        int updated = tenantMapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                .eq(TenantEntity::getStatus, TenantStatus.ENABLED.code())
                .lt(TenantEntity::getExpireTime, LocalDate.now())
                .set(TenantEntity::getStatus, TenantStatus.EXPIRED.code()));
        if (updated > 0) {
            log.info("已自动将 {} 个到期租户置为停用(EXPIRED)状态", updated);
        }
    }
}
