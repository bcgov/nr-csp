package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.repository.FlatPriceConversionRepository;
import ca.bc.gov.nrs.csp.backend.repository.LookupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PriceConversionServiceTest {

    @Mock
    FlatPriceConversionRepository factorRepo;

    @Mock
    LookupRepository lookupRepo;

    @InjectMocks
    PriceConversionService service;

    private static final LocalDate INV_DATE = LocalDate.of(2026, Month.JANUARY, 15);

    @BeforeEach
    void defaultGrouping() {
        // By default a species has no group mapping → grouping falls back to the
        // species code itself. Individual tests override as needed.
        lenient().when(lookupRepo.findSpeciesGroupCode(anyString())).thenReturn(Optional.empty());
    }

    private LineItem line(long id, String sort, String species, String grade, String volume, String price) {
        return new LineItem(id, 1L, sort, null, species, null, grade,
                10, new BigDecimal(price), new BigDecimal(volume), null, null);
    }

    @Test
    void apply_withNoLines_returnsEmpty() {
        PriceConversionService.Result result = service.apply(List.of(), "O", INV_DATE);
        assertThat(result.lines()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void apply_singleLineWithFactor_convertsToOwnPrice() {
        // A lone line's factor cancels out, so converted price == its own price.
        given(factorRepo.findApplicableFactor("O", "SORT01", "FIR", "1", INV_DATE)).willReturn(Optional.of(120));

        var result = service.apply(List.of(line(1, "SORT01", "FIR", "1", "5", "100")), "O", INV_DATE);

        assertThat(result.warnings()).isEmpty();
        assertThat(result.lines().get(0).convertedPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void apply_twoLinesDifferentFactors_spreadsTheFlatPrice() {
        given(factorRepo.findApplicableFactor("O", "SORT01", "FIR", "1", INV_DATE)).willReturn(Optional.of(120));
        given(factorRepo.findApplicableFactor("O", "SORT01", "FIR", "2", INV_DATE)).willReturn(Optional.of(80));

        var result = service.apply(
                List.of(line(1, "SORT01", "FIR", "1", "10", "100"), line(2, "SORT01", "FIR", "2", "30", "100")),
                "O", INV_DATE);

        // weighted avg factor = (10*120 + 30*80)/40 = 90 → converted = price * factor/avg
        assertThat(result.lines().get(0).convertedPrice()).isEqualByComparingTo("133.33"); // 100 * 120/90
        assertThat(result.lines().get(1).convertedPrice()).isEqualByComparingTo("88.89"); // 100 * 80/90
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void apply_zeroPrice_convertsToZero() {
        given(factorRepo.findApplicableFactor("O", "SORT01", "FIR", "1", INV_DATE)).willReturn(Optional.of(120));

        var result = service.apply(List.of(line(1, "SORT01", "FIR", "1", "5", "0")), "O", INV_DATE);

        assertThat(result.lines().get(0).convertedPrice()).isEqualByComparingTo("0.00");
    }

    @Test
    void apply_mapsMixedMaturityToOldGrowth() {
        given(factorRepo.findApplicableFactor(eq("O"), any(), any(), any(), any())).willReturn(Optional.of(120));

        service.apply(List.of(line(1, "SORT01", "FIR", "1", "5", "100")), "M", INV_DATE);

        // 'M' must be translated to 'O' for the factor lookup.
        verify(factorRepo).findApplicableFactor("O", "SORT01", "FIR", "1", INV_DATE);
    }

    @Test
    void apply_noFactorButSortCodeConfigured_warnsForMissingFactor() {
        given(factorRepo.findApplicableFactor(any(), any(), any(), any(), any())).willReturn(Optional.empty());
        given(factorRepo.existsForSortCode("SORT01")).willReturn(true);

        var result = service.apply(List.of(line(1, "SORT01", "FIR", "1", "5", "100")), "O", INV_DATE);

        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0).messageKey()).isEqualTo("invoice.conversion.factor.not.found.error");
        // The group's factor sum is null (no factors found) so, like the legacy
        // app, every line in the group is zeroed rather than left null.
        assertThat(result.lines().get(0).convertedPrice()).isEqualByComparingTo("0.00");
    }

    @Test
    void apply_mixedGroupSomeMissingFactors_leavesUnmatchedLineNull() {
        // One line has a factor, the other doesn't (but the sort code is configured):
        // the matched line spreads; the unmatched line is left null + warned.
        given(factorRepo.findApplicableFactor("O", "SORT01", "FIR", "1", INV_DATE)).willReturn(Optional.of(120));
        given(factorRepo.findApplicableFactor("O", "SORT01", "FIR", "2", INV_DATE)).willReturn(Optional.empty());
        given(factorRepo.existsForSortCode("SORT01")).willReturn(true);

        var result = service.apply(
                List.of(line(1, "SORT01", "FIR", "1", "10", "100"), line(2, "SORT01", "FIR", "2", "30", "100")),
                "O", INV_DATE);

        assertThat(result.lines().get(0).convertedPrice()).isNotNull();
        assertThat(result.lines().get(1).convertedPrice()).isNull();
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0).messageKey()).isEqualTo("invoice.conversion.factor.not.found.error");
    }

    @Test
    void apply_noFactorAndNoConfig_zeroesGroupWithoutWarning() {
        given(factorRepo.findApplicableFactor(any(), any(), any(), any(), any())).willReturn(Optional.empty());
        given(factorRepo.existsForSortCode("SORT01")).willReturn(false);

        var result = service.apply(List.of(line(1, "SORT01", "FIR", "1", "5", "100")), "O", INV_DATE);

        assertThat(result.warnings()).isEmpty();
        assertThat(result.lines().get(0).convertedPrice()).isEqualByComparingTo("0.00");
    }

    @Test
    void apply_outOfRange_warnsAndDropsValue() {
        // A lone line converts to its own price; a 6-integer-digit price is out of NUMBER(5,2) range.
        given(factorRepo.findApplicableFactor("O", "SORT01", "FIR", "1", INV_DATE)).willReturn(Optional.of(120));

        var result = service.apply(List.of(line(1, "SORT01", "FIR", "1", "5", "999999")), "O", INV_DATE);

        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0).messageKey()).isEqualTo("flat.price.conversion.out.of.range.error");
        assertThat(result.lines().get(0).convertedPrice()).isNull();
    }

    @Test
    void apply_groupsBySpeciesGroupCode() {
        // Two different species mapped to the same group, same sort + price → one group.
        given(lookupRepo.findSpeciesGroupCode("FIR")).willReturn(Optional.of("GRP"));
        given(lookupRepo.findSpeciesGroupCode("BAL")).willReturn(Optional.of("GRP"));
        given(factorRepo.findApplicableFactor("O", "SORT01", "FIR", "1", INV_DATE)).willReturn(Optional.of(120));
        given(factorRepo.findApplicableFactor("O", "SORT01", "BAL", "1", INV_DATE)).willReturn(Optional.of(80));

        var result = service.apply(
                List.of(line(1, "SORT01", "FIR", "1", "10", "100"), line(2, "SORT01", "BAL", "1", "30", "100")),
                "O", INV_DATE);

        // Spread across the combined group: weighted avg factor 90.
        assertThat(result.lines().get(0).convertedPrice()).isEqualByComparingTo("133.33");
        assertThat(result.lines().get(1).convertedPrice()).isEqualByComparingTo("88.89");
    }

    @Test
    void apply_zeroTotalVolumeGroup_isLeftUnconverted() {
        var result = service.apply(List.of(line(1, "SORT01", "FIR", "1", "0", "100")), "O", INV_DATE);
        assertThat(result.lines().get(0).convertedPrice()).isNull();
        assertThat(result.warnings()).isEmpty();
    }
}
