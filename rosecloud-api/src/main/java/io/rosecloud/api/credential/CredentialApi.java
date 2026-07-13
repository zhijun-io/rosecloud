package io.rosecloud.api.credential;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Auth-owned credential write API. Only the system service (and the auth self-service endpoints)
 * call this; it must never be exposed to external clients. Passwords are validated and hashed
 * inside auth, so callers pass raw passwords.
 */
@FeignClient(name = "rosecloud-auth", contextId = "credentialApi", path = "/api/credentials")
public interface CredentialApi {

    @PostMapping("/{userId}")
    void setPassword(@PathVariable("userId") Long userId, @RequestBody CredentialSetRequest request);

    @PutMapping("/{userId}")
    void changePassword(@PathVariable("userId") Long userId, @RequestBody CredentialChangeRequest request);
}
