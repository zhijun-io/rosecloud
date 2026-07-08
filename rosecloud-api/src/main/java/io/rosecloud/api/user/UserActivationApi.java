package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rosecloud-system", contextId = "userActivationApi", path = "/internal/user-credentials")
public interface UserActivationApi {

    @GetMapping("/activation/{activateToken}")
    ApiResponse<UserActivationInfo> check(@PathVariable("activateToken") String activateToken);

    @PostMapping("/activation/confirm")
    ApiResponse<UserActivationInfo> confirm(@RequestBody ActivationConfirmRequest request);

    @PostMapping("/activation/resend")
    ApiResponse<UserActivationInfo> resend(@RequestBody ActivationResendRequest request);
}
