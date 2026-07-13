package io.rosecloud.api.user;

import io.rosecloud.common.security.model.SecurityUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "rosecloud-system", contextId = "userApi", path = "/api/users")
public interface UserFeignApi extends UserApi {

    @Override
    @GetMapping("/auth/{username}")
    SecurityUser loadUserByUsername(@PathVariable("username") String username);

}
