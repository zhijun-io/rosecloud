package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "rosecloud-system", contextId = "userTenantApi", path = "/api/system/internal/user-tenants")
public interface UserTenantFeignApi extends UserTenantApi {

    @Override
    @GetMapping("/{userId}/tenants")
    ApiResponse<List<String>> listTenantIds(@PathVariable("userId") Long userId);

    @Override
    @GetMapping("/{userId}/platform-admin")
    ApiResponse<Boolean> isPlatformAdmin(@PathVariable("userId") Long userId);
}
