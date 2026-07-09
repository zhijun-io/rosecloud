package io.rosecloud.api.log;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rosecloud-system", contextId = "loginLogApi", path = "/api/system/login-logs")
public interface LoginLogFeignApi extends LoginLogApi {

    @Override
    @PostMapping
    void record(@RequestBody LoginLogRequest request);
}
