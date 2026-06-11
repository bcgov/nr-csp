package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.repository.LookupRepository;
import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LookupServiceTest {

    @Mock LookupRepository lookupRepository;

    LookupService lookupService;

    @BeforeEach
    void setUp() {
        lookupService = new LookupService(lookupRepository);
    }

    // ---------------------------------------------------------------
    // getMaturityCodes()
    // ---------------------------------------------------------------

    @Test
    void getMaturityCodes_returnsRepositoryResults() {
        List<LookupItem> expected = List.of(
                new LookupItem("O", "Old Growth"),
                new LookupItem("S", "Second Growth")
        );
        given(lookupRepository.findMaturityCodes()).willReturn(expected);

        List<LookupItem> result = lookupService.getMaturityCodes();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("O");
        assertThat(result.get(0).description()).isEqualTo("Old Growth");
    }

    @Test
    void getMaturityCodes_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findMaturityCodes()).willReturn(List.of());

        assertThat(lookupService.getMaturityCodes()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getInvoiceTypes()
    // ---------------------------------------------------------------

    @Test
    void getInvoiceTypes_returnsRepositoryResults() {
        List<LookupItem> expected = List.of(
                new LookupItem("SAL", "Sales"),
                new LookupItem("PUR", "Purchase")
        );
        given(lookupRepository.findInvoiceTypes()).willReturn(expected);

        List<LookupItem> result = lookupService.getInvoiceTypes();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("SAL");
        assertThat(result.get(0).description()).isEqualTo("Sales");
    }

    @Test
    void getInvoiceTypes_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findInvoiceTypes()).willReturn(List.of());

        assertThat(lookupService.getInvoiceTypes()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getInvoiceStatuses()
    // ---------------------------------------------------------------

    @Test
    void getInvoiceStatuses_returnsRepositoryResults() {
        List<LookupItem> expected = List.of(
                new LookupItem("APP", "Approved"),
                new LookupItem("REJ", "Rejected")
        );
        given(lookupRepository.findInvoiceStatuses()).willReturn(expected);

        List<LookupItem> result = lookupService.getInvoiceStatuses();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("APP");
        assertThat(result.get(0).description()).isEqualTo("Approved");
    }

    @Test
    void getInvoiceStatuses_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findInvoiceStatuses()).willReturn(List.of());

        assertThat(lookupService.getInvoiceStatuses()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getSubmissionStatuses()
    // ---------------------------------------------------------------

    @Test
    void getSubmissionStatuses_returnsRepositoryResults() {
        List<LookupItem> expected = List.of(
                new LookupItem("COM", "Complete"),
                new LookupItem("INB", "Inbox")
        );
        given(lookupRepository.findSubmissionStatuses()).willReturn(expected);

        List<LookupItem> result = lookupService.getSubmissionStatuses();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("COM");
        assertThat(result.get(0).description()).isEqualTo("Complete");
    }

    @Test
    void getSubmissionStatuses_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findSubmissionStatuses()).willReturn(List.of());

        assertThat(lookupService.getSubmissionStatuses()).isEmpty();
    }

}
