package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the content-aware equals/hashCode/toString added for the Object[] component.
 */
class ValidationMessageResponseTest {

    private static ValidationMessageResponse sample() {
        return new ValidationMessageResponse(
                "invoice.total.mismatch", new Object[]{"1234.50"}, "WARNING", "Total mismatch: 1234.50");
    }

    @Test
    void equals_equalContent_isTrue_andHashCodesMatch() {
        assertThat(sample()).isEqualTo(sample()).hasSameHashCodeAs(sample());
    }

    @Test
    void equals_arrayContentCompared_notReference() {
        ValidationMessageResponse a = new ValidationMessageResponse("k", new Object[]{new Object[]{"n"}}, "ERROR", "m");
        ValidationMessageResponse b = new ValidationMessageResponse("k", new Object[]{new Object[]{"n"}}, "ERROR", "m");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_anyDifferingComponent_isFalse() {
        ValidationMessageResponse base = sample();
        assertThat(base)
                .isNotEqualTo(new ValidationMessageResponse("other", new Object[]{"1234.50"}, "WARNING", "Total mismatch: 1234.50"))
                .isNotEqualTo(new ValidationMessageResponse("invoice.total.mismatch", new Object[]{"9"}, "WARNING", "Total mismatch: 1234.50"))
                .isNotEqualTo(new ValidationMessageResponse("invoice.total.mismatch", new Object[]{"1234.50"}, "ERROR", "Total mismatch: 1234.50"))
                .isNotEqualTo(new ValidationMessageResponse("invoice.total.mismatch", new Object[]{"1234.50"}, "WARNING", "other text"));
    }

    @Test
    void equals_nullAndOtherType_isFalse() {
        ValidationMessageResponse r = sample();
        assertThat(r).isNotEqualTo(null).isNotEqualTo("not a response");
    }

    @Test
    void toString_containsAllComponents() {
        assertThat(sample()).hasToString(
                "ValidationMessageResponse[messageKey=invoice.total.mismatch, args=[1234.50],"
                        + " type=WARNING, message=Total mismatch: 1234.50]");
    }
}
