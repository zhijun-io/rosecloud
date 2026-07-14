package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DAO for user-tenant cross-reference records.
 * Does not extend {@code MyBatisDao} as {@link UserTenantEntity} has no domain abstraction.
 */
@Repository
public class UserTenantDao {

    private final UserTenantMapper mapper;

    public UserTenantDao(UserTenantMapper mapper) {
        this.mapper = mapper;
    }

    public List<String> findTenantIdsByUserId(Long userId) {
        return mapper.selectList(
                        new LambdaQueryWrapper<UserTenantEntity>().eq(UserTenantEntity::getUserId, userId))
                .stream().map(UserTenantEntity::getTenantId).toList();
    }
}
