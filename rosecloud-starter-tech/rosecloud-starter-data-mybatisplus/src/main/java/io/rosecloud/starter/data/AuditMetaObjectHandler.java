package io.rosecloud.starter.data;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * Auto-fills audit fields ({@code createTime}/{@code updateTime}/
 * {@code createBy}/{@code updateBy}) on insert/update, taking the operator from
 * {@link UserContext}. No-ops when no user is bound (e.g. system/jobs).
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
        CurrentUser user = UserContext.get();
        return user == null ? null : user.userId();
    }
}
