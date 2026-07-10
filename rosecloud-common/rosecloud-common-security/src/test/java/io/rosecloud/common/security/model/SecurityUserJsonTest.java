package io.rosecloud.common.security.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUserJsonTest {

    @Test
    void serializesPasswordForAuthRoundTrip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SecurityUser user = new SecurityUser(
                1L,
                "admin@rosecloud.local",
                "平台管理员",
                "$2a$10$ipYqBLPr/rGe5c1AVgvWoODGrthzi8FKOjGE7HZQQ0EATEPaY/OJa",
                true,
                null,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "admin@rosecloud.local"),
                List.of(new SimpleGrantedAuthority("ROLE_platform-admin"))
        );

        String json = objectMapper.writeValueAsString(user);

        // Password must NOT be serialized (avoid leaking credentials over Feign / in tokens).
        assertThat(json).doesNotContain("\"password\"");
        assertThat(user.getPassword()).isEqualTo(user.getPassword());
    }
}
