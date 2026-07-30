package ca.bc.gov.nrs.csp.backend.util.validation;

import java.util.Arrays;
import java.util.Objects;

/**
 * equals/hashCode/toString are overridden because the record default compares the
 * {@code Object[]} component by reference; messages with identical args must be equal.
 */
public record ValidationMessage(String messageKey, Object[] args, MessageType type) {

    @Override
    public boolean equals(Object o) {
        return o instanceof ValidationMessage(String key, Object[] args1, MessageType type1)
                && Objects.equals(messageKey, key)
                && Arrays.deepEquals(args, args1)
                && type == type1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageKey, Arrays.deepHashCode(args), type);
    }

    @Override
    public String toString() {
        return "ValidationMessage[messageKey=" + messageKey
                + ", args=" + Arrays.deepToString(args) + ", type=" + type + "]";
    }
}
