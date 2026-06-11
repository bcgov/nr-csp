package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R07ReportRequest;
import ca.bc.gov.nrs.csp.backend.repository.CspSubmissionRepository;
import ca.bc.gov.nrs.csp.backend.service.SearchService;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class R07Validator {

    private final CspSubmissionRepository cspSubmissionRepository;
    private final SearchService searchService;
    private final List<ValidationMessage> messages = new ArrayList<>();

    public R07Validator(CspSubmissionRepository cspSubmissionRepository, SearchService searchService) {
        this.cspSubmissionRepository = cspSubmissionRepository;
        this.searchService = searchService;
    }

    public ValidationResult validate(R07ReportRequest r) {
        messages.clear();

        boolean hasYearMonth  = r.getYear() != null && r.getMonth() != null;
        boolean hasDateFrom   = r.getDateFrom() != null;
        boolean hasSeller     = !isBlank(r.getSellerClientNumber()) || !isBlank(r.getSellerClientName());
        boolean hasBuyer      = !isBlank(r.getBuyerClientNumber()) || !isBlank(r.getBuyerClientName());
        boolean hasSubmission = !isBlank(r.getSubmissionNumber());

        if (!hasYearMonth && !hasDateFrom && !hasSeller && !hasBuyer && !hasSubmission) {
            addError("report.r07.filter.required.error", null);
        }

        if (isNotNumeric(r.getSubmissionNumber())) {
            addError("report.submissionnumber.numeric.error", null);
        }

        validateSubmissionExists(r.getSubmissionNumber());
        validateClient(r.getSellerClientName(), r.getSellerClientNumber(), "seller");
        validateClient(r.getBuyerClientName(), r.getBuyerClientNumber(), "buyer");

        if (isDateRangeOutOfOrder(r.getDateFrom(), r.getDateTo())) {
            addError("report.daterange.order.error", null);
        }

        return new ValidationResult(messages);
    }

    private void validateClient(String name, String number, String role) {
        if (!isBlank(number)) {
            if (searchService.findClientsByNumber(number).isEmpty()) {
                addError("report.client.number.notfound.error", new Object[]{role, number});
            }
            return;
        }
        if (!isBlank(name)) {
            List<ClientLocation> results = searchService.findClientsByName(name);
            if (results.isEmpty()) {
                addError("report.client.name.notfound.error", new Object[]{role, name});
            } else if (!matchesEnteredName(results.get(0), name)) {
                addError("report.client.name.nomatch.error", new Object[]{name.trim(), role});
            }
        }
    }

    private static boolean matchesEnteredName(ClientLocation client, String enteredName) {
        String clientName = client.clientName() == null ? "" : client.clientName();
        String locnName = client.clientLocnName() == null ? "" : client.clientLocnName();
        String composite = (clientName + "," + locnName).toUpperCase();
        return composite.startsWith(enteredName.trim().toUpperCase());
    }

    private void validateSubmissionExists(String submissionNumber) {
        if (isBlank(submissionNumber) || isNotNumeric(submissionNumber)) {
            return;
        }
        long parsed;
        try {
            parsed = Long.parseLong(submissionNumber.trim());
        } catch (NumberFormatException e) {
            addError("report.submissionnumber.numeric.error", null);
            return;
        }
        if (!cspSubmissionRepository.existsBySubmissionNumber(parsed)) {
            addError("report.submissionnumber.notfound.error", new Object[]{submissionNumber.trim()});
        }
    }

    private boolean isDateRangeOutOfOrder(String dateFrom, String dateTo) {
        return dateFrom != null && dateTo != null && dateFrom.compareTo(dateTo) > 0;
    }

    private boolean isNotNumeric(String value) {
        return !isBlank(value) && !value.trim().matches("\\d+");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void addError(String key, Object[] args) {
        messages.add(new ValidationMessage(key, args == null ? null : Arrays.copyOf(args, args.length), MessageType.ERROR));
    }
}
