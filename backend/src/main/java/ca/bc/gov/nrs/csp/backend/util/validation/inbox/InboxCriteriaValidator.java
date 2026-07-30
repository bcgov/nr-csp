package ca.bc.gov.nrs.csp.backend.util.validation.inbox;

import ca.bc.gov.nrs.csp.backend.repository.ValidationLookupRepository;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates inbox search criteria.
 *
 * <p>Mirrors the legacy {@code InboxCriteriaValidator.isValid()} logic
 * (InboxCriteriaValidator.java:32-60) using the project's ValidationResult pattern.</p>
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Date range: start must not be after end when both are provided.</li>
 *   <li>Submitted By: must be Buyer or Seller when provided.</li>
 *   <li>Submission Type: must be Electronic or Manual when provided.</li>
 *   <li>Submission Status: must be a known code in CSP_SUBMISSION_STATUS_CODE when provided.</li>
 *   <li>Invoice Number: must not exceed {@value #MAX_INVOICE_NUM_LENGTH} characters when provided.</li>
 *   <li>Client Number / Location: must be supplied as a pair — providing one without the other is rejected.</li>
 * </ul>
 */
public class InboxCriteriaValidator {

    static final String DATE_RANGE_KEY          = "inbox.dateperiod.error";
    static final String SUBMITTED_BY_KEY        = "inbox.submittedby.invalid.error";
    static final String SUBMISSION_TYPE_KEY     = "inbox.submissiontype.invalid.error";
    static final String SUBMISSION_STATUS_KEY   = "inbox.submissionstatus.invalid.error";
    static final String INVOICE_NUM_LENGTH_KEY  = "inbox.invoicenum.maxlength.error";
    static final String CLIENT_PAIR_KEY         = "inbox.clientnum.pair.required.error";

    static final int MAX_INVOICE_NUM_LENGTH = 15;

    private static final Set<String> VALID_SUBMITTED_BY     = Set.of("buyer", "seller");
    private static final Set<String> VALID_SUBMISSION_TYPES = Set.of("electronic", "manual");

    private final ValidationLookupRepository lookupRepository;
    private final List<ValidationMessage> messages = new ArrayList<>();

    public InboxCriteriaValidator(ValidationLookupRepository lookupRepository) {
        this.lookupRepository = lookupRepository;
    }

    /**
     * Validates all inbox search criteria.
     *
     * @param submissionDateFrom  start of date range (may be null)
     * @param submissionDateTo    end of date range (may be null)
     * @param submittedBy         Buyer or Seller (may be null/blank)
     * @param submissionType      Electronic or Manual (may be null/blank)
     * @param submissionStatus    status code to look up in DB (may be null/blank)
     * @param invoiceNum          already trimmed + uppercased by service (may be null)
     * @param submitterClientNum  client number (must be paired with submitterLocNum)
     * @param submitterLocNum     location code (must be paired with submitterClientNum)
     */
    public ValidationResult validate(
            LocalDate submissionDateFrom,
            LocalDate submissionDateTo,
            String submittedBy,
            String submissionType,
            String submissionStatus,
            String invoiceNum,
            String submitterClientNum,
            String submitterLocNum) {

        messages.clear();

        // Date range
        if (submissionDateFrom != null && submissionDateTo != null
                && submissionDateFrom.isAfter(submissionDateTo)) {
            addError(DATE_RANGE_KEY, null);
        }

        // Submitted By enum
        if (submittedBy != null && !submittedBy.isBlank()
                && !VALID_SUBMITTED_BY.contains(submittedBy.toLowerCase())) {
            addError(SUBMITTED_BY_KEY, null);
        }

        // Submission Type enum
        if (submissionType != null && !submissionType.isBlank()
                && !VALID_SUBMISSION_TYPES.contains(submissionType.toLowerCase())) {
            addError(SUBMISSION_TYPE_KEY, null);
        }

        // Submission Status — DB lookup
        if (submissionStatus != null && !submissionStatus.isBlank()
                && !lookupRepository.existsSubmissionStatusCode(submissionStatus)) {
            addError(SUBMISSION_STATUS_KEY, new Object[]{submissionStatus});
        }

        // Invoice number: total length including wildcard characters must not exceed the column width.
        if (invoiceNum != null && invoiceNum.length() > MAX_INVOICE_NUM_LENGTH) {
            addError(INVOICE_NUM_LENGTH_KEY, new Object[]{MAX_INVOICE_NUM_LENGTH});
        }

        // Client number + location must be supplied as a pair.
        // In the legacy app the autocomplete always selected both together; partial
        // input changes query semantics (client-wide instead of specific location).
        boolean hasClientNum = submitterClientNum != null && !submitterClientNum.isBlank();
        boolean hasLocNum    = submitterLocNum    != null && !submitterLocNum.isBlank();
        if (hasClientNum != hasLocNum) {
            addError(CLIENT_PAIR_KEY, null);
        }

        return new ValidationResult(messages);
    }

    private void addError(String key, Object[] args) {
        messages.add(new ValidationMessage(key, args, MessageType.ERROR));
    }
}
