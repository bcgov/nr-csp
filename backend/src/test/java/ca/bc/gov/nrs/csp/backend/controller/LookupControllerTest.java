package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.lookup.LookupItemResponse;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.service.LookupService;
import ca.bc.gov.nrs.csp.backend.service.mapper.LookupMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LookupControllerTest {

    @Mock LookupService lookupService;
    @Mock LookupMapper lookupMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LookupController(lookupService, lookupMapper))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .build();
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/maturity
    // ---------------------------------------------------------------

    @Test
    void getMaturityCodes_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("O", "Old Growth"),
                new LookupItemResponse("S", "Second Growth")
        );
        given(lookupService.getMaturityCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/maturity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("O"))
                .andExpect(jsonPath("$[0].description").value("Old Growth"))
                .andExpect(jsonPath("$[1].code").value("S"));
    }

    @Test
    void getMaturityCodes_returns200_withEmptyList() throws Exception {
        given(lookupService.getMaturityCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/maturity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getMaturityCodes_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getMaturityCodes()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/maturity"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/type
    // ---------------------------------------------------------------

    @Test
    void getInvoiceTypes_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("SAL", "Sales"),
                new LookupItemResponse("PUR", "Purchase")
        );
        given(lookupService.getInvoiceTypes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SAL"))
                .andExpect(jsonPath("$[0].description").value("Sales"))
                .andExpect(jsonPath("$[1].code").value("PUR"));
    }

    @Test
    void getInvoiceTypes_returns200_withEmptyList() throws Exception {
        given(lookupService.getInvoiceTypes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getInvoiceTypes_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getInvoiceTypes()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/type"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/status
    // ---------------------------------------------------------------

    @Test
    void getInvoiceStatuses_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("APP", "Approved"),
                new LookupItemResponse("REJ", "Rejected")
        );
        given(lookupService.getInvoiceStatuses()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("APP"))
                .andExpect(jsonPath("$[0].description").value("Approved"))
                .andExpect(jsonPath("$[1].code").value("REJ"));
    }

    @Test
    void getInvoiceStatuses_returns200_withEmptyList() throws Exception {
        given(lookupService.getInvoiceStatuses()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getInvoiceStatuses_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getInvoiceStatuses()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/submission-status
    // ---------------------------------------------------------------

    @Test
    void getSubmissionStatuses_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("COM", "Complete"),
                new LookupItemResponse("INB", "Inbox")
        );
        given(lookupService.getSubmissionStatuses()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/submission-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("COM"))
                .andExpect(jsonPath("$[0].description").value("Complete"))
                .andExpect(jsonPath("$[1].code").value("INB"));
    }

    @Test
    void getSubmissionStatuses_returns200_withEmptyList() throws Exception {
        given(lookupService.getSubmissionStatuses()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/submission-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSubmissionStatuses_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getSubmissionStatuses()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/submission-status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

}
