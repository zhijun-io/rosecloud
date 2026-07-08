package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.service.SettingKeyService;
import io.rosecloud.system.service.dto.SettingKeyCreateRequest;
import io.rosecloud.system.service.dto.SettingKeyUpdateRequest;
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
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/setting-keys")
public class SettingKeyController {

    private final SettingKeyService settingKeyService;

    public SettingKeyController(SettingKeyService settingKeyService) {
        this.settingKeyService = settingKeyService;
    }

    @PreAuthorize("hasAuthority('system:setting-key:add')")
    @PostMapping
    public ApiResponse<String> create(@RequestBody SettingKeyCreateRequest request) {
        return ApiResponse.ok(settingKeyService.create(request));
    }

    @PreAuthorize("hasAuthority('system:setting-key:edit')")
    @PutMapping("/{key}")
    public ApiResponse<Void> update(@PathVariable String key, @RequestBody SettingKeyUpdateRequest request) {
        settingKeyService.update(key, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:setting-key:del')")
    @DeleteMapping("/{key}")
    public ApiResponse<Void> delete(@PathVariable String key) {
        settingKeyService.delete(key);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:setting-key:list')")
    @GetMapping("/{key}")
    public ApiResponse<SettingKey> get(@PathVariable String key) {
        return ApiResponse.ok(settingKeyService.get(key));
    }

    @PreAuthorize("hasAuthority('system:setting-key:list')")
    @GetMapping
    public ApiResponse<PageResult<SettingKey>> page(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(settingKeyService.page(current, size, keyword));
    }
}
