package ca.bc.gov.nrs.csp.backend.invoice.manual;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.invoice.shared.model.InvoiceTotals;
import ca.bc.gov.nrs.csp.backend.invoice.shared.rules.InvoiceTotalsRuleSet;

import java.util.List;

/**
 * The manual (CRUD) channel's totals glue: it feeds the invoice's line items to
 * the shared {@link InvoiceTotalsRuleSet} calculation, both to validate the
 * invoice and to derive the totals stored on its header.
 *
 * <p>Unlike the electronic channel — where the client submits its own totals and
 * a mismatch against the line items is a genuine warning — the invoice screen has
 * no totals fields: it shows them as read-only sums of the line items. So on this
 * channel the stored totals are always derived, and every write re-derives them.
 */
public final class ManualInvoiceTotals {

    private ManualInvoiceTotals() {}

    /** Line items in the shape the shared totals rules consume (volume, price, pieces). */
    public static List<InvoiceTotals.Line> toTotalsLines(List<LineItem> lines) {
        if (lines == null) {
            return List.of();
        }
        return lines.stream()
                .map(l -> new InvoiceTotals.Line(
                        l.volume(), l.price(), l.numOfPieces() == null ? 0 : l.numOfPieces()))
                .toList();
    }

    /**
     * A copy of {@code details} whose total amount / pieces / volume are the ones
     * calculated from {@code lines}.
     *
     * <p>Skipping this left an invoice that was created empty (totals 0) and then
     * filled in through the line-item endpoints stored with zero totals:
     * Submission History and the reports read those columns, and the
     * totals-variance rules (I25/I27/I29) warned about a 0-vs-calculated mismatch
     * the user had no field to correct.
     */
    public static InvoiceDetails withCalculatedTotals(InvoiceDetails details, List<LineItem> lines) {
        InvoiceTotalsRuleSet.Calculated totals =
                InvoiceTotalsRuleSet.calculate(details.invType(), toTotalsLines(lines));
        return new InvoiceDetails(
                details.invID(), details.invNumber(), details.invoiceDate(), details.invStatus(),
                details.invType(), details.maturity(), details.fobCode(), details.primarySortCode(),
                details.clientPrimarySortCode(),
                totals.amount(), totals.pieces(), totals.volume(),
                details.submitterClientNum(), details.submitterLocation(), details.submittedBy(),
                details.clientNumber(), details.clientLocation(),
                details.otherClientNum(), details.otherClientLocation(),
                details.otherClientName(), details.otherClientCity(), details.otherClientProvState(),
                details.boomNumbers(), details.timberMarks(), details.weightSlips(),
                details.replaceInvNum(), details.adjustInvNum(),
                details.reviewComments(), details.submitComments(), details.entryUserID()
        );
    }
}
