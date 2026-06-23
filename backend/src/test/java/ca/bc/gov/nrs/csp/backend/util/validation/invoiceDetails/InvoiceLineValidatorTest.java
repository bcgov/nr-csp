package ca.bc.gov.nrs.csp.backend.util.validation.invoiceDetails;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class InvoiceLineValidatorTest {

    private static final LocalDate INV_DATE = LocalDate.of(2024, Month.JUNE, 15);

    @Mock CommonValidation commonValidation;

    InvoiceLineValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InvoiceLineValidator(commonValidation);
        lenient().when(commonValidation.isValidSortCode(any(), any())).thenReturn(true);
        lenient().when(commonValidation.isValidSpeciesGradeCombination(any(), any())).thenReturn(true);
    }

    // ---------------------------------------------------------------
    // validate() — entry behaviour
    // ---------------------------------------------------------------

    @Test
    void validate_nullLine_returnsMissingError() {
        ValidationResult result = validator.validate(null, "SAL", INV_DATE);

        assertHasError(result, "invoice.lineitem.missing.error");
    }

    @Test
    void validate_validLine_returnsNoErrorsOrWarnings() {
        ValidationResult result = validator.validate(validLine(), "SAL", INV_DATE);

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.hasWarnings()).isFalse();
    }

    // ---------------------------------------------------------------
    // isValidSortCode
    // ---------------------------------------------------------------

    @Test
    void validate_invalidSortCode_addsError() {
        given(commonValidation.isValidSortCode("BAD", INV_DATE)).willReturn(false);
        LineItem line = lineWith(l -> l.secondSort = "BAD");

        ValidationResult result = validator.validate(line, "SAL", INV_DATE);

        assertHasError(result, "invoice.secondry.sortcode.invalid.error");
    }

    // ---------------------------------------------------------------
    // checkSpeciesGradeCombination
    // ---------------------------------------------------------------

    @Test
    void validate_invalidSpeciesGradeCombination_addsError() {
        given(commonValidation.isValidSpeciesGradeCombination("SP1", "G1")).willReturn(false);
        LineItem line = lineWith(l -> { l.species = "SP1"; l.grade = "G1"; });

        ValidationResult result = validator.validate(line, "SAL", INV_DATE);

        assertHasError(result, "invoice.species.grade.combination.error");
    }

    // ---------------------------------------------------------------
    // checkGrade
    // ---------------------------------------------------------------

    @Test
    void validate_nullGrade_addsRequiredError() {
        LineItem line = lineWith(l -> l.grade = null);

        ValidationResult result = validator.validate(line, "SAL", INV_DATE);

        assertHasError(result, "invoice.grade.invalid.required.error");
    }

    @Test
    void validate_gradeZ_addsWarning() {
        LineItem line = lineWith(l -> l.grade = "Z");

        ValidationResult result = validator.validate(line, "SAL", INV_DATE);

        assertHasWarning(result, "invoice.grade.z.warning");
    }

    // ---------------------------------------------------------------
    // checkNumberOfPieces
    // ---------------------------------------------------------------

    @Test
    void validate_zeroPieces_addsError() {
        LineItem line = lineWith(l -> l.numOfPieces = 0);

        ValidationResult result = validator.validate(line, "SAL", INV_DATE);

        assertHasError(result, "invoice.numberof.pieces.negative.or.zero.error");
    }

    @Test
    void validate_negativePieces_addsError() {
        LineItem line = lineWith(l -> l.numOfPieces = -5);

        ValidationResult result = validator.validate(line, "SAL", INV_DATE);

        assertHasError(result, "invoice.numberof.pieces.negative.or.zero.error");
    }

    @Test
    void validate_negativePieces_onAdj_isExempt() {
        LineItem line = lineWith(l -> l.numOfPieces = -5);

        ValidationResult result = validator.validate(line, "ADJ", INV_DATE);

        assertNoError(result, "invoice.numberof.pieces.negative.or.zero.error");
    }

    // ---------------------------------------------------------------
    // checkVolume
    // ---------------------------------------------------------------

    @Test
    void validate_negativeVolume_addsError() {
        LineItem line = lineWith(l -> l.volume = new BigDecimal("-1.00"));

        ValidationResult result = validator.validate(line, "SAL", INV_DATE);

        assertHasError(result, "invoice.volume.negative.value.error");
    }

    @Test
    void validate_zeroVolume_addsWarning() {
        LineItem line = lineWith(l -> l.volume = BigDecimal.ZERO);

        ValidationResult result = validator.validate(line, "SAL", INV_DATE);

        assertHasWarning(result, "invoice.volume.zero.value.warning");
    }

    @Test
    void validate_negativeVolume_onAdj_isExempt() {
        LineItem line = lineWith(l -> l.volume = new BigDecimal("-1.00"));

        ValidationResult result = validator.validate(line, "ADJ", INV_DATE);

        assertNoError(result, "invoice.volume.negative.value.error");
    }

    // ---------------------------------------------------------------
    // checkPrice
    // ---------------------------------------------------------------

    @Test
    void validate_negativePrice_addsError() {
        LineItem line = lineWith(l -> l.price = new BigDecimal("-1.00"));

        ValidationResult result = validator.validate(line, "SAL", INV_DATE);

        assertHasError(result, "invoice.price.negative.value.error");
    }

    @Test
    void validate_zeroPrice_addsWarning() {
        LineItem line = lineWith(l -> l.price = BigDecimal.ZERO);

        ValidationResult result = validator.validate(line, "SAL", INV_DATE);

        assertHasWarning(result, "invoice.price.zero.value.warning");
    }

    @Test
    void validate_negativePrice_onAdj_isExempt() {
        LineItem line = lineWith(l -> l.price = new BigDecimal("-1.00"));

        ValidationResult result = validator.validate(line, "ADJ", INV_DATE);

        assertNoError(result, "invoice.price.negative.value.error");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private LineItem validLine() {
        return new Line().build();
    }

    private LineItem lineWith(Consumer<Line> override) {
        Line l = new Line();
        override.accept(l);
        return l.build();
    }

    private static class Line {
        Long lineItemID = 1L;
        Long invoiceID = 100L;
        String secondSort = "SORT01";
        String species = "SP1";
        String grade = "G1";
        Integer numOfPieces = 10;
        BigDecimal price = new BigDecimal("25.00");
        BigDecimal volume = new BigDecimal("5.00");
        BigDecimal convertedPrice = null;
        BigDecimal amount = new BigDecimal("125.00");

        LineItem build() {
            return new LineItem(lineItemID, invoiceID, secondSort, secondSort, species, null, grade,
                    numOfPieces, price, volume, convertedPrice, amount);
        }
    }

    private void assertHasError(ValidationResult r, String key) {
        assertThat(r.errors().stream().map(ValidationMessage::messageKey).toList())
                .as("expected error with key %s", key)
                .contains(key);
    }

    private void assertHasWarning(ValidationResult r, String key) {
        assertThat(r.warnings().stream().map(ValidationMessage::messageKey).toList())
                .as("expected warning with key %s", key)
                .contains(key);
    }

    private void assertNoError(ValidationResult r, String key) {
        assertThat(r.errors().stream().map(ValidationMessage::messageKey).toList())
                .as("expected no error with key %s", key)
                .doesNotContain(key);
    }
}
