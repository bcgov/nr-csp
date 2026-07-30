package ca.bc.gov.nrs.csp.backend.util.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the content-aware equals/hashCode/toString added for the Object[] component.
 */
class ValidationMessageTest {

    private static ValidationMessage sample() {
        return new ValidationMessage("invoice.total.mismatch", new Object[]{"A", 2}, MessageType.ERROR);
    }


    @Test
    void equals_equalContent_isTrue_andHashCodesMatch() {
        assertThat(sample())
                .isEqualTo(sample())
                .hasSameHashCodeAs(sample());
    }

    @Test
    void equals_arrayContentCompared_notReference() {
        ValidationMessage a = new ValidationMessage("k", new Object[]{"x", new Object[]{"nested"}}, MessageType.WARNING);
        ValidationMessage b = new ValidationMessage("k", new Object[]{"x", new Object[]{"nested"}}, MessageType.WARNING);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_differentKey_args_orType_isFalse() {
        ValidationMessage base = sample();
        assertThat(base)
                .isNotEqualTo(new ValidationMessage("other.key", new Object[]{"A", 2}, MessageType.ERROR))
                .isNotEqualTo(new ValidationMessage("invoice.total.mismatch", new Object[]{"B"}, MessageType.ERROR))
                .isNotEqualTo(new ValidationMessage("invoice.total.mismatch", new Object[]{"A", 2}, MessageType.WARNING));
    }

    @Test
    void equals_nullAndOtherType_isFalse() {
        ValidationMessage m = sample();
        assertThat(m).isNotEqualTo(null).isNotEqualTo("not a message");
    }

    @Test
    void equals_nullArgs_bothNull_isTrue() {
        ValidationMessage a = new ValidationMessage("k", null, MessageType.ERROR);
        ValidationMessage b = new ValidationMessage("k", null, MessageType.ERROR);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void toString_containsKeyArgsAndType() {
        assertThat(sample()).hasToString(
                "ValidationMessage[messageKey=invoice.total.mismatch, args=[A, 2], type=ERROR]");
    }
}
