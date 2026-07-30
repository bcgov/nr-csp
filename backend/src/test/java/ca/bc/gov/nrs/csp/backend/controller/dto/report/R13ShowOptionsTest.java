package ca.bc.gov.nrs.csp.backend.controller.dto.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class R13ShowOptionsTest {

    private static R13ShowOptions allShown() {
        R13ShowOptions opts = new R13ShowOptions();
        opts.setShowSubmissionStatus(true);
        opts.setShowSubmissionNumber(true);
        opts.setShowApprovedBy(true);
        opts.setShowSubmissionMonthYear(true);
        opts.setShowApprovalMonthYear(true);
        opts.setShowSubmissionType(true);
        opts.setShowClientInvoiceDate(true);
        opts.setShowInvoiceNumber(true);
        opts.setShowInvoiceReplacesAdjusts(true);
        opts.setShowInvoiceBoomNumber(true);
        opts.setShowInvoiceTimberMark(true);
        opts.setShowInvoiceWeighSlip(true);
        opts.setShowInvoiceType(true);
        opts.setShowInvoiceStatus(true);
        opts.setShowSellerName(true);
        opts.setShowSellerNumber(true);
        opts.setShowBuyerName(true);
        opts.setShowBuyerNumber(true);
        opts.setShowMaturity(true);
        opts.setShowSpecies(true);
        opts.setShowSortCodeSecondary(true);
        opts.setShowGrade(true);
        opts.setShowFobPoint(true);
        opts.setShowPieces(true);
        opts.setShowVolume(true);
        opts.setShowAmount(true);
        opts.setShowSortCodePrimary(true);
        opts.setShowFlatPrice(true);
        opts.setShowSpreadPrice(true);
        opts.setShowPrice(true);
        opts.setShowReviewer(true);
        opts.setShowComments(true);
        opts.setShowEntryUserid(true);
        return opts;
    }

    @Test
    @DisplayName("defaults: every column hidden")
    void defaultsAreAllHidden() {
        R13ShowOptions opts = new R13ShowOptions();

        assertThat(opts.toShowMap()).hasSize(33).allSatisfy((k, v) -> assertThat(v).isFalse());
        assertThat(opts.hasAnyHiddenColumn()).isTrue();
        assertThat(opts.toCacheKey()).isEqualTo("0".repeat(33));
    }

    @Test
    @DisplayName("setters and getters round-trip for every column")
    void settersAndGettersRoundTrip() {
        R13ShowOptions opts = allShown();

        assertThat(opts.toShowMap()).hasSize(33).allSatisfy((k, v) -> assertThat(v).isTrue());
        assertThat(opts.hasAnyHiddenColumn()).isFalse();
        assertThat(opts.toCacheKey()).isEqualTo("1".repeat(33));
    }

    @Test
    @DisplayName("toShowMap reflects all selections and hasAnyHiddenColumn is false when all shown")
    void toShowMapAllTrueWhenAllShown() {
        R13ShowOptions opts = allShown();

        Map<String, Boolean> map = opts.toShowMap();

        assertThat(map).hasSize(33).allSatisfy((k, v) -> assertThat(v).isTrue());
        assertThat(opts.hasAnyHiddenColumn()).isFalse();
        assertThat(opts.toCacheKey()).isEqualTo("1".repeat(33));
    }

    @Test
    @DisplayName("toCacheKey encodes each column in declaration order")
    void toCacheKeyEncodesSelections() {
        R13ShowOptions opts = new R13ShowOptions();
        opts.setShowSubmissionStatus(true); // first entry in the map

        assertThat(opts.toCacheKey()).isEqualTo("1" + "0".repeat(32));
    }

    @Test
    @DisplayName("null values behave as hidden")
    void nullValuesTreatedAsHidden() {
        R13ShowOptions opts = allShown();
        opts.setShowPrice(null);

        assertThat(opts.getShowPrice()).isNull();
        assertThat(opts.toShowMap().get("SHOW_PRICE")).isFalse();
        assertThat(opts.hasAnyHiddenColumn()).isTrue();
    }

    @Test
    @DisplayName("toString includes column selections")
    void toStringIncludesSelections() {
        R13ShowOptions opts = new R13ShowOptions();
        opts.setShowInvoiceNumber(true);

        String s = opts.toString();

        assertThat(s)
                .contains("showInvoiceNumber=true")
                .contains("showSellerName=false")
                .contains("showEntryUserid=false");
    }
}
