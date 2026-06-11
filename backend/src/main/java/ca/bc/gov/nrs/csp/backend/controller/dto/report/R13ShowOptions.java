package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Show/hide options for R13 report columns. Maps to the SHOW_ parameters that
 * control which columns are included in the JRXML before compilation.
 * Defaults to {@code false} (hide all) — callers must explicitly set shown columns to true.
 */
public class R13ShowOptions {

    private Boolean showSubmissionStatus = false;
    private Boolean showSubmissionNumber = false;
    private Boolean showApprovedBy = false;
    private Boolean showSubmissionMonthYear = false;
    private Boolean showApprovalMonthYear = false;
    private Boolean showSubmissionType = false;
    private Boolean showClientInvoiceDate = false;
    private Boolean showInvoiceNumber = false;
    private Boolean showInvoiceReplacesAdjusts = false;
    private Boolean showInvoiceBoomNumber = false;
    private Boolean showInvoiceTimberMark = false;
    private Boolean showInvoiceWeighSlip = false;
    private Boolean showInvoiceType = false;
    private Boolean showInvoiceStatus = false;
    private Boolean showSellerName = false;
    private Boolean showSellerNumber = false;
    private Boolean showBuyerName = false;
    private Boolean showBuyerNumber = false;
    private Boolean showMaturity = false;
    private Boolean showSpecies = false;
    private Boolean showSortCodeSecondary = false;
    private Boolean showGrade = false;
    private Boolean showFobPoint = false;
    private Boolean showPieces = false;
    private Boolean showVolume = false;
    private Boolean showAmount = false;
    private Boolean showSortCodePrimary = false;
    private Boolean showFlatPrice = false;
    private Boolean showSpreadPrice = false;
    private Boolean showPrice = false;
    private Boolean showReviewer = false;
    private Boolean showComments = false;
    private Boolean showEntryUserid = false;

