package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.service.R10Service;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class R10ControllerTest {

    @Mock
    R10Service r10Service;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new R10Controller(r10Service))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .setValidator(validator)
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper),
                        new ResourceHttpMessageConverter(),
                        new ByteArrayHttpMessageConverter())
                .build();
    }

    @Nested
    @DisplayName("POST /api/R10")
    class PostR10 {

        @Test
        void shouldReturn200_whenValidRequest() throws Exception {
            given(r10Service.generateReport(any()))
                    .willReturn(new ReportResult(new byte[]{1}, "R10_test.pdf"));

            mockMvc.perform(post("/api/R10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"dateFrom\":\"20200101\",\"dateTo\":\"20201231\"}"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"R10_test.pdf\""));
        }

        @Test
        void shouldReturn400_whenDateFromMissing() throws Exception {
            mockMvc.perform(post("/api/R10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"dateTo\":\"20201231\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenDateFromInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/R10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"dateFrom\":\"2020-01-01\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenTimeFrameNotNumeric() throws Exception {
            mockMvc.perform(post("/api/R10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"dateFrom\":\"20200101\",\"timeFrame\":\"three\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
}
