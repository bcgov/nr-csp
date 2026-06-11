package ca.bc.gov.nrs.csp.backend.exception;

import ca.bc.gov.nrs.csp.backend.controller.HealthController;
import ca.bc.gov.nrs.csp.backend.service.HealthService;
import ca.bc.gov.nrs.csp.backend.service.mapper.HealthMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalApiExceptionHandlerTest {

    @Mock HealthService healthService;
    @Mock HealthMapper healthMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HealthController(healthService, healthMapper))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .build();
    }

    @Test
    void notFound_returns404_withApiErrorShape() throws Exception {
        given(healthService.getHealth()).willThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    void badRequest_returns400_withApiErrorShape() throws Exception {
        given(healthService.getHealth()).willThrow(new BadRequestException("Invalid input"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid input"));
    }

    @Test
    void unexpectedException_returns500_withApiErrorShape() throws Exception {
        given(healthService.getHealth()).willThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}
