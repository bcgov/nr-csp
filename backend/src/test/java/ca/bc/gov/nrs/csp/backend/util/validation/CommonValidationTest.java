package ca.bc.gov.nrs.csp.backend.util.validation;

import ca.bc.gov.nrs.csp.backend.repository.ValidationLookupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommonValidationTest {

    @Mock ValidationLookupRepository lookupRepo;

    @InjectMocks CommonValidation validation;

    private static final LocalDate DATE = LocalDate.of(2024, Month.JUNE, 1);

    @Test
    void isValidSortCode_delegatesToRepo_true() {
        given(lookupRepo.existsActiveSortCode("SC1", DATE)).willReturn(true);
        assertThat(validation.isValidSortCode("SC1", DATE)).isTrue();
    }

    @Test
    void isValidSortCode_delegatesToRepo_false() {
        given(lookupRepo.existsActiveSortCode("BAD", DATE)).willReturn(false);
        assertThat(validation.isValidSortCode("BAD", DATE)).isFalse();
    }

    @Test
    void isValidMaturity_delegatesToRepo_true() {
        given(lookupRepo.existsActiveMaturityCode("M", DATE)).willReturn(true);
        assertThat(validation.isValidMaturity("M", DATE)).isTrue();
    }

    @Test
    void isValidMaturity_delegatesToRepo_false() {
        given(lookupRepo.existsActiveMaturityCode("X", DATE)).willReturn(false);
        assertThat(validation.isValidMaturity("X", DATE)).isFalse();
    }

    @Test
    void isValidSpeciesGradeCombination_delegatesToRepo_true() {
        given(lookupRepo.existsSpeciesGradeCombination("FD", "U")).willReturn(true);
        assertThat(validation.isValidSpeciesGradeCombination("FD", "U")).isTrue();
    }

    @Test
    void isValidSpeciesGradeCombination_delegatesToRepo_false() {
        given(lookupRepo.existsSpeciesGradeCombination("FD", "Z")).willReturn(false);
        assertThat(validation.isValidSpeciesGradeCombination("FD", "Z")).isFalse();
    }

    @Test
    void isValidInvoiceType_delegatesToRepo_true() {
        given(lookupRepo.existsActiveInvoiceTypeCode("SAL", DATE)).willReturn(true);
        assertThat(validation.isValidInvoiceType("SAL", DATE)).isTrue();
    }

    @Test
    void isValidInvoiceType_delegatesToRepo_false() {
        given(lookupRepo.existsActiveInvoiceTypeCode("BAD", DATE)).willReturn(false);
        assertThat(validation.isValidInvoiceType("BAD", DATE)).isFalse();
    }

    @Test
    void isValidClientLocation_delegatesToRepo_true() {
        given(lookupRepo.existsClientLocation("00001234", "01")).willReturn(true);
        assertThat(validation.isValidClientLocation("00001234", "01")).isTrue();
    }

    @Test
    void isValidClientLocation_delegatesToRepo_false() {
        given(lookupRepo.existsClientLocation("99999999", "99")).willReturn(false);
        assertThat(validation.isValidClientLocation("99999999", "99")).isFalse();
    }

    @Test
    void findSpeciesGradeXrefId_returnsRepoResult() {
        given(lookupRepo.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.of(42L));
        assertThat(validation.findSpeciesGradeXrefId("FD", "U", DATE)).contains(42L);
    }

    @Test
    void findSpeciesGradeXrefId_returnsEmptyWhenNotFound() {
        given(lookupRepo.findSpeciesGradeXrefId("FD", "Z", DATE)).willReturn(Optional.empty());
        assertThat(validation.findSpeciesGradeXrefId("FD", "Z", DATE)).isEmpty();
    }

    @Test
    void isDuplicateFlatPriceConversion_delegatesToRepo_true() {
        given(lookupRepo.existsDuplicateFlatPriceConversion("P", "SC1", 10L, "M", DATE, null)).willReturn(true);
        assertThat(validation.isDuplicateFlatPriceConversion("P", "SC1", 10L, "M", DATE, null)).isTrue();
    }

    @Test
    void isDuplicateFlatPriceConversion_delegatesToRepo_false() {
        given(lookupRepo.existsDuplicateFlatPriceConversion("P", "SC1", 10L, "M", DATE, 5L)).willReturn(false);
        assertThat(validation.isDuplicateFlatPriceConversion("P", "SC1", 10L, "M", DATE, 5L)).isFalse();
    }

    @Test
    void isValidDateRange_effectiveDateBeforeExpiry_returnsTrue() {
        assertThat(validation.isValidDateRange(LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2024, Month.DECEMBER, 31))).isTrue();
    }

    @Test
    void isValidDateRange_effectiveDateEqualsExpiry_returnsTrue() {
        LocalDate date = LocalDate.of(2024, Month.JUNE, 1);
        assertThat(validation.isValidDateRange(date, date)).isTrue();
    }

    @Test
    void isValidDateRange_effectiveDateAfterExpiry_returnsFalse() {
        assertThat(validation.isValidDateRange(LocalDate.of(2024, Month.DECEMBER, 31), LocalDate.of(2024, Month.JANUARY, 1))).isFalse();
    }

    @Test
    void isValidDateRange_nullExpiry_returnsTrue() {
        assertThat(validation.isValidDateRange(LocalDate.of(2024, Month.JANUARY, 1), null)).isTrue();
    }
}
