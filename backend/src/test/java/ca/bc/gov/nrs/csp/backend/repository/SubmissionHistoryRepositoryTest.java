package ca.bc.gov.nrs.csp.backend.repository;

import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionDetailResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionHistoryRowResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionInvoiceCommentResponse;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionHistoryRepositoryTest {

    @Mock NamedParameterJdbcTemplate jdbc;

    SubmissionHistoryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SubmissionHistoryRepository(jdbc);
    }

    // ---------------------------------------------------------------
    // buildOrderBy
    // ---------------------------------------------------------------

    @Test
    void buildOrderBy_nullSort_usesDefaultWithTiebreaker() {
        assertThat(SubmissionHistoryRepository.buildOrderBy(null))
                .isEqualTo("entry_timestamp DESC, csp_submission_id DESC");
    }

    @Test
    void buildOrderBy_unsortedPageable_usesDefaultWithTiebreaker() {
        assertThat(SubmissionHistoryRepository.buildOrderBy(Sort.unsorted()))
                .isEqualTo("entry_timestamp DESC, csp_submission_id DESC");
    }

    @Test
    void buildOrderBy_ascendingKnownField_mapsToColumn() {
        assertThat(SubmissionHistoryRepository.buildOrderBy(Sort.by("clientName").ascending()))
                .isEqualTo("client_name ASC, csp_submission_id DESC");
    }

    @Test
    void buildOrderBy_multipleFields_joinsWithComma() {
        Sort sort = Sort.by(Sort.Order.desc("submittedBy"), Sort.Order.asc("submissionStatus"));
        assertThat(SubmissionHistoryRepository.buildOrderBy(sort))
                .isEqualTo("submitted_by DESC, submission_status ASC, csp_submission_id DESC");
    }

    @Test
    void buildOrderBy_unknownField_throwsBadRequest() {
        Sort unknownSort = Sort.by("bogus");
        assertThatThrownBy(() -> SubmissionHistoryRepository.buildOrderBy(unknownSort))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported sort field: bogus");
    }

    // ---------------------------------------------------------------
    // formatClient
    // ---------------------------------------------------------------

    @Test
    void formatClient_nullNumber_returnsNull() {
        assertThat(SubmissionHistoryRepository.formatClient(null, "00")).isNull();
    }

    @Test
    void formatClient_blankNumber_returnsNull() {
        assertThat(SubmissionHistoryRepository.formatClient("   ", "00")).isNull();
    }

    @Test
    void formatClient_nullLocn_returnsNumberOnly() {
        assertThat(SubmissionHistoryRepository.formatClient("126920", null)).isEqualTo("126920");
    }

    @Test
    void formatClient_blankLocn_returnsNumberOnly() {
        assertThat(SubmissionHistoryRepository.formatClient("126920", "  ")).isEqualTo("126920");
    }

    @Test
    void formatClient_numberAndLocn_joinsWithSlash() {
        assertThat(SubmissionHistoryRepository.formatClient("126920", "00")).isEqualTo("126920/00");
    }

    // ---------------------------------------------------------------
    // search
    // ---------------------------------------------------------------

    @Test
    void search_mapsRowsAndReturnsPagedResults() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), this.<SubmissionHistoryRowResponse>rowMapper()))
                .thenAnswer(inv -> List.of(rowMapperOf(inv).mapRow(rs, 0)));
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(25L);

        Page<SubmissionHistoryRowResponse> page = repository.search(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(25);
    }

    @Test
    void search_nullCount_treatedAsZero() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), this.<SubmissionHistoryRowResponse>rowMapper()))
                .thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(null);

        assertThat(repository.search(PageRequest.of(0, 10)).getTotalElements()).isZero();
    }

    // ---------------------------------------------------------------
    // findDetail
    // ---------------------------------------------------------------

    @Test
    void findDetail_present_mapsHeaderInvoicesAndLineItems() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        // Exercise both formatClient branches through the invoice mapper. Lenient
        // because the header/line-item mappers read other (unstubbed) columns.
        lenient().when(rs.getString("seller_client_number")).thenReturn("126920");
        lenient().when(rs.getString("seller_client_locn_code")).thenReturn("00");

        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), this.<Object>rowMapper()))
                .thenAnswer(inv -> rowMapperOf(inv).mapRow(rs, 0));
        when(jdbc.query(anyString(), any(SqlParameterSource.class), this.<Object>rowMapper()))
                .thenAnswer(inv -> List.of(rowMapperOf(inv).mapRow(rs, 0)));

        Optional<SubmissionDetailResponse> result = repository.findDetail(200456L);

        assertThat(result).isPresent();
        assertThat(result.get().invoices()).hasSize(1);
        assertThat(result.get().lineItems()).hasSize(1);
        assertThat(result.get().invoices().get(0).sellerClient()).isEqualTo("126920/00");
    }

    @Test
    void findDetail_notFound_returnsEmpty() {
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), this.<Object>rowMapper()))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThat(repository.findDetail(999L)).isEmpty();
    }

    // ---------------------------------------------------------------
    // findInvoiceComments
    // ---------------------------------------------------------------

    @Test
    void findInvoiceComments_mapsRows() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("invoice_number")).thenReturn("WFP521046");
        when(rs.getString("status")).thenReturn("Approved");
        when(rs.getString("comment_text")).thenReturn("Looks good");

        when(jdbc.query(anyString(), any(SqlParameterSource.class), this.<SubmissionInvoiceCommentResponse>rowMapper()))
                .thenAnswer(inv -> List.of(rowMapperOf(inv).mapRow(rs, 0)));

        List<SubmissionInvoiceCommentResponse> comments = repository.findInvoiceComments(200456L);

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).invoiceNumber()).isEqualTo("WFP521046");
        assertThat(comments.get(0).comment()).isEqualTo("Looks good");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Typed {@code any()} matcher for the RowMapper argument. */
    private <T> RowMapper<T> rowMapper() {
        return any();
    }

    /** Pulls the RowMapper the repository passed to the mocked jdbc call. */
    @SuppressWarnings("unchecked")
    private <T> RowMapper<T> rowMapperOf(org.mockito.invocation.InvocationOnMock inv) {
        return (RowMapper<T>) inv.getArgument(2);
    }
}
