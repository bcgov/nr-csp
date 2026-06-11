package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.exception.ValidationException;
import ca.bc.gov.nrs.csp.backend.repository.InboxRepository;
import ca.bc.gov.nrs.csp.backend.repository.ValidationLookupRepository;
import ca.bc.gov.nrs.csp.backend.service.model.InboxCriteria;
import ca.bc.gov.nrs.csp.backend.service.model.InboxRow;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;
import ca.bc.gov.nrs.csp.backend.util.validation.inbox.InboxCriteriaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Business logic for the Inbox search screen.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>String criteria normalisation: invoiceNum and submissionStatus (null-safe trim + UPPER)</li>
 *   <li>Criteria validation via {@link InboxCriteriaValidator}</li>
 *   <li>Delegates to {@link InboxRepository} for the actual query</li>
 * </ul>
 */
@Service
public class InboxService {

    private static final Logger log = LoggerFactory.getLogger(InboxService.class);

    private final InboxRepository inboxRepository;
    private final ValidationLookupRepository validationLookupRepository;

    public InboxService(InboxRepository inboxRepository,
                        ValidationLookupRepository validationLookupRepository) {
        this.inboxRepository = inboxRepository;
        this.validationLookupRepository = validationLookupRepository;
    }

    public Page<InboxRow> search(
            LocalDate submissionDateFrom,
            LocalDate submissionDateTo,
            String submittedBy,
            String submissionType,
            String submissionStatus,
            String invoiceNum,
            String submitterClientNum,
            String submitterLocNum,
            Pageable pageable) {

        // Normalise string criteria before validation so the validator and repository
        // always receive clean values.
        // Null or blank → null (filter skipped in repo), non-blank → trim + UPPER.
        String normalisedInvoiceNum      = normaliseUpperCase(invoiceNum);
        String normalisedSubmissionStatus = normaliseUpperCase(submissionStatus);

        ValidationResult validation = new InboxCriteriaValidator(validationLookupRepository)
                .validate(submissionDateFrom, submissionDateTo,
                        submittedBy, submissionType, normalisedSubmissionStatus, normalisedInvoiceNum,
                        submitterClientNum, submitterLocNum);
        throwIfErrors(validation, "Inbox search criteria validation failed.");

        InboxCriteria criteria = new InboxCriteria(
                submissionDateFrom,
                submissionDateTo,
                submittedBy,
                submissionType,
                normalisedSubmissionStatus,
                normalisedInvoiceNum,
                submitterClientNum,
                submitterLocNum
        );

        log.debug("Inbox search requested with criteria: {}", criteria);
        Page<InboxRow> results = inboxRepository.search(criteria, pageable);
        log.debug("Inbox search returned {} of {} result(s)", results.getNumberOfElements(), results.getTotalElements());
        return results;
    }

    private void throwIfErrors(ValidationResult result, String message) {
        if (result.hasErrors()) {
            throw new ValidationException(message, result);
        }
    }

    /**
     * Normalises a string criteria value.
     * Null or blank → null (repository will skip the filter).
     * Non-blank → trim whitespace and convert to uppercase.
     */
    private String normaliseUpperCase(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
