package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.service.R12Service;
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
class R12ControllerTest {

    @Mock
    R12Service r12Service;

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
                .standaloneSetup(new R12Controller(r12Service))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .setValidator(validator)
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper),
                        new ResourceHttpMessageConverter(),
                        new ByteArrayHttpMessageConverter())
                .build();
    }

    @Nested
    @DisplayName("POST /api/R12")
    class PostR12 {

        @Test
        void shouldReturn200_whenValidRequest() throws Exception {
            given(r12Service.generateReport(any()))
                    .willReturn(new ReportResult(new byte[]{1}, "R12_test.pdf"));

            mockMvc.perform(post("/api/R12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"year\":2020}"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"R12_test.pdf\""));
        }

        @Test
        void shouldReturn400_whenDateFromInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/R12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"dateFrom\":\"January-2020\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenDateToInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/R12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"dateTo\":\"2020-12\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn200_whenDateRangeValid() throws Exception {
            given(r12Service.generateReport(any()))
                    .willReturn(new ReportResult(new byte[]{1}, "R12_test.pdf"));

            mockMvc.perform(post("/api/R12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"dateFrom\":\"20200101\",\"dateTo\":\"20201231\"}"))
                    .andExpect(status().isOk());
        }
    }
}
