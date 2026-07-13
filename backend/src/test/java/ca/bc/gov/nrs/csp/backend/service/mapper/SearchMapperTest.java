package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.search.ClientLocationResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.search.SearchResultResponse;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchMapperTest {

    private final SearchMapper mapper = new SearchMapperImpl();

    private static SearchResult searchResult() {
        return new SearchResult(
                200456L,                     // coastalLogSaleId
                100123L,                     // cspSubmissionId
                67890L,                      // submissionId (unmapped source — ignored)
                "APP",                       // invoiceStatus
                "WFP521046",                 // invoiceNumber
                LocalDate.of(2024, Month.JANUARY, 31),   // invoiceDate
                "SAL",                       // type
                "01496328",                  // clientNumber
                "ACME LOGGING LTD",          // clientName
                "O",                         // maturity
                "ESF"                        // submissionType
        );
    }

    private static ClientLocation clientLocation() {
        return new ClientLocation(
                "00014963",
                "ACME LOGGING LTD",
                "00",
                "HEAD OFFICE",
                "VICTORIA",
                "BC"
        );
    }

    // ---------------------------------------------------------------
    // toResponse()
    // ---------------------------------------------------------------

    @Test
    void toResponse_mapsAllFields() {
        SearchResultResponse response = mapper.toResponse(searchResult());

        assertThat(response).isNotNull();
        assertThat(response.coastalLogSaleId()).isEqualTo(200456L);
        assertThat(response.cspSubmissionId()).isEqualTo(100123L);
        assertThat(response.invoiceStatus()).isEqualTo("APP");
        assertThat(response.invoiceNumber()).isEqualTo("WFP521046");
        assertThat(response.invoiceDate()).isEqualTo(LocalDate.of(2024, Month.JANUARY, 31));
        assertThat(response.type()).isEqualTo("SAL");
        assertThat(response.clientNumber()).isEqualTo("01496328");
        assertThat(response.clientName()).isEqualTo("ACME LOGGING LTD");
        assertThat(response.maturity()).isEqualTo("O");
        assertThat(response.submissionType()).isEqualTo("ESF");
    }

    @Test
    void toResponse_nullFields_areMappedAsNull() {
        SearchResultResponse response = mapper.toResponse(new SearchResult(
                null, null, null, null, null, null, null, null, null, null, null));

        assertThat(response).isNotNull();
        assertThat(response.coastalLogSaleId()).isNull();
        assertThat(response.cspSubmissionId()).isNull();
        assertThat(response.invoiceStatus()).isNull();
        assertThat(response.invoiceNumber()).isNull();
        assertThat(response.invoiceDate()).isNull();
        assertThat(response.type()).isNull();
        assertThat(response.clientNumber()).isNull();
        assertThat(response.clientName()).isNull();
        assertThat(response.maturity()).isNull();
        assertThat(response.submissionType()).isNull();
    }

    @Test
    void toResponse_nullInput_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    // ---------------------------------------------------------------
    // toResponseList()
    // ---------------------------------------------------------------

    @Test
    void toResponseList_mapsEveryElementInOrder() {
        SearchResult second = new SearchResult(
                200457L, 100124L, null, "REJ", "WFP521047",
                LocalDate.of(2024, Month.FEBRUARY, 1), "TRD", "01496329",
                "OTHER FORESTRY INC", "I", "Manual");

        List<SearchResultResponse> responses = mapper.toResponseList(List.of(searchResult(), second));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).coastalLogSaleId()).isEqualTo(200456L);
        assertThat(responses.get(1).coastalLogSaleId()).isEqualTo(200457L);
        assertThat(responses.get(1).invoiceStatus()).isEqualTo("REJ");
        assertThat(responses.get(1).submissionType()).isEqualTo("Manual");
    }

    @Test
    void toResponseList_emptyList_returnsEmptyList() {
        assertThat(mapper.toResponseList(List.of())).isEmpty();
    }

    @Test
    void toResponseList_nullInput_returnsNull() {
        assertThat(mapper.toResponseList(null)).isNull();
    }

    // ---------------------------------------------------------------
    // toClientLocationResponse()
    // ---------------------------------------------------------------

    @Test
    void toClientLocationResponse_mapsAllFields() {
        ClientLocationResponse response = mapper.toClientLocationResponse(clientLocation());

        assertThat(response).isNotNull();
        assertThat(response.clientNumber()).isEqualTo("00014963");
        assertThat(response.clientName()).isEqualTo("ACME LOGGING LTD");
        assertThat(response.clientLocnCode()).isEqualTo("00");
        assertThat(response.clientLocnName()).isEqualTo("HEAD OFFICE");
        assertThat(response.city()).isEqualTo("VICTORIA");
        assertThat(response.province()).isEqualTo("BC");
    }

    @Test
    void toClientLocationResponse_nullFields_areMappedAsNull() {
        ClientLocationResponse response = mapper.toClientLocationResponse(
                new ClientLocation(null, null, null, null, null, null));

        assertThat(response).isNotNull();
        assertThat(response.clientNumber()).isNull();
        assertThat(response.clientName()).isNull();
        assertThat(response.clientLocnCode()).isNull();
        assertThat(response.clientLocnName()).isNull();
        assertThat(response.city()).isNull();
        assertThat(response.province()).isNull();
    }

    @Test
    void toClientLocationResponse_nullInput_returnsNull() {
        assertThat(mapper.toClientLocationResponse(null)).isNull();
    }

    // ---------------------------------------------------------------
    // toClientLocationResponseList()
    // ---------------------------------------------------------------

    @Test
    void toClientLocationResponseList_mapsEveryElementInOrder() {
        ClientLocation second = new ClientLocation(
                "00014964", "BETA TIMBER CO", "01", "BRANCH", "NANAIMO", "BC");

        List<ClientLocationResponse> responses =
                mapper.toClientLocationResponseList(List.of(clientLocation(), second));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).clientNumber()).isEqualTo("00014963");
        assertThat(responses.get(1).clientNumber()).isEqualTo("00014964");
        assertThat(responses.get(1).clientLocnName()).isEqualTo("BRANCH");
    }

    @Test
    void toClientLocationResponseList_emptyList_returnsEmptyList() {
        assertThat(mapper.toClientLocationResponseList(List.of())).isEmpty();
    }

    @Test
    void toClientLocationResponseList_nullInput_returnsNull() {
        assertThat(mapper.toClientLocationResponseList(null)).isNull();
    }

    // ---------------------------------------------------------------
    // toResponsePage() — default method
    // ---------------------------------------------------------------

    @Test
    void toResponsePage_mapsContentAndPreservesPaging() {
        Page<SearchResult> page = new PageImpl<>(List.of(searchResult()), PageRequest.of(1, 10), 25);

        Page<SearchResultResponse> mapped = mapper.toResponsePage(page);

        assertThat(mapped.getTotalElements()).isEqualTo(25);
        assertThat(mapped.getNumber()).isEqualTo(1);
        assertThat(mapped.getSize()).isEqualTo(10);
        assertThat(mapped.getContent()).hasSize(1);
        assertThat(mapped.getContent().getFirst().invoiceNumber()).isEqualTo("WFP521046");
    }

    @Test
    void toResponsePage_emptyPage_returnsEmptyPage() {
        Page<SearchResult> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        assertThat(mapper.toResponsePage(page).getContent()).isEmpty();
    }
}
