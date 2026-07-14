package ca.bc.gov.nrs.csp.backend.invoice.manual;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.Finding;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.InvoiceLine;
import ca.bc.gov.nrs.csp.backend.invoice.shared.rules.InvoiceLineRuleSet;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.Severity;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceLineValidator {

    private static final Logger log = LoggerFactory.getLogger(InvoiceLineValidator.class);

    private final CommonValidation commonValidation;
    private final List<ValidationMessage> messages = new ArrayList<>();

    private String invType;
    private LocalDate invDate;
    private String lineLabel;

    public InvoiceLineValidator(CommonValidation commonValidation) {
        this.commonValidation = commonValidation;
    }

    public ValidationResult validate(LineItem line, String invType, LocalDate invDate) {
        messages.clear();
        this.invType = invType;
        this.invDate = invDate;
        this.lineLabel = (line != null && line.lineItemID() != null)
                ? "Line #" + line.lineItemID()
                : "Line #New";

        if (line == null) {
            addError("invoice.lineitem.missing.error", new Object[]{lineLabel});
            return new ValidationResult(messages);
        }

        isValidSortCode(line.secondSort(), "invoice.secondry.sortcode.invalid.error");
        checkSpeciesGradeCombination(line.species(), line.grade());
        checkLineValues(line);

        return new ValidationResult(messages);
    }

    private void checkLineValues(LineItem line) {
        InvoiceLine coreLine = new InvoiceLine(invType, lineLabel,
                line.grade(), line.numOfPieces(), line.volume(), line.price());
        for (Finding f : InvoiceLineRuleSet.validate(coreLine)) {
            if (f.severity() == Severity.ERROR) {
                log.debug("InvoiceLineValidator: line rule failed: {}", f.code());
                addError(f.code(), f.args());
            } else {
                addWarning(f.code(), f.args());
            }
        }
    }

    private boolean isValidSortCode(String sortCode, String messageKey) {
        boolean ok = commonValidation.isValidSortCode(sortCode, invDate);
        if (!ok) {
            log.debug("InvoiceLineValidator: invalid sort code {}", sortCode);
            addError(messageKey, new Object[]{sortCode, invDate, lineLabel});
        }
        return ok;
    }

    private boolean checkSpeciesGradeCombination(String species, String grade) {
        boolean ok = commonValidation.isValidSpeciesGradeCombination(species, grade);
        if (!ok) {
            log.debug("InvoiceLineValidator: invalid species/grade combination {}/{}", species, grade);
            addError("invoice.species.grade.combination.error", new Object[]{species, grade, lineLabel});
        }
        return ok;
    }

    private void addError(String key, Object[] args) {
        messages.add(new ValidationMessage(key, args, MessageType.ERROR));
    }

    private void addWarning(String key, Object[] args) {
        messages.add(new ValidationMessage(key, args, MessageType.WARNING));
    }
}
