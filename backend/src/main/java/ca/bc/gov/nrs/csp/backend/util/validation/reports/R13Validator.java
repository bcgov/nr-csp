package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R13ReportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R13ShowOptions;
import ca.bc.gov.nrs.csp.backend.service.SearchService;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class R13Validator {

    private final SearchService searchService;
    private final List<ValidationMessage> messages = new ArrayList<>();

    public R13Validator(SearchService searchService) {
        this.searchService = searchService;
    }

    public ValidationResult validate(R13ReportRequest r) {
        messages.clear();

        validateShowOptions(r);
        validateFilterRequirement(r);
        validateDateOrdering(r);
        validateClientInfo(r.getSellerNumbers(), r.getSellerName());
        validateClientInfo(r.getBuyerNumbers(), r.getBuyerName());

        return new ValidationResult(messages);
    }

    // ── Show options ──────────────────────────────────────────────────────────

    private void validateShowOptions(R13ReportRequest r) {
        R13ShowOptions opts = r.getShowOptions() != null ? r.getShowOptions() : new R13ShowOptions();
        long selectedCount = opts.toShowMap().values().stream().filter(Boolean::booleanValue).count();
        if (selectedCount < 2) {
            addError("R13 requires at least 2 output columns to be selected in showOptions");
        }
    }

    // ── Filter requirement (at least one filter group must be present) ────────

    private void validateFilterRequirement(R13ReportRequest r) {
        boolean hasDateRange  = !isBlank(r.getInvoiceDateFrom());
        boolean hasSubmission = !isBlank(r.getSubmissionNumber())
                             || !isBlank(r.getSubmissionMonthYear())
                             || !isBlank(r.getEntryUserId());
        boolean hasClient     = !isEmpty(r.getSellerNumbers())
                             || !isEmpty(r.getBuyerNumbers())
                             || !isBlank(r.getSellerName())
                             || !isBlank(r.getBuyerName());
        if (!hasDateRange && !hasSubmission && !hasClient) {
            addError("R13 requires at least one of: invoice date range, submission number/month-year/entry user, or seller/buyer client");
        }
    }

    // ── Date ordering (explicit fields only; computed end-date is always ≥ from) ──

    private void validateDateOrdering(R13ReportRequest r) {
        if (!isBlank(r.getInvoiceDateFrom()) && !isBlank(r.getInvoiceDateTo())
                && r.getInvoiceDateFrom().compareTo(r.getInvoiceDateTo()) > 0) {
            addError("invoiceDateFrom must not be after invoiceDateTo");
        }
    }

    // ── Client validation (C7 / C8 / C9) ─────────────────────────────────────

    private void validateClientInfo(List<String> clientNumbers, String clientName) {
        boolean hasNumbers = !isEmpty(clientNumbers);
        boolean hasName    = !isBlank(clientName);
        if (!hasNumbers && !hasName) return;

        if (hasNumbers) {
            for (String number : clientNumbers) {
                List<ClientLocation> found = searchService.findClientsByNumber(number);
                if (found.isEmpty()) {
                    String msg = hasName
                            ? "The Client Number (" + number + ") or Client Name (" + clientName.trim() + ") cannot be found in CSP"
                            : "The Client Number (" + number + ") cannot be found in CSP";
                    addError(msg);
                    return;
                }
                if (hasName) {
                    String upperName = clientName.trim().toUpperCase();
                    boolean nameMatch = found.stream()
                            .anyMatch(cl -> cl.clientName() != null
                                    && cl.clientName().toUpperCase().startsWith(upperName));
                    if (!nameMatch) {
                        addError("The Client Name (" + clientName.trim() + ") cannot be found in CSP");
                        return;
                    }
                }
            }
        } else {
            List<ClientLocation> found = searchService.findClientsByName(clientName);
            if (found.isEmpty()) {
                addError("The Client Name (" + clientName.trim() + ") cannot be found in CSP");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private void addError(String message) {
        messages.add(new ValidationMessage(message, null, MessageType.ERROR));
    }
}
