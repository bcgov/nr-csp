package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItem;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class LineItemRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public LineItemRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<LineItem> findByInvoiceId(Long invoiceId) {
        if (invoiceId == null) return List.of();
        // Joins csp_species_grade_xref to expose species_code and grade_code back to the caller,
        // and left-joins log_sale_species_code so the species description is included for display.
        String sql = """
                SELECT
                    d.coastal_log_sale_detail_id AS line_item_id,
                    d.coastal_log_sale_id AS invoice_id,
                    d.log_sale_sort_code AS second_sort,
                    d.client_secondary_sort_code AS client_secondary_sort,
                    sgx.log_sale_species_code AS species,
                    sc.description AS species_description,
                    sgx.log_sale_grade_code AS grade,
                    d.pieces,
                    d.price,
                    d.volume,
                    d.converted_price
                FROM THE.coastal_log_sale_detail d
                JOIN THE.csp_species_grade_xref sgx ON d.csp_species_grade_xref_id = sgx.csp_species_grade_xref_id
                LEFT JOIN THE.log_sale_species_code sc ON sgx.log_sale_species_code = sc.log_sale_species_code
                WHERE d.coastal_log_sale_id = :id
                ORDER BY d.coastal_log_sale_detail_id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("id", invoiceId);
        return jdbc.query(sql, params, (rs, rowNum) -> {
            BigDecimal price = rs.getBigDecimal("price");
            BigDecimal volume = rs.getBigDecimal("volume");
            BigDecimal amount = (price != null && volume != null) ? price.multiply(volume) : null;
            return new LineItem(
                    rs.getObject("line_item_id", Long.class),
                    rs.getObject("invoice_id", Long.class),
                    rs.getString("second_sort"),
                    rs.getString("client_secondary_sort"),
                    rs.getString("species"),
                    rs.getString("species_description"),
                    rs.getString("grade"),
                    rs.getObject("pieces", Integer.class),
                    price,
                    volume,
                    rs.getBigDecimal("converted_price"),
                    amount
            );
        });
    }

    public Long insertLineItem(Long invoiceId, LineItem line, String userId) {
        String sql = """
                INSERT INTO THE.coastal_log_sale_detail (
                    coastal_log_sale_detail_id,
                    coastal_log_sale_id,
                    csp_species_grade_xref_id,
                    log_sale_sort_code,
                    client_secondary_sort_code,
                    pieces, volume, price, converted_price,
                    revision_count,
                    entry_userid, entry_timestamp, update_userid, update_timestamp
                ) VALUES (
                    THE.COASTAL_LOG_SALE_DETAIL_SEQ.NEXTVAL,
                    :invoiceId,
                    (SELECT csp_species_grade_xref_id FROM THE.csp_species_grade_xref
                     WHERE log_sale_species_code = :species AND log_sale_grade_code = :grade),
                    :sortCode,
                    :clientSortCode,
                    :pieces, :volume, :price, :convertedPrice,
                    0,
                    :userId, SYSDATE, :userId, SYSDATE
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("invoiceId", invoiceId)
                .addValue("species", line.species())
                .addValue("grade", line.grade())
                .addValue("sortCode", line.secondSort())
                .addValue("clientSortCode", clientSortCodeOf(line))
                .addValue("pieces", line.numOfPieces())
                .addValue("volume", line.volume())
                .addValue("price", line.price())
                .addValue("convertedPrice", line.convertedPrice())
                .addValue("userId", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"COASTAL_LOG_SALE_DETAIL_ID"});
        return ((Number) keyHolder.getKeys().get("COASTAL_LOG_SALE_DETAIL_ID")).longValue();
    }

    public void updateLineItem(Long lineItemId, LineItem line, String userId) {
        String sql = """
                UPDATE THE.coastal_log_sale_detail SET
                    csp_species_grade_xref_id =
                        (SELECT csp_species_grade_xref_id FROM THE.csp_species_grade_xref
                         WHERE log_sale_species_code = :species AND log_sale_grade_code = :grade),
                    log_sale_sort_code = :sortCode,
                    client_secondary_sort_code = :clientSortCode,
                    pieces = :pieces,
                    volume = :volume,
                    price = :price,
                    converted_price = :convertedPrice,
                    revision_count = revision_count + 1,
                    update_userid = :userId,
                    update_timestamp = SYSDATE
                WHERE coastal_log_sale_detail_id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", lineItemId)
                .addValue("species", line.species())
                .addValue("grade", line.grade())
                .addValue("sortCode", line.secondSort())
                .addValue("clientSortCode", clientSortCodeOf(line))
                .addValue("pieces", line.numOfPieces())
                .addValue("volume", line.volume())
                .addValue("price", line.price())
                .addValue("convertedPrice", line.convertedPrice())
                .addValue("userId", userId);
        jdbc.update(sql, params);
    }

    /**
     * Persist only the system-computed {@code converted_price} (flat-price
     * spread). Kept separate from {@link #updateLineItem} so the conversion run
     * during submit doesn't rewrite user-entered columns or bump the revision
     * count used for optimistic locking.
     */
    public void updateConvertedPrice(Long lineItemId, BigDecimal convertedPrice, String userId) {
        String sql = """
                UPDATE THE.coastal_log_sale_detail SET
                    converted_price = :convertedPrice,
                    update_userid = :userId,
                    update_timestamp = SYSDATE
                WHERE coastal_log_sale_detail_id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("convertedPrice", convertedPrice)
                .addValue("userId", userId)
                .addValue("id", lineItemId);
        jdbc.update(sql, params);
    }

    // Use the client-supplied value when present, otherwise fall back to the
    // secondary sort code (the historical behaviour) so a blank entry can't
    // produce a constraint violation.
    private static String clientSortCodeOf(LineItem line) {
        String clientSort = line.clientSecondarySort();
        return (clientSort != null && !clientSort.isBlank()) ? clientSort : line.secondSort();
    }

    public void deleteLineItem(Long lineItemId) {
        jdbc.update("DELETE FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_detail_id = :id",
                new MapSqlParameterSource("id", lineItemId));
    }

    public void deleteAllByInvoiceId(Long invoiceId) {
        jdbc.update("DELETE FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = :id",
                new MapSqlParameterSource("id", invoiceId));
    }

    public List<Long> findIdsByInvoiceId(Long invoiceId) {
        return jdbc.queryForList(
                "SELECT coastal_log_sale_detail_id FROM THE.coastal_log_sale_detail WHERE coastal_log_sale_id = :id",
                new MapSqlParameterSource("id", invoiceId),
                Long.class);
    }
}
