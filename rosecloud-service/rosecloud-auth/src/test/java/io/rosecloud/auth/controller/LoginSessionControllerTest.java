package io.rosecloud.auth.controller;

import io.rosecloud.auth.service.LoginSessionService;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.security.model.LoginSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginSessionControllerTest {

    @Test
    void clampsInvalidPageBounds() {
        LoginSessionService service = mock(LoginSessionService.class);
        when(service.findAll()).thenReturn(List.of(sampleSession("s1"), sampleSession("s2")));

        LoginSessionController controller = new LoginSessionController(service);

        ApiResponse<PagedData<LoginSession>> response = controller.online(new PageQuery(1, 1000));
        PagedData<LoginSession> result = response.data();

        assertThat(result.getData()).hasSize(2);
    }

    private static LoginSession sampleSession(String id) {
        return new LoginSession(
                id,
                "token-" + id,
                null,
                1L,
                "user",
                null,
                null,
                null,
                Instant.parse("2026-07-10T00:00:00Z"),
                Instant.parse("2026-07-17T00:00:00Z"));
    }
}
