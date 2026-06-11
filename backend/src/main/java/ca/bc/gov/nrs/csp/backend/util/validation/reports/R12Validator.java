package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R12ReportRequest;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class R12Validator {

    private final List<ValidationMessage> messages = new ArrayList<>();

    public ValidationResult validate(R12ReportRequest r) {
        messages.clear();

        if (r.getYear() == null) {
            if (isBlank(r.getDateFrom())) {
                addError("report.r12.startdate.required.error", null);
            }
            if (r.getDateTo() == null && isBlank(r.getTimeFrame())) {
                addError("report.r12.enddate.or.timeframe.required.error", null);
            }
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
