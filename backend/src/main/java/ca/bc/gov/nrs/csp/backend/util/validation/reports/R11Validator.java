package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R11ReportRequest;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class R11Validator {

    private final List<ValidationMessage> messages = new ArrayList<>();

    public ValidationResult validate(R11ReportRequest r) {
        messages.clear();

        if (isBlank(r.getDateFrom())) {
            addError("report.startdate.required.error", null);
        }

        if (isBlank(r.getModelingCode())) {
            addError("report.r11.reporttype.required.error", null);
        }

        if (isDateRangeOutOfOrder(r.getDateFrom(), r.getDateTo())) {
            addError("report.daterange.order.error", null);
        }

        return new ValidationResult(messages);
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
