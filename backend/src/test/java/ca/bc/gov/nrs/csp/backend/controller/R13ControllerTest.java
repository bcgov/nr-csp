package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.service.R13Service;
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
class R13ControllerTest {

    @Mock
    R13Service r13Service;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new R13Controller(r13Service))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .setValidator(validator)
                .setMessageConverters(
                        new JacksonJsonHttpMessageConverter(),
                        new ResourceHttpMessageConverter(),
                        new ByteArrayHttpMessageConverter())
                .build();
    }

    private static final String VALID_BODY = """
            {
              "reportFormat": "PDF",
              "reportName": "Test R13",
              "invoiceDateFrom": "20200101",
              "showOptions": {
                "showInvoiceNumber": true,
                "showSellerName": true
              }
            }
            """;

    @Nested
    @DisplayName("POST /api/R13")
    class PostR13 {

        @Test
        void shouldReturn200_whenValidRequest() throws Exception {
            given(r13Service.generateReport(any()))
                    .willReturn(new ReportResult(new byte[]{1}, "R13_test.pdf"));

            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"R13_test.pdf\""));
        }

        @Test
        void shouldReturn400_whenReportNameMissing() throws Exception {
            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"invoiceDateFrom\":\"20200101\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenInvoiceDateFromInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"reportName\":\"Test\",\"invoiceDateFrom\":\"2020/01/01\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenSubmissionNumberNotNumeric() throws Exception {
            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"reportName\":\"Test\",\"submissionNumber\":\"SUB-001\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenTimeFrameIsNonNumeric() throws Exception {
            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"reportName\":\"Test\",\"invoiceDateFrom\":\"20200101\",\"timeFrame\":\"abc\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenTimeFrameExceeds12() throws Exception {
            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"reportName\":\"Test\",\"invoiceDateFrom\":\"20200101\",\"timeFrame\":\"13\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenInvoiceNumberExceeds15Chars() throws Exception {
            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"reportName\":\"Test\",\"invoiceDateFrom\":\"20200101\",\"invoiceNumbers\":[\"1234567890123456\"]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenBoomNumberExceeds15Chars() throws Exception {
            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"reportName\":\"Test\",\"invoiceDateFrom\":\"20200101\",\"invoiceBoomNumbers\":[\"1234567890123456\"]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenTimberMarkExceeds15Chars() throws Exception {
            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"reportName\":\"Test\",\"invoiceDateFrom\":\"20200101\",\"invoiceTimberMarks\":[\"1234567890123456\"]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenWeighSlipExceeds15Chars() throws Exception {
            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"reportName\":\"Test\",\"invoiceDateFrom\":\"20200101\",\"invoiceWeighSlips\":[\"1234567890123456\"]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldReturn400_whenReplacesAdjustsExceeds15Chars() throws Exception {
            mockMvc.perform(post("/api/R13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reportFormat\":\"PDF\",\"reportName\":\"Test\",\"invoiceDateFrom\":\"20200101\",\"invoiceReplacesAdjusts\":[\"1234567890123456\"]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
}
