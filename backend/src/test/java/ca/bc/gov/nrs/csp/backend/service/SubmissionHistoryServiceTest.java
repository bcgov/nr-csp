package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionDetailResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionHistoryRowResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionInvoiceCommentResponse;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.repository.SubmissionHistoryRepository;
import ca.bc.gov.nrs.csp.backend.security.SecurityContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubmissionHistoryServiceTest {

    private static final Pageable PAGE = PageRequest.of(0, 10);

    @Mock SubmissionHistoryRepository repository;

    SubmissionHistoryService service;

    @BeforeEach
    void setUp() {
        service = new SubmissionHistoryService(repository);
    }

    // ---------------------------------------------------------------
    // search()
    // ---------------------------------------------------------------

    @Test
    void search_delegatesToRepository_andReturnsPage() {
        SubmissionHistoryRowResponse row = new SubmissionHistoryRowResponse(
                200456L, LocalDate.of(2024, Month.JANUARY, 31), "IDIR\\jdoe",
                "00014963", "ACME LOGGING LTD", "Approved", 3, 1);
        given(repository.search(PAGE, List.of())).willReturn(new PageImpl<>(List.of(row), PAGE, 1));

        Page<SubmissionHistoryRowResponse> results = service.search(PAGE);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).cspSubmissionId()).isEqualTo(200456L);
        assertThat(results.getTotalElements()).isEqualTo(1);
        verify(repository).search(PAGE, List.of());
    }

    @Test
    void search_emptyPage_returnsEmptyContent() {
        given(repository.search(PAGE, List.of())).willReturn(new PageImpl<>(List.of(), PAGE, 0));

        assertThat(service.search(PAGE).getContent()).isEmpty();
    }

    @Test
    void search_restrictedCaller_passesClientNumbersToRepository() {
        given(repository.search(PAGE, List.of("000478HH")))
                .willReturn(new PageImpl<>(List.of(), PAGE, 0));

        try (MockedStatic<SecurityContextUtils> mocked = mockStatic(SecurityContextUtils.class)) {
            mocked.when(SecurityContextUtils::currentClientNumbers).thenReturn(List.of("000478HH"));

            service.search(PAGE);
        }

        verify(repository).search(PAGE, List.of("000478HH"));
    }

    // ---------------------------------------------------------------
    // getById()
    // ---------------------------------------------------------------

    @Test
    void getById_found_returnsDetail() {
        SubmissionDetailResponse detail = detailWithId(200456L);
        given(repository.findDetail(200456L, List.of())).willReturn(Optional.of(detail));

        SubmissionDetailResponse result = service.getById(200456L);

        assertThat(result.cspSubmissionId()).isEqualTo(200456L);
        verify(repository).findDetail(200456L, List.of());
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        given(repository.findDetail(999L, List.of())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999")
                .hasMessageContaining("was not found");
    }

    @Test
    void getById_restrictedCaller_passesClientNumbersToRepository() {
        SubmissionDetailResponse detail = detailWithId(200456L);
        given(repository.findDetail(200456L, List.of("000478HH"))).willReturn(Optional.of(detail));

        try (MockedStatic<SecurityContextUtils> mocked = mockStatic(SecurityContextUtils.class)) {
            mocked.when(SecurityContextUtils::currentClientNumbers).thenReturn(List.of("000478HH"));

            service.getById(200456L);
        }

        verify(repository).findDetail(200456L, List.of("000478HH"));
    }

    // ---------------------------------------------------------------
    // getInvoiceComments()
    // ---------------------------------------------------------------

    @Test
    void getInvoiceComments_delegatesToRepository() {
        SubmissionInvoiceCommentResponse comment =
                new SubmissionInvoiceCommentResponse("WFP521046", "Approved", "Looks good");
        given(repository.findInvoiceComments(200456L, List.of())).willReturn(List.of(comment));

        List<SubmissionInvoiceCommentResponse> results = service.getInvoiceComments(200456L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).invoiceNumber()).isEqualTo("WFP521046");
        verify(repository).findInvoiceComments(200456L, List.of());
    }

    @Test
    void getInvoiceComments_restrictedCaller_passesClientNumbersToRepository() {
        given(repository.findInvoiceComments(200456L, List.of("000478HH"))).willReturn(List.of());

        try (MockedStatic<SecurityContextUtils> mocked = mockStatic(SecurityContextUtils.class)) {
            mocked.when(SecurityContextUtils::currentClientNumbers).thenReturn(List.of("000478HH"));

            service.getInvoiceComments(200456L);
        }

        verify(repository).findInvoiceComments(200456L, List.of("000478HH"));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private SubmissionDetailResponse detailWithId(Long id) {
        return new SubmissionDetailResponse(
                id, "ESUB-1", LocalDate.of(2024, Month.JANUARY, 31), "IDIR\\jdoe", "Approved",
                "00014963", "ACME LOGGING LTD", "00", null, null, "Y", "N", null,
                List.of(), List.of());
    }
}
