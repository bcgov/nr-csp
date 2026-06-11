package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import ca.bc.gov.nrs.csp.backend.repository.FlatPriceConversionRepository;
import ca.bc.gov.nrs.csp.backend.repository.LookupRepository;
import ca.bc.gov.nrs.csp.backend.util.validation.MessageType;
import ca.bc.gov.nrs.csp.backend.util.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flat-price conversion ("price spread") engine — a faithful port of the legacy
 * {@code SubmissionTransferRules.fillPriceConversion / convertPrice}.
 *
 * When several species/grades under the same sort code are invoiced at a
 * single (flat) price, this spreads that price across them using the configured
 * conversion factors so each line gets a value-weighted "converted price" that
 * still averages back to the flat price by volume. A line with a non-null,
 * non-zero converted price is considered "converted" (price-conversion = Y);
 * otherwise it is left as a flat price (N).
 *
 * Algorithm:
 *
 *   Group lines by {@code (sortCode, speciesGroupCode, price)} — because
 *   price is part of the key each group is inherently flat-priced.
 *   For each group whose total volume &gt; 0, look up each line's factor
 *   (PRODUCT modelling, maturity with {@code M}&rarr;{@code O}, sort code,
 *   species, grade, effective on the invoice date).
 *   {@code calcFactorᵢ = (volumeᵢ / totalVolume) × factorᵢ × 0.01};
 *   {@code sum = Σ calcFactorᵢ}.
 *   If {@code sum != 0}:
 *       {@code convertedPriceᵢ = (price × factorᵢ × 0.01) / sum} (HALF_UP, 2dp);
 *       values outside {@code NUMBER(5,2)} are dropped with a warning.
 *       If {@code sum == 0} (or no factors found) every line in the group is set
 *       to 0.
 *
 */
@Service
public class PriceConversionService {

    private static final Logger log = LoggerFactory.getLogger(PriceConversionService.class);

    private static final String MATURITY_MIXED = "M";
    private static final String MATURITY_OLD_GROWTH = "O";
    private static final BigDecimal ONE_HUNDREDTH = new BigDecimal("0.01");
    private static final int RATIO_SCALE = 10;

    // Message keys ported verbatim from the legacy app's resource bundle.
    private static final String MSG_FACTOR_NOT_FOUND = "invoice.conversion.factor.not.found.error";
    private static final String MSG_OUT_OF_RANGE = "flat.price.conversion.out.of.range.error";

    private final FlatPriceConversionRepository factorRepo;
    private final LookupRepository lookupRepo;

    public PriceConversionService(FlatPriceConversionRepository factorRepo, LookupRepository lookupRepo) {
        this.factorRepo = factorRepo;
        this.lookupRepo = lookupRepo;
    }

    /** Result of a conversion run: the updated lines and any non-blocking warnings. */
    public record Result(List<LineItem> lines, List<ValidationMessage> warnings) {}

    /**
     * Compute the converted price for every line. The returned lines mirror the
     * input order with {@code convertedPrice} populated (null when not converted).
     */
    @Transactional(readOnly = true)
    public Result apply(List<LineItem> lines, String maturity, LocalDate invoiceDate) {
        log.info("Flat-price conversion requested lines={} maturity={} invoiceDate={}", lines == null ? 0 : lines.size(), maturity, invoiceDate);
        List<ValidationMessage> warnings = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            return new Result(List.of(), warnings);
        }

        String effectiveMaturity = MATURITY_MIXED.equals(maturity) ? MATURITY_OLD_GROWTH : maturity;

        // Resolve each species to its group code once (fallback = species code).
        Map<String, String> groupBySpecies = new HashMap<>();

        // Partition lines into (sortCode, speciesGroupCode, price) groups.
        Map<String, List<LineItem>> groups = new LinkedHashMap<>();
        for (LineItem line : lines) {
            String group = groupBySpecies.computeIfAbsent(line.species(),
                    s -> lookupRepo.findSpeciesGroupCode(s).orElse(s));
            String key = safe(line.secondSort()) + "::" + safe(group) + "::" + priceKey(line.price());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(line);
        }

        // Computed converted price keyed by line id (absent => leave null).
        Map<Long, BigDecimal> converted = new HashMap<>();
        for (List<LineItem> group : groups.values()) {
            convertGroup(group, effectiveMaturity, invoiceDate, converted, warnings);
        }

