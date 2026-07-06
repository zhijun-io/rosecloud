package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public UserRepositoryImpl(UserMapper userMapper, UserRoleMapper userRoleMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public Optional<UserAuthInfo> findAuthInfo(String username) {
        UserPO po = userMapper.selectOne(
                new LambdaQueryWrapper<UserPO>().eq(UserPO::getUsername, username));
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(new UserAuthInfo(po.getId(), po.getUsername(), po.getPassword(),
                po.getStatus(), po.getTenantId(), loadRoleCodes(po.getId())));
    }

    private List<String> loadRoleCodes(Long userId) {
        List<UserRolePO> links = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRolePO>().eq(UserRolePO::getUserId, userId));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = links.stream().map(UserRolePO::getRoleId).toList();
        List<RolePO> roles = roleMapper.selectList(
                new LambdaQueryWrapper<RolePO>().in(RolePO::getId, roleIds));
        return roles.stream().map(RolePO::getCode).toList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.exists(new LambdaQueryWrapper<UserPO>().eq(UserPO::getUsername, username));
    }

    @Override
    public Long insert(User user, String passwordHash) {
        UserPO po = new UserPO();
        po.setUsername(user.username());
        po.setPassword(passwordHash);
        po.setNickname(user.nickname());
        po.setStatus(user.status());
        po.setTenantId(user.tenantId());
        userMapper.insert(po);
        return po.getId();
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<User> page(long current, long size, String keyword) {
        Page<UserPO> page = new Page<>(current, size);
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(UserPO::getUsername, keyword).or().like(UserPO::getNickname, keyword);
        }
        wrapper.orderByDesc(UserPO::getCreateTime);
        IPage<UserPO> result = userMapper.selectPage(page, wrapper);
        List<User> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }

    private User toDomain(UserPO po) {
        return new User(po.getId(), po.getUsername(), po.getNickname(), po.getStatus(), po.getTenantId());
    }
}
