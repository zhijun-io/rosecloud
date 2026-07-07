package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.Config;
import io.rosecloud.system.service.ConfigService;
import io.rosecloud.system.service.dto.ConfigRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/configs")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @PreAuthorize("hasAuthority('system:config:add')")
    @PostMapping
    public ApiResponse<Long> create(@RequestBody ConfigRequest request) {
        return ApiResponse.ok(configService.create(request));
    }

    @PreAuthorize("hasAuthority('system:config:edit')")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody ConfigRequest request) {
        configService.update(id, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:config:del')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:config:list')")
    @GetMapping("/{id}")
    public ApiResponse<Config> get(@PathVariable Long id) {
        return ApiResponse.ok(configService.get(id));
    }

    @PreAuthorize("hasAuthority('system:config:list')")
    @GetMapping("/keys/{key}")
    public ApiResponse<Config> getByKey(@PathVariable String key) {
        return ApiResponse.ok(configService.getByKey(key));
    }

    @PreAuthorize("hasAuthority('system:config:list')")
    @GetMapping
    public ApiResponse<PageResult<Config>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(configService.page(current, size, keyword));
    }
}
