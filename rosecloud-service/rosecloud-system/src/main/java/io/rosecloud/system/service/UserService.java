package io.rosecloud.system.service;

import io.rosecloud.api.user.UserPasswordUpdateRequest;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.service.dto.ChangePasswordRequest;
import io.rosecloud.system.service.dto.UserCreateRequest;
import io.rosecloud.system.service.dto.UserProfile;

import java.util.List;

public interface UserService {

    Long create(UserCreateRequest request);

    Long createWithoutPassword(String username, String nickname, String tenantId);

    PagedData<User> page(PageQuery pageQuery);

    User get(Long id);

    void delete(Long id);

    SecurityUser loadByUsername(String username);

    void changePassword(ChangePasswordRequest request);

    void updatePassword(Long userId, UserPasswordUpdateRequest request);

    void assignRoles(Long userId, List<Long> roleIds);

    List<Long> findRoleIdsByUserId(Long userId);

    UserProfile me();
}
