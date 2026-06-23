package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.inbox.InboxRowResponse;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.service.InboxService;
import ca.bc.gov.nrs.csp.backend.service.mapper.InboxMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InboxControllerTest {

    @Mock InboxService inboxService;
    @Mock InboxMapper inboxMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InboxController(inboxService, inboxMapper))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // ---------------------------------------------------------------
    // 200 — happy path
    // ---------------------------------------------------------------

    @Test
    void searchInbox_noFilters_returns200WithEmptyPage() throws Exception {
        given(inboxService.search(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn((Page) new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));
        given(inboxMapper.toResponsePage(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        mockMvc.perform(get("/api/inbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void searchInbox_withResults_returns200WithPagedContent() throws Exception {
        InboxRowResponse row = new InboxRowResponse(
                "SUB001", LocalDate.of(2024, 1, 15), "Inbox", "Electronic", 3, 2, 0, 1, 0
        );
        given(inboxService.search(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn((Page) new PageImpl<>(List.of(), PageRequest.of(0, 100), 1));
        given(inboxMapper.toResponsePage(any()))
                .willReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 100), 1));

        mockMvc.perform(get("/api/inbox").param("submissionStatus", "INB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].submissionId").value("SUB001"))
                .andExpect(jsonPath("$.content[0].submissionStatus").value("Inbox"))
                .andExpect(jsonPath("$.content[0].submissionType").value("Electronic"))
                .andExpect(jsonPath("$.content[0].invTotal").value(3))
                .andExpect(jsonPath("$.content[0].invApproved").value(2))
                .andExpect(jsonPath("$.content[0].invRejected").value(0))
                .andExpect(jsonPath("$.content[0].invProcessing").value(1))
                .andExpect(jsonPath("$.content[0].invCancelled").value(0));
    }

    @Test
    void searchInbox_honorsPageAndSizeQueryParams() throws Exception {
        given(inboxService.search(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn((Page) new PageImpl<>(List.of(), PageRequest.of(1, 50), 0));
        given(inboxMapper.toResponsePage(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(1, 50), 0));

        mockMvc.perform(get("/api/inbox").param("page", "1").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(50));
    }

    // ---------------------------------------------------------------
    // 400 — bad request scenarios
    // ---------------------------------------------------------------

    @Test
    void searchInbox_serviceThrowsValidationExceptionForDateRange_returns400WithValidationError() throws Exception {
        ValidationResult result = new ValidationResult(List.of(
                new ValidationMessage("inbox.dateperiod.error", null, MessageType.ERROR)));
        given(inboxService.search(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new ValidationException("Inbox search criteria validation failed.", result));

        mockMvc.perform(get("/api/inbox")
                        .param("submissionDateFrom", "2024-02-01")
                        .param("submissionDateTo", "2024-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].messageKey").value("inbox.dateperiod.error"));
    }

    @Test
    void searchInbox_serviceThrowsBadRequestForUnknownSortField_returns400WithApiError() throws Exception {
        given(inboxService.search(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new BadRequestException("Unsupported sort field: unknownField."));

        mockMvc.perform(get("/api/inbox").param("sort", "unknownField,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Unsupported sort field: unknownField."));
    }
}
