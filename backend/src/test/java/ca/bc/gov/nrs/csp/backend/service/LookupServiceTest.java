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

    // ---------------------------------------------------------------
    // getSortCodes()
    // ---------------------------------------------------------------

    @Test
    void getSortCodes_returnsRepositoryResults() {
        List<LookupItem> expected = List.of(
                new LookupItem("01", "Sort 01"),
                new LookupItem("02", "Sort 02")
        );
        given(lookupRepository.findSortCodes()).willReturn(expected);

        List<LookupItem> result = lookupService.getSortCodes();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("01");
        assertThat(result.get(0).description()).isEqualTo("Sort 01");
    }

    @Test
    void getSortCodes_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findSortCodes()).willReturn(List.of());

        assertThat(lookupService.getSortCodes()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getSpeciesCodes()
    // ---------------------------------------------------------------

    @Test
    void getSpeciesCodes_returnsRepositoryResults() {
        List<LookupItem> expected = List.of(
                new LookupItem("CE", "Cedar"),
                new LookupItem("FI", "Fir")
        );
        given(lookupRepository.findSpeciesCodes()).willReturn(expected);

        List<LookupItem> result = lookupService.getSpeciesCodes();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("CE");
        assertThat(result.get(0).description()).isEqualTo("Cedar");
    }

    @Test
    void getSpeciesCodes_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findSpeciesCodes()).willReturn(List.of());

        assertThat(lookupService.getSpeciesCodes()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getGradeCodes()
    // ---------------------------------------------------------------

    @Test
    void getGradeCodes_returnsRepositoryResults() {
        List<LookupItem> expected = List.of(
                new LookupItem("4", "Grade 4"),
                new LookupItem("6", "Grade 6")
        );
        given(lookupRepository.findGradeCodes()).willReturn(expected);

        List<LookupItem> result = lookupService.getGradeCodes();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("4");
        assertThat(result.get(0).description()).isEqualTo("Grade 4");
    }

    @Test
    void getGradeCodes_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findGradeCodes()).willReturn(List.of());

        assertThat(lookupService.getGradeCodes()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getGradesBySpecies()
    // ---------------------------------------------------------------

    @Test
    void getGradesBySpecies_returnsRepositoryResults_forGivenSpecies() {
        List<LookupItem> expected = List.of(
                new LookupItem("A", "Grade A"),
                new LookupItem("B", "Grade B")
        );
        given(lookupRepository.findGradesBySpecies("FD")).willReturn(expected);

        List<LookupItem> result = lookupService.getGradesBySpecies("FD");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("A");
        assertThat(result.get(0).description()).isEqualTo("Grade A");
    }

    @Test
    void getGradesBySpecies_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findGradesBySpecies("ZZ")).willReturn(List.of());

        assertThat(lookupService.getGradesBySpecies("ZZ")).isEmpty();
    }

    // ---------------------------------------------------------------
    // getModellingCodes()
    // ---------------------------------------------------------------

    @Test
    void getModellingCodes_returnsRepositoryResults() {
        List<LookupItem> expected = List.of(
                new LookupItem("M1", "Model One"),
                new LookupItem("M2", "Model Two")
        );
        given(lookupRepository.findModellingCodes()).willReturn(expected);

        List<LookupItem> result = lookupService.getModellingCodes();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("M1");
        assertThat(result.get(0).description()).isEqualTo("Model One");
    }

    @Test
    void getModellingCodes_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findModellingCodes()).willReturn(List.of());

        assertThat(lookupService.getModellingCodes()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getFobCodes()
    // ---------------------------------------------------------------

    @Test
    void getFobCodes_returnsRepositoryResults() {
        List<LookupItem> expected = List.of(
                new LookupItem("BW", "Barge or Water"),
                new LookupItem("TR", "Truck")
        );
        given(lookupRepository.findFobCodes()).willReturn(expected);

        List<LookupItem> result = lookupService.getFobCodes();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("BW");
        assertThat(result.get(0).description()).isEqualTo("Barge or Water");
    }

    @Test
    void getFobCodes_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findFobCodes()).willReturn(List.of());

        assertThat(lookupService.getFobCodes()).isEmpty();
    }

    // ---------------------------------------------------------------
    // getSpeciesGradeCombinations()
    // ---------------------------------------------------------------

    @Test
    void getSpeciesGradeCombinations_returnsRepositoryResults() {
        List<LookupRepository.SpeciesGradeCombo> expected = List.of(
                new LookupRepository.SpeciesGradeCombo("CE", "4"),
                new LookupRepository.SpeciesGradeCombo("FI", "6")
        );
        given(lookupRepository.findSpeciesGradeCombinations()).willReturn(expected);

        List<LookupRepository.SpeciesGradeCombo> result = lookupService.getSpeciesGradeCombinations();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).species()).isEqualTo("CE");
        assertThat(result.get(0).grade()).isEqualTo("4");
        assertThat(result.get(1).species()).isEqualTo("FI");
        assertThat(result.get(1).grade()).isEqualTo("6");
    }

    @Test
    void getSpeciesGradeCombinations_returnsEmptyList_whenRepositoryReturnsNone() {
        given(lookupRepository.findSpeciesGradeCombinations()).willReturn(List.of());

        assertThat(lookupService.getSpeciesGradeCombinations()).isEmpty();
    }

}
