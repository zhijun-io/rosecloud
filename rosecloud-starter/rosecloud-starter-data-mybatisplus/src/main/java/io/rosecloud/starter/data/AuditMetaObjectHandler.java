package io.rosecloud.starter.data;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.rosecloud.common.security.model.SecurityUser;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

/**
 * Auto-fills audit fields ({@code createTime}, {@code updateTime},
 * {@code createBy}, {@code updateBy}) on insert/update, taking the operator
 * from {@link SecurityContextHolder#getContext()}. No-ops when no user is
 * bound (e.g. system/jobs).
 */
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        Long userId = currentUserId();
        if (userId != null) {
            strictInsertFill(metaObject, "createBy", Long.class, userId);
            strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        Long userId = currentUserId();
        if (userId != null) {
            strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
    }

    private static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getUserId();
        }
        return null;
    }
}
