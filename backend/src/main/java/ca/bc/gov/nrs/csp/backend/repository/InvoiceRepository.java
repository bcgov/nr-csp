package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceDetails;
import ca.bc.gov.nrs.csp.backend.util.constants.ConstantsCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class InvoiceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public InvoiceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------
    // Duplicate-check / read-only lookups used by validation
    // ---------------------------------------------------------------

    public record InvoiceMatch(
            Long coastalLogSaleId,
            String invoiceStatusCode,
            String invoiceTypeCode,
            String submitterClientNumber,
            String submitterClientLocnCode,
            String buyerClientNumber,
            String buyerClientLocnCode,
            String sellerClientNumber,
            String sellerClientLocnCode,
            String buyerParticipantName,
            String buyerParticipantCity,
            String buyerParticipantProvince,
            String sellerParticipantName,
            String sellerParticipantCity,
            String sellerParticipantProvince
    ) {}

    public List<InvoiceMatch> findByClientInvoiceNo(String clientInvoiceNo) {
        String sql = """
                SELECT
                       cls.coastal_log_sale_id,
                       cls.log_sale_entry_status_code AS invoice_status_code,
                       cls.csp_invoice_type_code AS invoice_type_code,
                       sub_cl.client_number AS submitter_client_number,
                       sub_cl.client_locn_code AS submitter_client_locn_code,
                       buyer_cl.client_number AS buyer_client_number,
                       buyer_cl.client_locn_code AS buyer_client_locn_code,
                       seller_cl.client_number AS seller_client_number,
                       seller_cl.client_locn_code AS seller_client_locn_code,
                       buyer_part.name AS buyer_participant_name,
                       buyer_part.city AS buyer_participant_city,
                       buyer_part.province AS buyer_participant_province,
                       seller_part.name AS seller_participant_name,
                       seller_part.city AS seller_participant_city,
                       seller_part.province AS seller_participant_province
                FROM THE.coastal_log_sale cls
                JOIN THE.csp_submission sub ON cls.csp_submission_id = sub.csp_submission_id
                JOIN THE.client_location sub_cl
                    ON sub.client_number = sub_cl.client_number
                   AND sub.client_locn_code = sub_cl.client_locn_code
                LEFT JOIN THE.client_location buyer_cl
                    ON cls.buyer_client_number = buyer_cl.client_number
                   AND cls.buyer_client_locn_code = buyer_cl.client_locn_code
                LEFT JOIN THE.client_location seller_cl
                    ON cls.seller_client_number = seller_cl.client_number
                   AND cls.seller_client_locn_code = seller_cl.client_locn_code
                LEFT JOIN THE.log_sale_participant buyer_part ON cls.buyer_log_sale_participant_id = buyer_part.log_sale_participant_id
                LEFT JOIN THE.log_sale_participant seller_part ON cls.seller_log_sale_participant_id = seller_part.log_sale_participant_id
                WHERE cls.client_invoice_no = :clientInvoiceNo
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("clientInvoiceNo", clientInvoiceNo);
        return jdbc.query(sql, params, (rs, rowNum) -> new InvoiceMatch(
                rs.getLong("coastal_log_sale_id"),
                rs.getString("invoice_status_code"),
                rs.getString("invoice_type_code"),
                rs.getString("submitter_client_number"),
                rs.getString("submitter_client_locn_code"),
                rs.getString("buyer_client_number"),
                rs.getString("buyer_client_locn_code"),
                rs.getString("seller_client_number"),
                rs.getString("seller_client_locn_code"),
                rs.getString("buyer_participant_name"),
                rs.getString("buyer_participant_city"),
                rs.getString("buyer_participant_province"),
                rs.getString("seller_participant_name"),
                rs.getString("seller_participant_city"),
                rs.getString("seller_participant_province")
        ));
    }

    public record RelatedInvoice(Long coastalLogSaleId, String invoiceStatusCode) {}

    public List<RelatedInvoice> findByInvoiceNoAndClient(String clientInvoiceNo, String clientNumber, String clientLocnCode) {
        String sql = """
                SELECT
                       cls.coastal_log_sale_id,
                       cls.log_sale_entry_status_code AS invoice_status_code
                FROM THE.coastal_log_sale cls
                JOIN THE.csp_submission sub ON cls.csp_submission_id = sub.csp_submission_id
                JOIN THE.client_location cl
                    ON sub.client_number = cl.client_number
                   AND sub.client_locn_code = cl.client_locn_code
                WHERE cls.client_invoice_no = :clientInvoiceNo
                  AND cl.client_number = :clientNumber
                  AND cl.client_locn_code = :clientLocnCode
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("clientInvoiceNo", clientInvoiceNo)
                .addValue("clientNumber", clientNumber)
                .addValue("clientLocnCode", clientLocnCode);
        return jdbc.query(sql, params, (rs, rowNum) -> new RelatedInvoice(
                rs.getLong("coastal_log_sale_id"),
                rs.getString("invoice_status_code")
        ));
    }

    /**
     * Mirrors the legacy InvSubmissionDAO#isMonthCompleted logic:
     * counts invoices whose parent submission has MONTH_COMPLETE_IND='Y'
     * for the given client + location, where the invoice's client_invoice_date
     * falls in the same YYYY-MM as the invoice we're validating. The current
     * invoice (if any) is excluded from the count so the warning doesn't
     * fire against itself.
     */
    public boolean isMonthCompleted(LocalDate invoiceDate, String clientNumber, String clientLocnCode, Long excludeInvoiceId) {
        if (invoiceDate == null || clientNumber == null || clientLocnCode == null) return false;
        String sql = """
                SELECT COUNT(*)
                FROM THE.coastal_log_sale cls
                JOIN THE.csp_submission sub ON cls.csp_submission_id = sub.csp_submission_id
                WHERE sub.month_complete_ind = 'Y'
                  AND sub.client_number = :clientNumber
                  AND sub.client_locn_code = :clientLocnCode
                  AND TO_CHAR(cls.client_invoice_date, 'YYYY-MM') = TO_CHAR(:invoiceDate, 'YYYY-MM')
                  AND (:excludeInvoiceId IS NULL OR cls.coastal_log_sale_id <> :excludeInvoiceId)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("clientNumber", clientNumber)
                .addValue("clientLocnCode", clientLocnCode)
                .addValue("invoiceDate", Date.valueOf(invoiceDate))
                .addValue("excludeInvoiceId", excludeInvoiceId, Types.BIGINT);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public int countBoomNumberDuplicates(Long excludeInvoiceId, String logSourceCode, String boomNumber) {
        if (boomNumber == null || boomNumber.isBlank()) return 0;
        String sql = """
                SELECT COUNT(*) FROM THE.coastal_log_sale_log_source clls
                WHERE clls.log_source_code = :logSourceCode
                  AND clls.source_document_reference = :boomNumber
                  AND (:excludeInvoiceId IS NULL OR clls.coastal_log_sale_id <> :excludeInvoiceId)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("logSourceCode", logSourceCode)
                .addValue("boomNumber", boomNumber)
                .addValue("excludeInvoiceId", excludeInvoiceId, Types.BIGINT);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count == null ? 0 : count;
    }

    public String findReviewCommentsById(Long coastalLogSaleId) {
        if (coastalLogSaleId == null) return null;
        String sql = "SELECT reviewer_notes FROM THE.coastal_log_sale WHERE coastal_log_sale_id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", coastalLogSaleId);
        List<String> results = jdbc.queryForList(sql, params, String.class);
        return results.isEmpty() ? null : results.get(0);
    }

    // ---------------------------------------------------------------
    // Full load (header + log-source lists) by id
    // ---------------------------------------------------------------

    /**
     * Wraps a loaded invoice with its parent submission id, which lives on csp_submission
     * but is needed by the response shape.
     */
    public record LoadedInvoice(
            InvoiceDetails details,
            Long submissionId,
            Long buyerParticipantId,
            Long sellerParticipantId,
            // The business submission number (csp_submission.submission_id), shown
            // to the user as "Submission ID". Distinct from `submissionId`, which is
            // the surrogate PK (csp_submission_id) used as the join/update key.
            // Null for manually-entered submissions (insertSubmission doesn't set it).
            Long submissionNumber
    ) {}

    public Optional<LoadedInvoice> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = """
                SELECT
                       cls.coastal_log_sale_id AS inv_id,
                       cls.client_invoice_no AS inv_number,
                       cls.client_invoice_date AS invoice_date,
                       cls.log_sale_entry_status_code AS inv_status,
                       cls.csp_invoice_type_code AS inv_type,
                       cls.log_sale_type_code AS maturity,
                       cls.log_sale_fob_location AS fob_code,
                       cls.log_sale_sort_code AS primary_sort_code,
                       cls.client_total_invoice_amt AS total_amt,
                       cls.client_total_invoice_pieces AS total_pieces,
                       cls.client_total_invoice_volume AS total_vol,
                       cls.reviewer_notes AS review_comments,
                       cls.submitter_notes AS submit_comments,
                       cls.entry_userid AS entry_userid,
                       cls.csp_submission_id AS submission_id,
                       sub.submission_id AS submission_number,
                       cls.buyer_log_sale_participant_id AS buyer_participant_id,
                       cls.seller_log_sale_participant_id AS seller_participant_id,
                       sub.client_number AS submitter_client_num,
                       sub.client_locn_code AS submitter_location,
                       cls.seller_client_number AS seller_client_number,
                       cls.seller_client_locn_code AS seller_client_locn_code,
                       cls.buyer_client_number AS buyer_client_number,
                       cls.buyer_client_locn_code AS buyer_client_locn_code,
                       buyer_part.name AS buyer_participant_name,
                       buyer_part.city AS buyer_participant_city,
                       buyer_part.province AS buyer_participant_province,
                       seller_part.name AS seller_participant_name,
                       seller_part.city AS seller_participant_city,
                       seller_part.province AS seller_participant_province
                FROM THE.coastal_log_sale cls
                JOIN THE.csp_submission sub ON cls.csp_submission_id = sub.csp_submission_id
                LEFT JOIN THE.log_sale_participant buyer_part ON cls.buyer_log_sale_participant_id = buyer_part.log_sale_participant_id
                LEFT JOIN THE.log_sale_participant seller_part ON cls.seller_log_sale_participant_id = seller_part.log_sale_participant_id
                WHERE cls.coastal_log_sale_id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<LoadedInvoice> rows = jdbc.query(sql, params, (rs, rowNum) -> mapLoadedInvoice(rs, id));
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }

    private LoadedInvoice mapLoadedInvoice(ResultSet rs, Long id) throws SQLException {
        Long submissionId = rs.getObject("submission_id", Long.class);
        Long submissionNumber = rs.getObject("submission_number", Long.class);
        Long buyerParticipantId = rs.getObject("buyer_participant_id", Long.class);
        Long sellerParticipantId = rs.getObject("seller_participant_id", Long.class);
        // submittedBy is derived from whether the submitter == the seller side or the buyer side
        // of the coastal_log_sale row.
        String submitterClientNum = rs.getString("submitter_client_num");
        String submitterLocation = rs.getString("submitter_location");
        String sellerClientNum = rs.getString("seller_client_number");
        String sellerClientLoc = rs.getString("seller_client_locn_code");
        boolean submittedBySeller = java.util.Objects.equals(submitterClientNum, sellerClientNum)
                && java.util.Objects.equals(submitterLocation, sellerClientLoc);
        String submittedBy = submittedBySeller ? "Seller" : "Buyer";

        // The "other party" is whichever side isn't the submitter. When the submitter is the seller,
        // read buyer columns; when the submitter is the buyer, read seller columns. Without this
        // branch, GET responses for buyer-submitted invoices would echo back the submitter's own
        // buyer info as the "other party".
        String otherClientNum = submittedBySeller
                ? rs.getString("buyer_client_number")
                : sellerClientNum;
        String otherClientLoc = submittedBySeller
                ? rs.getString("buyer_client_locn_code")
                : sellerClientLoc;
        String otherClientName = submittedBySeller
                ? rs.getString("buyer_participant_name")
                : rs.getString("seller_participant_name");
        String otherClientCity = submittedBySeller
                ? rs.getString("buyer_participant_city")
                : rs.getString("seller_participant_city");
        String otherClientProvince = submittedBySeller
                ? rs.getString("buyer_participant_province")
                : rs.getString("seller_participant_province");

        InvoiceDetails details = new InvoiceDetails(
                rs.getObject("inv_id", Long.class),
                rs.getString("inv_number"),
                rs.getDate("invoice_date") == null ? null : rs.getDate("invoice_date").toLocalDate(),
                rs.getString("inv_status"),
                rs.getString("inv_type"),
                rs.getString("maturity"),
                rs.getString("fob_code"),
                rs.getString("primary_sort_code"),
                rs.getBigDecimal("total_amt"),
                rs.getObject("total_pieces", Integer.class),
                rs.getBigDecimal("total_vol"),
                submitterClientNum,
                submitterLocation,
                submittedBy,
                sellerClientNum,
                sellerClientLoc,
                otherClientNum,
                otherClientLoc,
                otherClientName,
                otherClientCity,
                otherClientProvince,
                findLogSources(id, "BOOM"),
                findLogSources(id, "MARK"),
                findLogSources(id, "WEIGH"),
                findRelatedInvoiceNumbers(id, ConstantsCode.INVRELATETYPE_REPLACE),
                findRelatedInvoiceNumbers(id, ConstantsCode.INVRELATETYPE_ADJUST),
                rs.getString("review_comments"),
                rs.getString("submit_comments"),
                rs.getString("entry_userid")
        );
        return new LoadedInvoice(details, submissionId, buyerParticipantId, sellerParticipantId, submissionNumber);
    }

    /**
     * Loads the client invoice numbers of every invoice related to the given parent invoice
     * via {@code refTypeCode} (REP for "replaces" / ADJ for "adjusts") and returns them as
     * a comma-separated string — matching the shape the validator and request bodies use.
     * Returns {@code null} when there are no related invoices.
     */
    public String findRelatedInvoiceNumbers(Long parentId, String refTypeCode) {
        if (parentId == null || refTypeCode == null) return null;
        String sql = """
                SELECT cls.client_invoice_no
                FROM THE.coastal_log_sale_rltd_invc r
                JOIN THE.coastal_log_sale cls ON r.related_coastal_log_sale_id = cls.coastal_log_sale_id
                WHERE r.coastal_log_sale_id = :parentId
                  AND r.csp_invoice_ref_type_code = :refTypeCode
                ORDER BY r.coastal_log_sale_rltd_invc_id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("parentId", parentId)
                .addValue("refTypeCode", refTypeCode);
        List<String> numbers = jdbc.queryForList(sql, params, String.class);
        return numbers.isEmpty() ? null : String.join(",", numbers);
    }

    public List<String> findLogSources(Long coastalLogSaleId, String logSourceCode) {
        String sql = """
                SELECT source_document_reference
                FROM THE.coastal_log_sale_log_source
                WHERE coastal_log_sale_id = :id AND log_source_code = :code
                ORDER BY coastal_log_sale_log_source_id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", coastalLogSaleId)
                .addValue("code", logSourceCode);
        return jdbc.queryForList(sql, params, String.class);
    }

    // ---------------------------------------------------------------
    // CUD on coastal_log_sale + coastal_log_sale_log_source
    // ---------------------------------------------------------------

    public Long insertInvoice(InvoiceDetails details, Long submissionId, String status,
                              Long buyerParticipantId, Long sellerParticipantId, String userId) {
        String sql = """
                INSERT INTO THE.coastal_log_sale (
                    coastal_log_sale_id,
                    csp_submission_id,
                    log_sale_entry_status_code,
                    csp_invoice_type_code,
                    log_sale_type_code,
                    log_sale_fob_location,
                    log_sale_sort_code,
                    client_primary_sort_code,
                    seller_client_number,
                    seller_client_locn_code,
                    buyer_client_number,
                    buyer_client_locn_code,
                    buyer_log_sale_participant_id,
                    seller_log_sale_participant_id,
                    client_invoice_no,
                    client_invoice_date,
                    cancelled_ind,
                    late_invoice_ind,
                    amv_calculate_date,
                    client_total_boomstick_pieces,
                    client_total_log_pieces,
                    client_total_invoice_pieces,
                    client_total_invoice_volume,
                    client_total_invoice_amt,
                    reviewer_notes,
                    submitter_notes,
                    revision_count,
                    entry_userid, entry_timestamp,
                    update_userid, update_timestamp
                ) VALUES (
                    THE.COASTAL_LOG_SALE_SEQ.NEXTVAL,
                    :submissionId,
                    :status,
                    :invType,
                    :maturity,
                    :fobCode,
                    :primarySortCode,
                    :clientPrimarySortCode,
                    :sellerClientNumber,
                    :sellerClientLocnCode,
                    :buyerClientNumber,
                    :buyerClientLocnCode,
                    :buyerParticipantId,
                    :sellerParticipantId,
                    :invNumber,
                    :invoiceDate,
                    'N',
                    'N',
                    :amvCalcDate,
                    0,
                    :totalPieces,
                    :totalPieces,
                    :totalVol,
                    :totalAmt,
                    :reviewerNotes,
                    :submitterNotes,
                    0,
                    :userId, SYSDATE,
                    :userId, SYSDATE
                )
                """;
        // Derive buyer/seller columns from submittedBy.
        boolean submittedBySeller = "Seller".equals(details.submittedBy());
        String sellerClientNumber = submittedBySeller ? details.submitterClientNum() : details.otherClientNum();
        String sellerClientLocnCode = submittedBySeller ? details.submitterLocation() : details.otherClientLocation();
        String buyerClientNumber = submittedBySeller ? details.otherClientNum() : details.submitterClientNum();
        String buyerClientLocnCode = submittedBySeller ? details.otherClientLocation() : details.submitterLocation();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("submissionId", submissionId)
                .addValue("status", status)
                .addValue("invType", details.invType())
                .addValue("maturity", details.maturity())
                .addValue("fobCode", details.fobCode())
                .addValue("primarySortCode", details.primarySortCode())
                .addValue("clientPrimarySortCode", details.primarySortCode())
                .addValue("sellerClientNumber", sellerClientNumber)
                .addValue("sellerClientLocnCode", sellerClientLocnCode)
                .addValue("buyerClientNumber", buyerClientNumber)
                .addValue("buyerClientLocnCode", buyerClientLocnCode)
                .addValue("buyerParticipantId", buyerParticipantId, Types.BIGINT)
                .addValue("sellerParticipantId", sellerParticipantId, Types.BIGINT)
                .addValue("invNumber", details.invNumber())
                .addValue("invoiceDate", details.invoiceDate() == null ? null : Date.valueOf(details.invoiceDate()))
                .addValue("amvCalcDate", details.invoiceDate() == null ? null : Date.valueOf(details.invoiceDate()))
                .addValue("totalPieces", details.totalPieces() == null ? 0 : details.totalPieces())
                .addValue("totalVol", details.totalVol())
                .addValue("totalAmt", details.totalAmt())
                .addValue("reviewerNotes", details.reviewComments())
                .addValue("submitterNotes", details.submitComments())
                .addValue("userId", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"COASTAL_LOG_SALE_ID"});
        return ((Number) keyHolder.getKeys().get("COASTAL_LOG_SALE_ID")).longValue();
    }

    public void updateInvoice(Long id, InvoiceDetails details, String status,
                              Long buyerParticipantId, Long sellerParticipantId, String userId) {
        String sql = """
                UPDATE THE.coastal_log_sale SET
                    log_sale_entry_status_code = :status,
                    csp_invoice_type_code = :invType,
                    log_sale_type_code = :maturity,
                    log_sale_fob_location = :fobCode,
                    log_sale_sort_code = :primarySortCode,
                    client_primary_sort_code = :clientPrimarySortCode,
                    seller_client_number = :sellerClientNumber,
                    seller_client_locn_code = :sellerClientLocnCode,
                    buyer_client_number = :buyerClientNumber,
                    buyer_client_locn_code = :buyerClientLocnCode,
                    buyer_log_sale_participant_id = :buyerParticipantId,
                    seller_log_sale_participant_id = :sellerParticipantId,
                    client_invoice_no = :invNumber,
                    client_invoice_date = :invoiceDate,
                    client_total_invoice_pieces = :totalPieces,
                    client_total_log_pieces = :totalPieces,
                    client_total_invoice_volume = :totalVol,
                    client_total_invoice_amt = :totalAmt,
                    reviewer_notes = :reviewerNotes,
                    submitter_notes = :submitterNotes,
                    revision_count = revision_count + 1,
                    update_userid = :userId,
                    update_timestamp = SYSDATE
                WHERE coastal_log_sale_id = :id
                """;
        boolean submittedBySeller = "Seller".equals(details.submittedBy());
        String sellerClientNumber = submittedBySeller ? details.submitterClientNum() : details.otherClientNum();
        String sellerClientLocnCode = submittedBySeller ? details.submitterLocation() : details.otherClientLocation();
        String buyerClientNumber = submittedBySeller ? details.otherClientNum() : details.submitterClientNum();
        String buyerClientLocnCode = submittedBySeller ? details.otherClientLocation() : details.submitterLocation();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status)
                .addValue("invType", details.invType())
                .addValue("maturity", details.maturity())
                .addValue("fobCode", details.fobCode())
                .addValue("primarySortCode", details.primarySortCode())
                .addValue("clientPrimarySortCode", details.primarySortCode())
                .addValue("sellerClientNumber", sellerClientNumber)
                .addValue("sellerClientLocnCode", sellerClientLocnCode)
                .addValue("buyerClientNumber", buyerClientNumber)
                .addValue("buyerClientLocnCode", buyerClientLocnCode)
                .addValue("buyerParticipantId", buyerParticipantId, Types.BIGINT)
                .addValue("sellerParticipantId", sellerParticipantId, Types.BIGINT)
                .addValue("invNumber", details.invNumber())
                .addValue("invoiceDate", details.invoiceDate() == null ? null : Date.valueOf(details.invoiceDate()))
                .addValue("totalPieces", details.totalPieces() == null ? 0 : details.totalPieces())
                .addValue("totalVol", details.totalVol())
                .addValue("totalAmt", details.totalAmt())
                .addValue("reviewerNotes", details.reviewComments())
                .addValue("submitterNotes", details.submitComments())
                .addValue("userId", userId);
        jdbc.update(sql, params);
    }

    public void updateStatus(Long id, String status, String userId) {
        String sql = """
                UPDATE THE.coastal_log_sale SET
                    log_sale_entry_status_code = :status,
                    revision_count = revision_count + 1,
                    update_userid = :userId,
                    update_timestamp = SYSDATE
                WHERE coastal_log_sale_id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status)
                .addValue("userId", userId);
        jdbc.update(sql, params);
    }

    public void updateReviewerNotes(Long id, String reviewerNotes, String userId) {
        String sql = """
                UPDATE THE.coastal_log_sale SET
                    reviewer_notes = :notes,
                    revision_count = revision_count + 1,
                    update_userid = :userId,
                    update_timestamp = SYSDATE
                WHERE coastal_log_sale_id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("notes", reviewerNotes)
                .addValue("userId", userId);
        jdbc.update(sql, params);
    }

    public void deleteInvoice(Long id) {
        jdbc.update("DELETE FROM THE.coastal_log_sale WHERE coastal_log_sale_id = :id",
                new MapSqlParameterSource("id", id));
    }

    public void deleteAllLogSources(Long invoiceId) {
        jdbc.update("DELETE FROM THE.coastal_log_sale_log_source WHERE coastal_log_sale_id = :id",
                new MapSqlParameterSource("id", invoiceId));
    }

    public void deleteAllRelatedInvoiceRefs(Long invoiceId) {
        jdbc.update("DELETE FROM THE.coastal_log_sale_rltd_invc WHERE coastal_log_sale_id = :id",
                new MapSqlParameterSource("id", invoiceId));
    }

    /**
     * Deletes any rows in {@code coastal_log_sale_rltd_invc} that reference {@code invoiceId}
     * as the *related* invoice (i.e. some other invoice claimed to replace or adjust this one).
     * Without this, deleting an invoice that's been referenced by another would fail the
     * RELATED_COASTAL_LOG_SALE_ID FK constraint.
     */
    public void deleteAllIncomingRelatedInvoiceRefs(Long invoiceId) {
        jdbc.update("DELETE FROM THE.coastal_log_sale_rltd_invc WHERE related_coastal_log_sale_id = :id",
                new MapSqlParameterSource("id", invoiceId));
    }

    /** Returns the number of {@code coastal_log_sale} rows attached to the given submission. */
    public int countByCspSubmissionId(Long submissionId) {
        if (submissionId == null) return 0;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM THE.coastal_log_sale WHERE csp_submission_id = :id",
                new MapSqlParameterSource("id", submissionId),
                Integer.class);
        return count == null ? 0 : count;
    }

    public int countByCspSubmissionIdAndStatus(Long submissionId, String statusCode) {
        if (submissionId == null) return 0;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM THE.coastal_log_sale" +
                " WHERE csp_submission_id = :id AND log_sale_entry_status_code = :status",
                new MapSqlParameterSource().addValue("id", submissionId).addValue("status", statusCode),
                Integer.class);
        return count == null ? 0 : count;
    }

    /**
     * Replaces this invoice's related-invoice references of the given type (REP or ADJ).
     * Deletes existing rows for the parent+type, then inserts one row per invoice number
     * in {@code csvInvoiceNumbers}, resolving each number to its coastal_log_sale_id via the
     * submitter's client. The validator runs the same lookup, so any number reaching this
     * method should already exist for the submitter.
     */
    public void replaceRelatedInvoices(Long parentId, String refTypeCode, String csvInvoiceNumbers,
                                        String submitterClientNum, String submitterLocnCode, String userId) {
        jdbc.update("DELETE FROM THE.coastal_log_sale_rltd_invc"
                        + " WHERE coastal_log_sale_id = :id AND csp_invoice_ref_type_code = :type",
                new MapSqlParameterSource().addValue("id", parentId).addValue("type", refTypeCode));

        if (csvInvoiceNumbers == null || csvInvoiceNumbers.isBlank()) return;

        String resolveSql = """
                SELECT cls.coastal_log_sale_id
                FROM THE.coastal_log_sale cls
                JOIN THE.csp_submission sub ON cls.csp_submission_id = sub.csp_submission_id
                WHERE cls.client_invoice_no = :invNo
                  AND sub.client_number = :submitterClientNum
                  AND sub.client_locn_code = :submitterLocnCode
                """;

        String insertSql = """
                INSERT INTO THE.coastal_log_sale_rltd_invc (
                    coastal_log_sale_rltd_invc_id, coastal_log_sale_id,
                    related_coastal_log_sale_id, csp_invoice_ref_type_code,
                    revision_count,
                    entry_userid, entry_timestamp, update_userid, update_timestamp
                ) VALUES (
                    THE.COASTAL_LOG_SALE_RTLD_INVC_SEQ.NEXTVAL, :parentId,
                    :relatedId, :refTypeCode,
                    0, :userId, SYSDATE, :userId, SYSDATE
                )
                """;

        for (String invNo : csvInvoiceNumbers.split(",")) {
            String trimmed = invNo.trim();
            if (trimmed.isEmpty()) continue;
            List<Long> ids = jdbc.queryForList(resolveSql,
                    new MapSqlParameterSource()
                            .addValue("invNo", trimmed)
                            .addValue("submitterClientNum", submitterClientNum)
                            .addValue("submitterLocnCode", submitterLocnCode),
                    Long.class);
            if (ids.isEmpty()) continue; // validator should have rejected; defensive skip
            jdbc.update(insertSql,
                    new MapSqlParameterSource()
                            .addValue("parentId", parentId)
                            .addValue("relatedId", ids.get(0))
                            .addValue("refTypeCode", refTypeCode)
                            .addValue("userId", userId));
        }
    }

    public void replaceLogSources(Long invoiceId, String logSourceCode, List<String> values, String userId) {
        jdbc.update("DELETE FROM THE.coastal_log_sale_log_source"
                        + " WHERE coastal_log_sale_id = :id AND log_source_code = :code",
                new MapSqlParameterSource().addValue("id", invoiceId).addValue("code", logSourceCode));
        if (values == null || values.isEmpty()) return;
        String insertSql = """
                INSERT INTO THE.coastal_log_sale_log_source (
                    coastal_log_sale_log_source_id, coastal_log_sale_id, log_source_code,
                    source_document_reference, revision_count,
                    entry_userid, entry_timestamp, update_userid, update_timestamp
                ) VALUES (
                    THE.COASTAL_LOG_SALE_SOURCE_SEQ.NEXTVAL, :id, :code,
                    :ref, 0, :userId, SYSDATE, :userId, SYSDATE
                )
                """;
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", invoiceId)
                    .addValue("code", logSourceCode)
                    .addValue("ref", value.trim())
                    .addValue("userId", userId);
            jdbc.update(insertSql, params);
        }
    }
}
