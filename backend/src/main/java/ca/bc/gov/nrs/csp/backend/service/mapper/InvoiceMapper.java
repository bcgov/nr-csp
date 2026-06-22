package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.CreateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItemRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItemResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.UpdateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationMessageResponse;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Component
public class InvoiceMapper {

    private final MessageSource messageSource;

    public InvoiceMapper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /** Builds the domain {@link InvoiceDetails} (validator input) from a create request. */
    public InvoiceDetails toDetails(CreateInvoiceRequest req, String entryUserID) {
        return new InvoiceDetails(
                null,
                req.invNumber(),
                req.invoiceDate(),
                null,                       // invStatus is server-controlled (DFT on create)
                req.invType(),
                req.maturity(),
                req.fobCode(),
                req.primarySortCode(),
                req.totalAmt(),
                req.totalPieces(),
                req.totalVol(),
                req.submitterClientNum(),
                req.submitterLocation(),
                req.submittedBy(),
                req.clientNumber(),
                req.clientLocation(),
                req.otherClientNum(),
                req.otherClientLocation(),
                req.otherClientName(),
                req.otherClientCity(),
                req.otherClientProvState(),
                req.boomNumbers() == null ? List.of() : req.boomNumbers(),
                req.timberMarks() == null ? List.of() : req.timberMarks(),
                req.weightSlips() == null ? List.of() : req.weightSlips(),
                req.replaceInvNum(),
                req.adjustInvNum(),
                req.reviewComments(),
                req.submitComments(),
                entryUserID
        );
    }

    /** Builds the domain {@link InvoiceDetails} from an update request, carrying the existing invID and entryUserID. */
    public InvoiceDetails toDetails(UpdateInvoiceRequest req, Long invID, String existingStatus, String existingEntryUserID) {
        return new InvoiceDetails(
                invID,
                req.invNumber(),
                req.invoiceDate(),
                existingStatus,
                req.invType(),
                req.maturity(),
                req.fobCode(),
                req.primarySortCode(),
                req.totalAmt(),
                req.totalPieces(),
                req.totalVol(),
                req.submitterClientNum(),
                req.submitterLocation(),
                req.submittedBy(),
                req.clientNumber(),
                req.clientLocation(),
                req.otherClientNum(),
                req.otherClientLocation(),
                req.otherClientName(),
                req.otherClientCity(),
                req.otherClientProvState(),
                req.boomNumbers() == null ? List.of() : req.boomNumbers(),
                req.timberMarks() == null ? List.of() : req.timberMarks(),
                req.weightSlips() == null ? List.of() : req.weightSlips(),
                req.replaceInvNum(),
                req.adjustInvNum(),
                req.reviewComments(),
                req.submitComments(),
                existingEntryUserID
        );
    }

    public LineItem toLineItem(LineItemRequest req, Long invoiceId) {
        if (req == null) return null;
        // Description is resolved server-side from the species code lookup;
        // null on the inbound path, populated when the row is read back.
        return new LineItem(
                req.lineItemID(),
                invoiceId,
                req.secondSort(),
                req.clientSecondarySort(),
                req.species(),
                null,
                req.grade(),
                req.numOfPieces(),
                req.price(),
                req.volume(),
                req.convertedPrice(),
                computeAmount(req.volume(), req.price())
        );
    }

    public List<LineItem> toLineItems(List<LineItemRequest> requests, Long invoiceId) {
        if (requests == null) return List.of();
        return requests.stream().map(r -> toLineItem(r, invoiceId)).toList();
    }

    public LineItemResponse toLineItemResponse(LineItem line) {
        return new LineItemResponse(
                line.lineItemID(),
                line.invoiceID(),
                line.secondSort(),
                line.clientSecondarySort(),
                line.species(),
                line.speciesDescription(),
                line.grade(),
                line.numOfPieces(),
                line.price(),
                line.volume(),
                line.convertedPrice(),
                line.amount()
        );
    }

    public List<LineItemResponse> toLineItemResponses(List<LineItem> lines) {
        if (lines == null) return List.of();
        return lines.stream().map(this::toLineItemResponse).toList();
    }

    public ValidationMessageResponse toMessageResponse(ValidationMessage message) {
        return new ValidationMessageResponse(
                message.messageKey(),
                message.args(),
                message.type().name(),
                resolveMessageText(message.messageKey(), message.args())
        );
    }

    /**
     * Resolve a validation message key to its human-readable text via the
     * Spring {@link MessageSource} (backed by {@code messages.properties}).
     * If the key is missing from the bundle we fall back to the key itself
     * so the UI can still render something meaningful; this also makes it
     * easy to spot missing translations during development.
     */
    private String resolveMessageText(String key, Object[] args) {
        if (key == null || key.isBlank()) return "";
        try {
            return messageSource.getMessage(key, args, Locale.getDefault());
        } catch (NoSuchMessageException e) {
            return key;
        }
    }

    public List<ValidationMessageResponse> toWarningResponses(ValidationResult result) {
        if (result == null) return List.of();
        return result.warnings().stream().map(this::toMessageResponse).toList();
    }

    public List<ValidationMessageResponse> toErrorResponses(ValidationResult result) {
        if (result == null) return List.of();
        return result.errors().stream().map(this::toMessageResponse).toList();
    }

    public InvoiceResponse toResponse(InvoiceDetails details, Long submissionId, Long submissionNumber, List<LineItem> lineItems, ValidationResult validationSource) {
        return new InvoiceResponse(
                details.invID(),
                submissionId,
                submissionNumber,
                details.invNumber(),
                details.invoiceDate(),
                details.invStatus(),
                details.invType(),
                details.maturity(),
                details.fobCode(),
                details.primarySortCode(),
                details.totalAmt(),
                details.totalPieces(),
                details.totalVol(),
                details.submitterClientNum(),
                details.submitterLocation(),
                details.submittedBy(),
                details.clientNumber(),
                details.clientLocation(),
                details.otherClientNum(),
                details.otherClientLocation(),
                details.otherClientName(),
                details.otherClientCity(),
                details.otherClientProvState(),
                details.boomNumbers(),
                details.timberMarks(),
                details.weightSlips(),
                details.replaceInvNum(),
                details.adjustInvNum(),
                details.reviewComments(),
                details.submitComments(),
                details.entryUserID(),
                toLineItemResponses(lineItems),
                toWarningResponses(validationSource),
                toErrorResponses(validationSource)
        );
    }

    private static BigDecimal computeAmount(BigDecimal volume, BigDecimal price) {
        if (volume == null || price == null) return null;
        return volume.multiply(price);
    }
}
