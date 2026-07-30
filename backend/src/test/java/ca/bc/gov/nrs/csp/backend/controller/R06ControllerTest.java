package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.service.R06Service;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class R06ControllerTest {

    @Mock
    R06Service r06Service;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new R06Controller(r06Service))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .setValidator(validator)
                .setMessageConverters(
                        new JacksonJsonHttpMessageConverter(),
                        new ResourceHttpMessageConverter(),
                        new ByteArrayHttpMessageConverter())
                .build();
    }

    @Nested
    @DisplayName("POST /api/R06")
    class PostR06 {

        @Test
        void shouldReturn200_whenValidRequest() throws Exception {
            given(r06Service.generateReport(any()))
                    .willReturn(new ReportResult(new byte[]{1, 2, 3}, "R06_test.pdf"));

            mockMvc.perform(post("/api/R06")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\"}"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"R06_test.pdf\""))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }

        @Test
        void shouldReturn400_whenDateFromInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/R06")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"dateFrom\":\"2020-01-01\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenDateToInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/R06")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"dateTo\":\"01/2020\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn200_whenCsvFormat() throws Exception {
            given(r06Service.generateReport(any()))
                    .willReturn(new ReportResult(new byte[]{1}, "R06_test.csv"));

            mockMvc.perform(post("/api/R06")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"CSV\"}"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"R06_test.csv\""));
        }
    }
}
