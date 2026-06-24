package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.repository.InboxRepository;
import ca.bc.gov.nrs.csp.backend.repository.ValidationLookupRepository;
import ca.bc.gov.nrs.csp.backend.service.model.InboxCriteria;
import ca.bc.gov.nrs.csp.backend.service.model.InboxRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

    private static final Pageable PAGE = PageRequest.of(0, 100);

    @Mock InboxRepository inboxRepository;
    @Mock ValidationLookupRepository lookupRepository;

    InboxService inboxService;

    @BeforeEach
    void setUp() {
        inboxService = new InboxService(inboxRepository, lookupRepository);
        // Default: any status code is valid — lenient so tests that don't supply a
        // status don't trigger UnnecessaryStubbingException.
        lenient().when(lookupRepository.existsSubmissionStatusCode(any())).thenReturn(true);
    }

    // ---------------------------------------------------------------
    // Date range validation — mirrors InboxCriteriaValidator
    // ---------------------------------------------------------------

    @Test
    void search_dateFromAfterDateTo_throwsValidationException() {
        assertThatThrownBy(() -> search(
                LocalDate.of(2024, Month.FEBRUARY, 1),
                LocalDate.of(2024, Month.JANUARY, 1),
                null, null, null, null, null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    ValidationException ve = (ValidationException) ex;
                    assertThat(ve.getResult().hasErrors()).isTrue();
                    assertThat(ve.getResult().errors().get(0).messageKey())
                            .isEqualTo("inbox.dateperiod.error");
                });
    }

    @Test
    void search_dateFromEqualsDateTo_doesNotThrow() {
        LocalDate same = LocalDate.of(2024, Month.JANUARY, 15);
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        assertThat(search(same, same, null, null, null, null, null, null).getContent()).isEmpty();
    }

    @Test
    void search_dateFromBeforeDateTo_doesNotThrow() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        assertThat(search(
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                null, null, null, null, null, null).getContent()).isEmpty();
    }

    @Test
    void search_bothDatesNull_doesNotThrow() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        assertThat(search(null, null, null, null, null, null, null, null).getContent()).isEmpty();
    }

    // ---------------------------------------------------------------
    // Submission status normalisation
    // ---------------------------------------------------------------

    @Test
    void search_submissionStatusLowercase_isUppercased() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        search(null, null, null, null, "inb", null, null, null);

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        assertThat(captor.getValue().submissionStatus()).isEqualTo("INB");
    }

    @Test
    void search_submissionStatusWithWhitespace_isTrimmedAndUppercased() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        search(null, null, null, null, "  com  ", null, null, null);

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        assertThat(captor.getValue().submissionStatus()).isEqualTo("COM");
    }

    @Test
    void search_nullSubmissionStatus_passesNullToCriteria() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        search(null, null, null, null, null, null, null, null);

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        assertThat(captor.getValue().submissionStatus()).isNull();
    }

    // ---------------------------------------------------------------
    // Invoice number normalisation (Q6)
    // ---------------------------------------------------------------

    @Test
    void search_nullInvoiceNum_passesNullToCriteria() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        search(null, null, null, null, null, null, null, null);

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        assertThat(captor.getValue().invoiceNum()).isNull();
    }

    @Test
    void search_blankInvoiceNum_passesNullToCriteria() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        search(null, null, null, null, null, "   ", null, null);

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        assertThat(captor.getValue().invoiceNum()).isNull();
    }

    @Test
    void search_invoiceNumWithWhitespaceAndLowercase_isTrimmedAndUppercased() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        search(null, null, null, null, null, "  abc123  ", null, null);

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        assertThat(captor.getValue().invoiceNum()).isEqualTo("ABC123");
    }

    @Test
    void search_invoiceNumAlreadyUppercase_remainsUnchanged() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        search(null, null, null, null, null, "WFP521046", null, null);

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        assertThat(captor.getValue().invoiceNum()).isEqualTo("WFP521046");
    }

    // ---------------------------------------------------------------
    // Keyword normalisation
    // ---------------------------------------------------------------

    @Test
    void search_nonBlankKeyword_isTrimmedAndPassedToCriteria() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        inboxService.search(null, null, null, null, null, null, null, null, "  hello  ", PAGE);

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        assertThat(captor.getValue().keyword()).isEqualTo("hello");
    }

    @Test
    void search_blankKeyword_passesNullToCriteria() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        inboxService.search(null, null, null, null, null, null, null, null, "   ", PAGE);

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        assertThat(captor.getValue().keyword()).isNull();
    }

    @Test
    void search_nullKeyword_passesNullToCriteria() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());

        search(null, null, null, null, null, null, null, null);

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        assertThat(captor.getValue().keyword()).isNull();
    }

    // ---------------------------------------------------------------
    // Criteria passthrough — confirm other fields are passed unchanged
    // ---------------------------------------------------------------

    @Test
    void search_allCriteriaProvided_passedThroughToCriteria() {
        given(inboxRepository.search(any(), any())).willReturn(emptyPage());
        LocalDate from = LocalDate.of(2024, Month.JANUARY, 1);
        LocalDate to = LocalDate.of(2024, Month.DECEMBER, 31);

        search(from, to, "Buyer", "Electronic", "INB", "WFP", "00012345", "00");

        ArgumentCaptor<InboxCriteria> captor = ArgumentCaptor.forClass(InboxCriteria.class);
        verify(inboxRepository).search(captor.capture(), any());
        InboxCriteria c = captor.getValue();

        assertThat(c.submissionDateFrom()).isEqualTo(from);
        assertThat(c.submissionDateTo()).isEqualTo(to);
        assertThat(c.submittedBy()).isEqualTo("Buyer");
        assertThat(c.submissionType()).isEqualTo("Electronic");
        assertThat(c.submissionStatus()).isEqualTo("INB");
        assertThat(c.submitterClientNum()).isEqualTo("00012345");
        assertThat(c.submitterLocNum()).isEqualTo("00");
    }

    // ---------------------------------------------------------------
    // Return value passthrough
    // ---------------------------------------------------------------

    @Test
    void search_repositoryResultsReturnedToCallers() {
        InboxRow row = new InboxRow(1L, 42L, "SUB001", LocalDate.of(2024, Month.JANUARY, 15),
                "Inbox", "Electronic", 3, 2, 0, 1, 0);
        given(inboxRepository.search(any(), any()))
                .willReturn(new PageImpl<>(List.of(row), PAGE, 1));

        Page<InboxRow> result = search(null, null, null, null, null, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).submissionStatus()).isEqualTo("Inbox");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Page<InboxRow> search(
            LocalDate dateFrom, LocalDate dateTo,
            String submittedBy, String submissionType, String submissionStatus,
            String invoiceNum, String clientNum, String locNum) {
        return inboxService.search(
                dateFrom, dateTo, submittedBy, submissionType, submissionStatus,
                invoiceNum, clientNum, locNum, null, PAGE);
    }

    private Page<InboxRow> emptyPage() {
        return new PageImpl<>(List.of(), PAGE, 0);
    }
}
