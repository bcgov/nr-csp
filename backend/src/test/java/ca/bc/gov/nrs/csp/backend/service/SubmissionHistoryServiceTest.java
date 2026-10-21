package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionDetailResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionHistoryRowResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionInvoiceCommentResponse;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.repository.SubmissionHistoryRepository;
import ca.bc.gov.nrs.csp.backend.security.ClientScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SubmissionHistoryServiceTest {

    private static final Pageable PAGE = PageRequest.of(0, 10);

    @Mock SubmissionHistoryRepository repository;

    SubmissionHistoryService service;

    private static final ClientScope ALL = ClientScope.allClients();

    @BeforeEach
    void setUp() {
        service = new SubmissionHistoryService(repository);
        signIn("ADMIN");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------
    // search()
    // ---------------------------------------------------------------

    @Test
    void search_delegatesToRepository_andReturnsPage() {
        SubmissionHistoryRowResponse row = new SubmissionHistoryRowResponse(
                200456L, "9001", LocalDate.of(2024, Month.JANUARY, 31), "IDIR\\jdoe",
                "00014963", "ACME LOGGING LTD", "Approved", 3, 1);
        given(repository.search(PAGE, ALL)).willReturn(new PageImpl<>(List.of(row), PAGE, 1));

        Page<SubmissionHistoryRowResponse> results = service.search(PAGE);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).cspSubmissionId()).isEqualTo(200456L);
        assertThat(results.getTotalElements()).isEqualTo(1);
        verify(repository).search(PAGE, ALL);
    }

    @Test
    void search_emptyPage_returnsEmptyContent() {
        given(repository.search(PAGE, ALL)).willReturn(new PageImpl<>(List.of(), PAGE, 0));

        assertThat(service.search(PAGE).getContent()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getById()
    // ---------------------------------------------------------------

    @Test
    void getById_found_returnsDetail() {
        SubmissionDetailResponse detail = detailWithId(200456L);
        given(repository.findDetail(200456L, ALL)).willReturn(Optional.of(detail));

        SubmissionDetailResponse result = service.getById(200456L);

        assertThat(result.cspSubmissionId()).isEqualTo(200456L);
        verify(repository).findDetail(200456L, ALL);
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        given(repository.findDetail(999L, ALL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999")
                .hasMessageContaining("was not found");
    }

    // ---------------------------------------------------------------
    // getInvoiceComments()
    // ---------------------------------------------------------------

    @Test
    void getInvoiceComments_delegatesToRepository() {
        SubmissionInvoiceCommentResponse comment =
                new SubmissionInvoiceCommentResponse("WFP521046", "Approved", "Looks good");
        given(repository.findInvoiceComments(200456L, ALL)).willReturn(List.of(comment));

        List<SubmissionInvoiceCommentResponse> results = service.getInvoiceComments(200456L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).invoiceNumber()).isEqualTo("WFP521046");
        verify(repository).findInvoiceComments(200456L, ALL);
    }

    // ---------------------------------------------------------------
    // Client scope
    // ---------------------------------------------------------------

    @Test
    void submitter_isScopedToTheirForestClients_everywhere() {
        signIn("SUBMITTER", "FOREST_CLIENT-00000002", "FOREST_CLIENT-00000001");
        ClientScope theirs = ClientScope.of(List.of("00000001", "00000002"));
        given(repository.search(PAGE, theirs)).willReturn(new PageImpl<>(List.of(), PAGE, 0));
        given(repository.findDetail(9001L, theirs)).willReturn(Optional.empty());
        given(repository.findInvoiceComments(200456L, theirs)).willReturn(List.of());

        service.search(PAGE);
        // Another client's submission reads as missing, not forbidden.
        assertThatThrownBy(() -> service.getById(9001L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.getInvoiceComments(200456L)).isEmpty();

        verify(repository).search(PAGE, theirs);
        verify(repository).findDetail(9001L, theirs);
        verify(repository).findInvoiceComments(200456L, theirs);
    }

    @Test
    void submitterWithoutAClient_isDeniedBeforeAnyQuery() {
        signIn("SUBMITTER");

        assertThatThrownBy(() -> service.search(PAGE)).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> service.getById(9001L)).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> service.getInvoiceComments(200456L)).isInstanceOf(AuthorizationDeniedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void noRole_isDeniedBeforeAnyQuery() {
        signIn();

        assertThatThrownBy(() -> service.search(PAGE)).isInstanceOf(AuthorizationDeniedException.class);
        verifyNoInteractions(repository);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static void signIn(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", null, Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private SubmissionDetailResponse detailWithId(Long id) {
        return new SubmissionDetailResponse(
                id, "ESUB-1", LocalDate.of(2024, Month.JANUARY, 31), "IDIR\\jdoe", "Approved",
                "00014963", "ACME LOGGING LTD", "00", null, null, "Y", "N", null,
                List.of(), List.of());
    }
}
