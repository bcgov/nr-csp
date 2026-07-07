package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionDetailResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionHistoryRowResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionInvoiceCommentResponse;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.service.SubmissionHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubmissionHistoryControllerTest {

    @Mock SubmissionHistoryService service;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SubmissionHistoryController(service))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // ---------------------------------------------------------------
    // listSubmissionHistory
    // ---------------------------------------------------------------

    @Test
    void listSubmissionHistory_returns200WithPagedResults() throws Exception {
        SubmissionHistoryRowResponse row = new SubmissionHistoryRowResponse(
                200456L, LocalDate.of(2024, Month.JANUARY, 31), "IDIR\\jdoe",
                "00014963", "ACME LOGGING LTD", "Approved", 3, 1);
        given(service.search(any())).willReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/submission-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cspSubmissionId").value(200456))
                .andExpect(jsonPath("$.content[0].submissionStatus").value("Approved"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listSubmissionHistory_empty_returns200() throws Exception {
        given(service.search(any())).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/submission-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---------------------------------------------------------------
    // getSubmissionDetail
    // ---------------------------------------------------------------

    @Test
    void getSubmissionDetail_found_returns200() throws Exception {
        SubmissionDetailResponse detail = new SubmissionDetailResponse(
                200456L, "ESUB-1", LocalDate.of(2024, Month.JANUARY, 31), "IDIR\\jdoe", "Approved",
                "00014963", "ACME LOGGING LTD", "00", null, null, "Y", "N", null,
                List.of(), List.of());
        given(service.getById(200456L)).willReturn(detail);

        mockMvc.perform(get("/api/submission-history/200456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cspSubmissionId").value(200456))
                .andExpect(jsonPath("$.clientName").value("ACME LOGGING LTD"));
    }

    @Test
    void getSubmissionDetail_notFound_returns404() throws Exception {
        given(service.getById(999L)).willThrow(new ResourceNotFoundException("Submission 999 was not found."));

        mockMvc.perform(get("/api/submission-history/999"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // getSubmissionInvoiceComments
    // ---------------------------------------------------------------

    @Test
    void getSubmissionInvoiceComments_returns200WithList() throws Exception {
        SubmissionInvoiceCommentResponse comment =
                new SubmissionInvoiceCommentResponse("WFP521046", "Approved", "Looks good");
        given(service.getInvoiceComments(200456L)).willReturn(List.of(comment));

        mockMvc.perform(get("/api/submission-history/200456/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("WFP521046"))
                .andExpect(jsonPath("$[0].comment").value("Looks good"));
    }
}
