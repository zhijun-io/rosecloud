package io.rosecloud.system.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(String username, String password, String nickname,
                                @NotBlank @Size(max = 10) @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]{0,9}$")
                                String tenantId) {
}