        List<LineItem> out = new ArrayList<>(lines.size());
        for (LineItem line : lines) {
            out.add(withConvertedPrice(line, converted.get(line.lineItemID())));
        }
        log.info("Flat-price conversion complete warnings={}", warnings.size());
        return new Result(out, warnings);
    }

    private void convertGroup(List<LineItem> group, String maturity, LocalDate invoiceDate,
                              Map<Long, BigDecimal> converted, List<ValidationMessage> warnings) {
        BigDecimal totalVolume = BigDecimal.ZERO;
        for (LineItem line : group) {
            totalVolume = totalVolume.add(nz(line.volume()));
        }
        if (totalVolume.compareTo(BigDecimal.ZERO) == 0) {
            return; // nothing to spread
        }

        String sortCode = group.get(0).secondSort();
        Map<Long, Integer> factorByLine = new HashMap<>();
        List<BigDecimal> calcFactors = new ArrayList<>();
        boolean missingFactorWarned = false;

        for (LineItem line : group) {
            Integer factor = factorRepo
                    .findApplicableFactor(maturity, sortCode, line.species(), line.grade(), invoiceDate)
                    .orElse(null);
            if (factor != null) {
                BigDecimal calc = nz(line.volume())
                        .divide(totalVolume, RATIO_SCALE, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(factor))
                        .multiply(ONE_HUNDREDTH);
                calcFactors.add(calc);
                factorByLine.put(line.lineItemID(), factor);
            } else if (!missingFactorWarned && factorRepo.existsForSortCode(sortCode)) {
                // A factor is configured for this sort code but not this exact
                // maturity/species/grade/date — surface once per group.
                warnings.add(new ValidationMessage(
                        MSG_FACTOR_NOT_FOUND,
                        new Object[] { maturity, sortCode, line.species(), line.grade(),
                                invoiceDate != null ? invoiceDate.toString() : null },
                        MessageType.WARNING));
                missingFactorWarned = true;
            }
        }

        // Legacy sumBigDecimalValues returns null for an empty list; both null
        // and zero fall through to the "set everything to zero" branch.
        BigDecimal sum = calcFactors.isEmpty() ? null : sum(calcFactors);
        if (sum != null && sum.compareTo(BigDecimal.ZERO) != 0) {
            for (LineItem line : group) {
                Integer factor = factorByLine.get(line.lineItemID());
                if (factor == null) {
                    continue; // no factor for this line → leave null
                }
                BigDecimal cp = nz(line.price())
                        .multiply(BigDecimal.valueOf(factor))
                        .multiply(ONE_HUNDREDTH)
                        .divide(sum, 2, RoundingMode.HALF_UP);
                if (isOutOfRange(cp)) {
                    // Args mirror the legacy message: {0}=price, {2}=species,
                    // {3}=grade, {4}=sort code ({1} is intentionally unused).
                    warnings.add(new ValidationMessage(
                            MSG_OUT_OF_RANGE,
                            new Object[] { cp, null, line.species(), line.grade(), sortCode },
                            MessageType.WARNING));
                    // leave null
                } else {
                    converted.put(line.lineItemID(), cp);
                }
            }
        } else {
            // Zero is a valid factor; the spread collapses to zero for the group.
            for (LineItem line : group) {
                converted.put(line.lineItemID(), BigDecimal.ZERO);
            }
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static BigDecimal sum(List<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            if (v != null) {
                total = total.add(v);
            }
        }
        return total;
    }

    private static boolean isOutOfRange(BigDecimal value) {
        return value.precision() - value.scale() > 5;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    /** Scale-insensitive price key so 25 and 25.00 group together. */
    private static String priceKey(BigDecimal price) {
        if (price == null) {
            return "null";
        }
        return price.compareTo(BigDecimal.ZERO) == 0 ? "0" : price.stripTrailingZeros().toPlainString();
    }

    private static LineItem withConvertedPrice(LineItem line, BigDecimal convertedPrice) {
        return new LineItem(
                line.lineItemID(),
                line.invoiceID(),
                line.secondSort(),
                line.clientSecondarySort(),
                line.species(),
                line.speciesDescription(),
                line.grade(),
                line.numOfPieces(),
                line.price(),
                line.volume(),
                convertedPrice,
                line.amount());
    }
}
