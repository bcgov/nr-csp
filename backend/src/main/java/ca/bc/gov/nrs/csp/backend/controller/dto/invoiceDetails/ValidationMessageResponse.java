package ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.Objects;

/**
 * equals/hashCode/toString are overridden because the record default compares the
 * {@code Object[]} component by reference; responses with identical args must be equal.
 */
@Schema(description = "A single validation error or warning")
public record ValidationMessageResponse(
        @Schema(description = "Message key (matches an entry in messages.properties)", example = "invoice.totalamount.dismatch.warning")
        String messageKey,
        @Schema(description = "Positional arguments substituted into the translated message template")
        Object[] args,
        @Schema(description = "Severity", example = "WARNING", allowableValues = {"ERROR", "WARNING"})
        String type,
        @Schema(description = "Resolved human-readable text with args interpolated, ready to render", example = "Submitted total amount 1234.50 does not match the calculated total.")
        String message
) {

    @Override
    public boolean equals(Object o) {
        return o instanceof ValidationMessageResponse(String key, Object[] args1, String type1, String message1)
                && Objects.equals(messageKey, key)
                && Arrays.deepEquals(args, args1)
                && Objects.equals(type, type1)
                && Objects.equals(message, message1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageKey, Arrays.deepHashCode(args), type, message);
    }

    @Override
    public String toString() {
        return "ValidationMessageResponse[messageKey=" + messageKey
                + ", args=" + Arrays.deepToString(args)
                + ", type=" + type + ", message=" + message + "]";
    }
}
