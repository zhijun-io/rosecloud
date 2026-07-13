package io.rosecloud.auth.controller;
import lombok.RequiredArgsConstructor;

import io.rosecloud.api.credential.CredentialChangeRequest;
import io.rosecloud.api.credential.CredentialSetRequest;
import io.rosecloud.auth.service.CredentialService;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.starter.security.annotation.InternalApi;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth-owned credential endpoints. {@code setPassword} / {@code changePassword} are internal:
 * the system service calls them on user create / activate / self-service change. Passwords are
 * validated and hashed here, never in the caller.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/credentials")
public class CredentialController {

    private final CredentialService credentialService;
    @InternalApi
    @PostMapping("/{userId}")
    public void setPassword(@PathVariable("userId") Long userId, @RequestBody CredentialSetRequest request) {
        credentialService.setPassword(userId, request.password());
    }

    @InternalApi
    @PutMapping("/{userId}")
    public void changePassword(@PathVariable("userId") Long userId, @RequestBody CredentialChangeRequest request) {
        credentialService.changePassword(userId, request.currentPassword(), request.newPassword());
    }
}
