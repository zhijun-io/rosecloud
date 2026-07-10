package io.rosecloud.starter.web.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.error.CommonErrorCode;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailureController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()))
                .build();
    }

    @Test
    void wrapsBizExceptionAsApiResponse() throws Exception {
        mockMvc.perform(get("/biz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.message").value("bad request"));
    }

    @Test
    void wrapsAccessDeniedAsApiResponse() throws Exception {
        mockMvc.perform(get("/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.FORBIDDEN.code()))
                .andExpect(jsonPath("$.message").value(SecurityErrorCode.FORBIDDEN.message()));
    }

    @Test
    void wrapsUnexpectedErrorAsApiResponse() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INTERNAL_ERROR.code()))
                .andExpect(jsonPath("$.message").value(CommonErrorCode.INTERNAL_ERROR.message()));
    }

    @Test
    void wrapsMissingRequestParameterAsApiResponse() throws Exception {
        mockMvc.perform(get("/missing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void wrapsTypeMismatchAsApiResponse() throws Exception {
        mockMvc.perform(get("/type-mismatch").param("id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.message").exists());
    }

    @RestController
    static class FailureController {
        @GetMapping(value = "/biz", produces = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> biz() {
            throw new BizException(CommonErrorCode.PARAM_INVALID, "bad request");
        }

        @GetMapping(value = "/denied", produces = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> denied() {
            throw new AccessDeniedException("denied");
        }

        @GetMapping(value = "/boom", produces = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> boom() {
            throw new IllegalStateException("boom");
        }

        @GetMapping(value = "/missing", produces = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> missing(@RequestParam String sessionId) {
            return ApiResponse.ok();
        }

        @GetMapping(value = "/type-mismatch", produces = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> typeMismatch(@RequestParam Long id) {
            return ApiResponse.ok();
        }
    }
}
