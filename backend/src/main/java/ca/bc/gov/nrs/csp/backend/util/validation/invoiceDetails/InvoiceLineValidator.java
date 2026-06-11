package ca.bc.gov.nrs.csp.backend.util.validation.invoiceDetails;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
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
        checkGrade(line.grade());
        checkNumberOfPieces(line.numOfPieces());
        checkVolume(line.volume());
        checkPrice(line.price());

        return new ValidationResult(messages);
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

    private boolean checkGrade(String grade) {
        if (grade == null) {
            log.debug("InvoiceLineValidator: missing grade");
            addError("invoice.grade.invalid.required.error", new Object[]{lineLabel});
            return false;
        }
        if (grade.equals("Z")) {
            log.debug("InvoiceLineValidator: grade Z warning");
            addWarning("invoice.grade.z.warning", new Object[]{lineLabel});
        }
        return true;
    }

    private boolean checkNumberOfPieces(Integer numOfPieces) {
        if (ConstantsCode.INVTYPE_ADJUST.equals(invType)) return true;
        if (numOfPieces == null || numOfPieces <= 0) {
            log.debug("InvoiceLineValidator: invalid number of pieces");
            addError("invoice.numberof.pieces.negative.or.zero.error", new Object[]{lineLabel});
            return false;
        }
        return true;
    }

    private boolean checkVolume(BigDecimal volume) {
        if (ConstantsCode.INVTYPE_ADJUST.equals(invType)) return true;
        if (volume == null || volume.signum() < 0) {
            log.debug("InvoiceLineValidator: negative or missing volume");
            addError("invoice.volume.negative.value.error", new Object[]{lineLabel});
            return false;
        }
        if (volume.signum() == 0) {
            log.debug("InvoiceLineValidator: zero volume warning");
            addWarning("invoice.volume.zero.value.warning", new Object[]{lineLabel});
        }
        return true;
    }

    private boolean checkPrice(BigDecimal price) {
        if (ConstantsCode.INVTYPE_ADJUST.equals(invType)) return true;
        if (price == null || price.signum() < 0) {
            log.debug("InvoiceLineValidator: negative or missing price");
            addError("invoice.price.negative.value.error", new Object[]{lineLabel});
            return false;
        }
        if (price.signum() == 0) {
            log.debug("InvoiceLineValidator: zero price warning");
            addWarning("invoice.price.zero.value.warning", new Object[]{lineLabel});
        }
        return true;
    }

    private void addError(String key, Object[] args) {
        messages.add(new ValidationMessage(key, args, MessageType.ERROR));
    }

    private void addWarning(String key, Object[] args) {
        messages.add(new ValidationMessage(key, args, MessageType.WARNING));
    }
}
