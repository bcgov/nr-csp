package ca.bc.gov.nrs.csp.backend.util.validation.inbox;

import ca.bc.gov.nrs.csp.backend.repository.ValidationLookupRepository;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InboxCriteriaValidatorTest {

    @Mock ValidationLookupRepository lookupRepository;

    InboxCriteriaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InboxCriteriaValidator(lookupRepository);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Validate with only dates set; other criteria null. */
    private ValidationResult validateDates(LocalDate from, LocalDate to) {
        return validator.validate(from, to, null, null, null, null, null, null);
    }

    // ---------------------------------------------------------------
    // Date range
    // ---------------------------------------------------------------

    @Test
    void validate_bothDatesNull_isValid() {
        assertThat(validateDates(null, null).isValid()).isTrue();
    }

    @Test
    void validate_onlyFromDate_isValid() {
        assertThat(validateDates(LocalDate.of(2024, Month.JANUARY, 1), null).isValid()).isTrue();
    }

    @Test
    void validate_onlyToDate_isValid() {
        assertThat(validateDates(null, LocalDate.of(2024, Month.DECEMBER, 31)).isValid()).isTrue();
    }

    @Test
    void validate_fromBeforeTo_isValid() {
        assertThat(validateDates(LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2024, Month.DECEMBER, 31)).isValid()).isTrue();
    }

    @Test
    void validate_fromEqualsTo_isValid() {
        LocalDate same = LocalDate.of(2024, Month.JUNE, 15);
        assertThat(validateDates(same, same).isValid()).isTrue();
    }

    @Test
    void validate_fromAfterTo_hasErrorWithCorrectKey() {
        ValidationResult result = validateDates(LocalDate.of(2024, Month.FEBRUARY, 1), LocalDate.of(2024, Month.JANUARY, 1));

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).messageKey()).isEqualTo(InboxCriteriaValidator.DATE_RANGE_KEY);
    }

    // ---------------------------------------------------------------
    // Submitted By
    // ---------------------------------------------------------------

    @Test
    void validate_submittedByNull_isValid() {
        assertThat(validator.validate(null, null, null, null, null, null, null, null).isValid()).isTrue();
    }

    @Test
    void validate_submittedByBuyer_isValid() {
        assertThat(validator.validate(null, null, "Buyer", null, null, null, null, null).isValid()).isTrue();
    }

    @Test
    void validate_submittedBySeller_isValid() {
        assertThat(validator.validate(null, null, "Seller", null, null, null, null, null).isValid()).isTrue();
    }

    @Test
    void validate_submittedByCaseInsensitive_isValid() {
        assertThat(validator.validate(null, null, "BUYER", null, null, null, null, null).isValid()).isTrue();
    }

    @Test
    void validate_submittedByInvalidValue_hasError() {
        ValidationResult result = validator.validate(null, null, "Admin", null, null, null, null, null);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors().get(0).messageKey()).isEqualTo(InboxCriteriaValidator.SUBMITTED_BY_KEY);
    }

    // ---------------------------------------------------------------
    // Submission Type
    // ---------------------------------------------------------------

    @Test
    void validate_submissionTypeElectronic_isValid() {
        assertThat(validator.validate(null, null, null, "Electronic", null, null, null, null).isValid()).isTrue();
    }

    @Test
    void validate_submissionTypeManual_isValid() {
        assertThat(validator.validate(null, null, null, "Manual", null, null, null, null).isValid()).isTrue();
    }

    @Test
    void validate_submissionTypeInvalidValue_hasError() {
        ValidationResult result = validator.validate(null, null, null, "Paper", null, null, null, null);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors().get(0).messageKey()).isEqualTo(InboxCriteriaValidator.SUBMISSION_TYPE_KEY);
    }

    // ---------------------------------------------------------------
    // Submission Status (DB lookup)
    // ---------------------------------------------------------------

    @Test
    void validate_submissionStatusKnownCode_isValid() {
        given(lookupRepository.existsSubmissionStatusCode("INB")).willReturn(true);

        assertThat(validator.validate(null, null, null, null, "INB", null, null, null).isValid()).isTrue();
    }

    @Test
    void validate_submissionStatusUnknownCode_hasError() {
        given(lookupRepository.existsSubmissionStatusCode("GARBAGE")).willReturn(false);

        ValidationResult result = validator.validate(null, null, null, null, "GARBAGE", null, null, null);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors().get(0).messageKey()).isEqualTo(InboxCriteriaValidator.SUBMISSION_STATUS_KEY);
        assertThat(result.errors().get(0).args()).containsExactly("GARBAGE");
    }

    @Test
    void validate_submissionStatusNull_doesNotCallDb() {
        // null status — lookup should not be invoked
        assertThat(validator.validate(null, null, null, null, null, null, null, null).isValid()).isTrue();
    }

    // ---------------------------------------------------------------
    // Invoice Number max length
    // ---------------------------------------------------------------

    @Test
    void validate_invoiceNumAtMaxLength_isValid() {
        String exactly15 = "A".repeat(InboxCriteriaValidator.MAX_INVOICE_NUM_LENGTH);
        assertThat(validator.validate(null, null, null, null, null, exactly15, null, null).isValid()).isTrue();
    }

    @Test
    void validate_invoiceNumTooLong_hasError() {
        String tooLong = "A".repeat(InboxCriteriaValidator.MAX_INVOICE_NUM_LENGTH + 1);
        ValidationResult result = validator.validate(null, null, null, null, null, tooLong, null, null);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors().get(0).messageKey()).isEqualTo(InboxCriteriaValidator.INVOICE_NUM_LENGTH_KEY);
    }

    @Test
    void validate_invoiceNumNull_isValid() {
        assertThat(validator.validate(null, null, null, null, null, null, null, null).isValid()).isTrue();
    }

    // ---------------------------------------------------------------
    // Multiple errors accumulate
    // ---------------------------------------------------------------

    @Test
    void validate_multipleViolations_allErrorsReported() {
        given(lookupRepository.existsSubmissionStatusCode(anyString())).willReturn(false);

        ValidationResult result = validator.validate(
                LocalDate.of(2024, Month.FEBRUARY, 1), LocalDate.of(2024, Month.JANUARY, 1), // date reversed
                "BadRole",                                            // bad submittedBy
                null, "GARBAGE", null, null, null);                   // bad status

        assertThat(result.errors()).hasSize(3);
    }

    // ---------------------------------------------------------------
    // Client number / location pair rule
    // ---------------------------------------------------------------

    @Test
    void validate_bothClientNumAndLocNum_isValid() {
        assertThat(validator.validate(null, null, null, null, null, null, "00012345", "00").isValid()).isTrue();
    }

    @Test
    void validate_bothClientNumAndLocNumNull_isValid() {
        assertThat(validator.validate(null, null, null, null, null, null, null, null).isValid()).isTrue();
    }

    @Test
    void validate_clientNumWithoutLocNum_hasError() {
        ValidationResult result = validator.validate(null, null, null, null, null, null, "00012345", null);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors().get(0).messageKey()).isEqualTo(InboxCriteriaValidator.CLIENT_PAIR_KEY);
    }

    @Test
    void validate_locNumWithoutClientNum_hasError() {
        ValidationResult result = validator.validate(null, null, null, null, null, null, null, "00");

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors().get(0).messageKey()).isEqualTo(InboxCriteriaValidator.CLIENT_PAIR_KEY);
    }

    // ---------------------------------------------------------------
    // State not leaked between calls
    // ---------------------------------------------------------------

    @Test
    void validate_calledTwice_secondCallDoesNotInheritFirstCallErrors() {
        validator.validate(LocalDate.of(2024, Month.FEBRUARY, 1), LocalDate.of(2024, Month.JANUARY, 1),
                null, null, null, null, null, null);
        ValidationResult second = validator.validate(LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2024, Month.DECEMBER, 31),
                null, null, null, null, null, null);

        assertThat(second.isValid()).isTrue();
    }
}
