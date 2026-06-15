package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R10ReportRequest;
import ca.bc.gov.nrs.csp.backend.service.SearchService;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class R10Validator {

    private final SearchService searchService;
    private final List<ValidationMessage> messages = new ArrayList<>();

    public R10Validator(SearchService searchService) {
        this.searchService = searchService;
    }

    public ValidationResult validate(R10ReportRequest r) {
        messages.clear();

        if (isBlank(r.getDateFrom())) {
            addError("report.startdate.required.error", null);
        }

        if (r.getDateTo() == null && isBlank(r.getTimeFrame())) {
            addError("report.r10.enddate.or.timeframe.required.error", null);
        }

        validateClient(r.getSellerClientNumber(), "seller");
        validateClient(r.getBuyerClientNumber(), "buyer");

        if (isDateRangeOutOfOrder(r.getDateFrom(), r.getDateTo())) {
            addError("report.daterange.order.error", null);
        }

        return new ValidationResult(messages);
    }

    private void validateClient(String number, String role) {
        if (!isBlank(number) && searchService.findClientsByNumber(number).isEmpty()) {
            addError("report.client.number.notfound.error", new Object[]{role, number});
        }
    }

    private boolean isDateRangeOutOfOrder(String dateFrom, String dateTo) {
        return dateFrom != null && dateTo != null && dateFrom.compareTo(dateTo) > 0;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void addError(String key, Object[] args) {
        messages.add(new ValidationMessage(key, args == null ? null : Arrays.copyOf(args, args.length), MessageType.ERROR));
    }
}