    public Map<String, Boolean> toShowMap() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("SHOW_SUBMISSION_STATUS",        bv(showSubmissionStatus));
        map.put("SHOW_SUBMISSION_NUMBER",         bv(showSubmissionNumber));
        map.put("SHOW_APPROVED_BY",               bv(showApprovedBy));
        map.put("SHOW_SUBMISSION_MONTH_YEAR",     bv(showSubmissionMonthYear));
        map.put("SHOW_APPROVAL_MONTH_YEAR",       bv(showApprovalMonthYear));
        map.put("SHOW_SUBMISSION_TYPE",           bv(showSubmissionType));
        map.put("SHOW_CLIENT_INVOICE_DATE",       bv(showClientInvoiceDate));
        map.put("SHOW_INVOICE_NUMBER",            bv(showInvoiceNumber));
        map.put("SHOW_INVOICE_REPLACES_ADJUSTS",  bv(showInvoiceReplacesAdjusts));
        map.put("SHOW_INVOICE_BOOM_NUMBER",       bv(showInvoiceBoomNumber));
        map.put("SHOW_INVOICE_TIMBER_MARK",       bv(showInvoiceTimberMark));
        map.put("SHOW_INVOICE_WEIGH_SLIP",        bv(showInvoiceWeighSlip));
        map.put("SHOW_INVOICE_TYPE",              bv(showInvoiceType));
        map.put("SHOW_INVOICE_STATUS",            bv(showInvoiceStatus));
        map.put("SHOW_SELLER_NAME",               bv(showSellerName));
        map.put("SHOW_SELLER_NUMBER",             bv(showSellerNumber));
        map.put("SHOW_BUYER_NAME",                bv(showBuyerName));
        map.put("SHOW_BUYER_NUMBER",              bv(showBuyerNumber));
        map.put("SHOW_MATURITY",                  bv(showMaturity));
        map.put("SHOW_SPECIES",                   bv(showSpecies));
        map.put("SHOW_SORT_CODE_SECONDARY",       bv(showSortCodeSecondary));
        map.put("SHOW_GRADE",                     bv(showGrade));
        map.put("SHOW_FOB_POINT",                 bv(showFobPoint));
        map.put("SHOW_PIECES",                    bv(showPieces));
        map.put("SHOW_VOLUME",                    bv(showVolume));
        map.put("SHOW_AMOUNT",                    bv(showAmount));
        map.put("SHOW_SORT_CODE_PRIMARY",         bv(showSortCodePrimary));
        map.put("SHOW_FLAT_PRICE",                bv(showFlatPrice));
        map.put("SHOW_SPREAD_PRICE",              bv(showSpreadPrice));
        map.put("SHOW_PRICE",                     bv(showPrice));
        map.put("SHOW_REVIEWER",                  bv(showReviewer));
        map.put("SHOW_COMMENTS",                  bv(showComments));
        map.put("SHOW_ENTRY_USERID",              bv(showEntryUserid));
        return map;
    }

    public boolean hasAnyHiddenColumn() {
        return toShowMap().values().stream().anyMatch(v -> !v);
    }

    public String toCacheKey() {
        StringBuilder sb = new StringBuilder();
        toShowMap().values().forEach(v -> sb.append(v ? '1' : '0'));
        return sb.toString();
    }

    private boolean bv(Boolean b) { return b != null && b; }

    public Boolean getShowSubmissionStatus() { return showSubmissionStatus; }
    public void setShowSubmissionStatus(Boolean v) { this.showSubmissionStatus = v; }
    public Boolean getShowSubmissionNumber() { return showSubmissionNumber; }
    public void setShowSubmissionNumber(Boolean v) { this.showSubmissionNumber = v; }
    public Boolean getShowApprovedBy() { return showApprovedBy; }
    public void setShowApprovedBy(Boolean v) { this.showApprovedBy = v; }
    public Boolean getShowSubmissionMonthYear() { return showSubmissionMonthYear; }
    public void setShowSubmissionMonthYear(Boolean v) { this.showSubmissionMonthYear = v; }
    public Boolean getShowApprovalMonthYear() { return showApprovalMonthYear; }
    public void setShowApprovalMonthYear(Boolean v) { this.showApprovalMonthYear = v; }
    public Boolean getShowSubmissionType() { return showSubmissionType; }
    public void setShowSubmissionType(Boolean v) { this.showSubmissionType = v; }
    public Boolean getShowClientInvoiceDate() { return showClientInvoiceDate; }
    public void setShowClientInvoiceDate(Boolean v) { this.showClientInvoiceDate = v; }
    public Boolean getShowInvoiceNumber() { return showInvoiceNumber; }
    public void setShowInvoiceNumber(Boolean v) { this.showInvoiceNumber = v; }
    public Boolean getShowInvoiceReplacesAdjusts() { return showInvoiceReplacesAdjusts; }
    public void setShowInvoiceReplacesAdjusts(Boolean v) { this.showInvoiceReplacesAdjusts = v; }
    public Boolean getShowInvoiceBoomNumber() { return showInvoiceBoomNumber; }
    public void setShowInvoiceBoomNumber(Boolean v) { this.showInvoiceBoomNumber = v; }
    public Boolean getShowInvoiceTimberMark() { return showInvoiceTimberMark; }
    public void setShowInvoiceTimberMark(Boolean v) { this.showInvoiceTimberMark = v; }
    public Boolean getShowInvoiceWeighSlip() { return showInvoiceWeighSlip; }
    public void setShowInvoiceWeighSlip(Boolean v) { this.showInvoiceWeighSlip = v; }
    public Boolean getShowInvoiceType() { return showInvoiceType; }
    public void setShowInvoiceType(Boolean v) { this.showInvoiceType = v; }
    public Boolean getShowInvoiceStatus() { return showInvoiceStatus; }
    public void setShowInvoiceStatus(Boolean v) { this.showInvoiceStatus = v; }
    public Boolean getShowSellerName() { return showSellerName; }
    public void setShowSellerName(Boolean v) { this.showSellerName = v; }
    public Boolean getShowSellerNumber() { return showSellerNumber; }
    public void setShowSellerNumber(Boolean v) { this.showSellerNumber = v; }
    public Boolean getShowBuyerName() { return showBuyerName; }
    public void setShowBuyerName(Boolean v) { this.showBuyerName = v; }
    public Boolean getShowBuyerNumber() { return showBuyerNumber; }
    public void setShowBuyerNumber(Boolean v) { this.showBuyerNumber = v; }
    public Boolean getShowMaturity() { return showMaturity; }
    public void setShowMaturity(Boolean v) { this.showMaturity = v; }
    public Boolean getShowSpecies() { return showSpecies; }
    public void setShowSpecies(Boolean v) { this.showSpecies = v; }
    public Boolean getShowSortCodeSecondary() { return showSortCodeSecondary; }
    public void setShowSortCodeSecondary(Boolean v) { this.showSortCodeSecondary = v; }
    public Boolean getShowGrade() { return showGrade; }
    public void setShowGrade(Boolean v) { this.showGrade = v; }
    public Boolean getShowFobPoint() { return showFobPoint; }
    public void setShowFobPoint(Boolean v) { this.showFobPoint = v; }
    public Boolean getShowPieces() { return showPieces; }
    public void setShowPieces(Boolean v) { this.showPieces = v; }
    public Boolean getShowVolume() { return showVolume; }
    public void setShowVolume(Boolean v) { this.showVolume = v; }
    public Boolean getShowAmount() { return showAmount; }
    public void setShowAmount(Boolean v) { this.showAmount = v; }
    public Boolean getShowSortCodePrimary() { return showSortCodePrimary; }
    public void setShowSortCodePrimary(Boolean v) { this.showSortCodePrimary = v; }
    public Boolean getShowFlatPrice() { return showFlatPrice; }
    public void setShowFlatPrice(Boolean v) { this.showFlatPrice = v; }
    public Boolean getShowSpreadPrice() { return showSpreadPrice; }
    public void setShowSpreadPrice(Boolean v) { this.showSpreadPrice = v; }
    public Boolean getShowPrice() { return showPrice; }
    public void setShowPrice(Boolean v) { this.showPrice = v; }
    public Boolean getShowReviewer() { return showReviewer; }
    public void setShowReviewer(Boolean v) { this.showReviewer = v; }
    public Boolean getShowComments() { return showComments; }
    public void setShowComments(Boolean v) { this.showComments = v; }
    public Boolean getShowEntryUserid() { return showEntryUserid; }
    public void setShowEntryUserid(Boolean v) { this.showEntryUserid = v; }

    @Override
    public String toString() {
        return "showSubmissionStatus=" + showSubmissionStatus +
               " showSubmissionNumber=" + showSubmissionNumber +
               " showApprovedBy=" + showApprovedBy +
               " showSubmissionMonthYear=" + showSubmissionMonthYear +
               " showApprovalMonthYear=" + showApprovalMonthYear +
               " showSubmissionType=" + showSubmissionType +
               " showClientInvoiceDate=" + showClientInvoiceDate +
               " showInvoiceNumber=" + showInvoiceNumber +
               " showInvoiceReplacesAdjusts=" + showInvoiceReplacesAdjusts +
               " showInvoiceBoomNumber=" + showInvoiceBoomNumber +
               " showInvoiceTimberMark=" + showInvoiceTimberMark +
               " showInvoiceWeighSlip=" + showInvoiceWeighSlip +
               " showInvoiceType=" + showInvoiceType +
               " showInvoiceStatus=" + showInvoiceStatus +
               " showSellerName=" + showSellerName +
               " showSellerNumber=" + showSellerNumber +
               " showBuyerName=" + showBuyerName +
               " showBuyerNumber=" + showBuyerNumber +
               " showMaturity=" + showMaturity +
               " showSpecies=" + showSpecies +
               " showSortCodeSecondary=" + showSortCodeSecondary +
               " showGrade=" + showGrade +
               " showFobPoint=" + showFobPoint +
               " showPieces=" + showPieces +
               " showVolume=" + showVolume +
               " showAmount=" + showAmount +
               " showSortCodePrimary=" + showSortCodePrimary +
               " showFlatPrice=" + showFlatPrice +
               " showSpreadPrice=" + showSpreadPrice +
               " showPrice=" + showPrice +
               " showReviewer=" + showReviewer +
               " showComments=" + showComments +
               " showEntryUserid=" + showEntryUserid;
    }
}
