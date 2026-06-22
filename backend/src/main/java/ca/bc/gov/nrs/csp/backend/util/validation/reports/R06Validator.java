package ca.bc.gov.nrs.csp.backend.util.validation.reports;

import ca.bc.gov.nrs.csp.backend.controller.dto.report.R06ReportRequest;
import ca.bc.gov.nrs.csp.backend.service.SearchService;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class R06Validator {

    private static final int INVOICE_NUMBER_MAX_LENGTH = 15;

    private final SearchService searchService;
    private final List<ValidationMessage> messages = new ArrayList<>();

    public R06Validator(SearchService searchService) {
        this.searchService = searchService;
    }

    public ValidationResult validate(R06ReportRequest r) {
        messages.clear();

        boolean hasInvoiceNumbers = !isBlank(r.getInvoiceNumbers());
        if (!hasInvoiceNumbers) {
            if (isBlank(r.getDateFrom())) {
                addError("report.r06.startdate.required.error", null);
            }
            if (isBlank(r.getDateTo())) {
                addError("report.r06.enddate.required.error", null);
            }
        }

        validateInvoiceNumberLengths(r.getInvoiceNumbers());
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

    private void validateInvoiceNumberLengths(String invoiceNumbers) {
        if (isBlank(invoiceNumbers)) {
            return;
        }
        for (String token : invoiceNumbers.split(",")) {
            if (token.trim().length() > INVOICE_NUMBER_MAX_LENGTH) {
                addError("report.r06.invoicenumber.length.error", new Object[]{token.trim()});
                return;
            }
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
