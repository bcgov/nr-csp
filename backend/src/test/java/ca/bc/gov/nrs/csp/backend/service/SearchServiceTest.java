package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.repository.ClientLocationRepository;
import ca.bc.gov.nrs.csp.backend.repository.SearchRepository;
import ca.bc.gov.nrs.csp.backend.service.model.ClientLocation;
import ca.bc.gov.nrs.csp.backend.service.model.SearchCriteria;
import ca.bc.gov.nrs.csp.backend.service.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    private static final Pageable PAGE = PageRequest.of(0, 10);

    @Mock SearchRepository searchRepository;
    @Mock ClientLocationRepository clientLocationRepository;

    SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(searchRepository, clientLocationRepository);
    }

    // ---------------------------------------------------------------
    // search() — date validation
    // ---------------------------------------------------------------

    @Test
    void search_startDateAfterEndDate_throwsBadRequest() {
        SearchCriteria criteria = criteriaWith(
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 1, 1)
        );

        assertThatThrownBy(() -> searchService.search(criteria, PAGE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Start date must not be after end date");
    }

    @Test
    void search_startDateEqualsEndDate_doesNotThrow() {
        LocalDate same = LocalDate.of(2024, 1, 15);
        SearchCriteria criteria = criteriaWith(same, same);
        given(searchRepository.search(any(), any())).willReturn(emptyPage());

        assertThat(searchService.search(criteria, PAGE).getContent()).isEmpty();
    }

    // ---------------------------------------------------------------
    // search() — submitter client number normalisation
    // ---------------------------------------------------------------

    @Test
    void search_nonNumericSubmitterClientNum_throwsBadRequest() {
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, "ABC123", null, null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> searchService.search(criteria, PAGE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be numeric");
    }

    @Test
    void search_submitterClientNum_isPaddedToEightDigits() {
        SearchCriteria criteria = new SearchCriteria(
                null, null, null, "14963", null, null, null, null, null, null, null, null
        );
        given(searchRepository.search(any(), any())).willReturn(emptyPage());

        searchService.search(criteria, PAGE);

        ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(searchRepository).search(captor.capture(), any());
        assertThat(captor.getValue().submitterClientNum()).isEqualTo("00014963");
    }

    @Test
    void search_validCriteria_returnsRepositoryResults() {
        SearchResult result = new SearchResult(200456L, 1L, null, "APP", "WFP521046",
                LocalDate.of(2024, 1, 31), "SAL", "014963285", "ACME LOGGING LTD", "O", "ESF");
        given(searchRepository.search(any(), any())).willReturn(new PageImpl<>(List.of(result), PAGE, 1));

        Page<SearchResult> results = searchService.search(emptyCriteria(), PAGE);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).invoiceNumber()).isEqualTo("WFP521046");
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    // ---------------------------------------------------------------
    // findClientsByName()
    // ---------------------------------------------------------------

    @Test
    void findClientsByName_nullName_throwsBadRequest() {
        assertThatThrownBy(() -> searchService.findClientsByName(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void findClientsByName_blankName_throwsBadRequest() {
        assertThatThrownBy(() -> searchService.findClientsByName("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void findClientsByName_validName_returnsResults() {
        ClientLocation client = new ClientLocation("00014963", "ACME LOGGING LTD", "00", "HEAD OFFICE", "VICTORIA", "BC");
        given(clientLocationRepository.findByName("acme")).willReturn(List.of(client));

        List<ClientLocation> results = searchService.findClientsByName("acme");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).clientName()).isEqualTo("ACME LOGGING LTD");
    }

    @Test
    void findClientsByName_trimsWhitespaceBeforeQuery() {
        given(clientLocationRepository.findByName("acme")).willReturn(List.of());

        searchService.findClientsByName("  acme  ");

        verify(clientLocationRepository).findByName("acme");
    }

    // ---------------------------------------------------------------
    // findClientsByNumber()
    // ---------------------------------------------------------------

    @Test
    void findClientsByNumber_nullNumber_throwsBadRequest() {
        assertThatThrownBy(() -> searchService.findClientsByNumber(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void findClientsByNumber_blankNumber_throwsBadRequest() {
        assertThatThrownBy(() -> searchService.findClientsByNumber("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void findClientsByNumber_nonNumeric_throwsBadRequest() {
        assertThatThrownBy(() -> searchService.findClientsByNumber("ABC"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be numeric");
    }

    @Test
    void findClientsByNumber_shortNumber_isPaddedToEightDigits() {
        given(clientLocationRepository.findByNumber("00014963")).willReturn(List.of());

        searchService.findClientsByNumber("14963");

        verify(clientLocationRepository).findByNumber("00014963");
    }

    @Test
    void findClientsByNumber_validNumber_returnsResults() {
        ClientLocation client = new ClientLocation("00014963", "ACME LOGGING LTD", "00", "HEAD OFFICE", "VICTORIA", "BC");
        given(clientLocationRepository.findByNumber("00014963")).willReturn(List.of(client));

        List<ClientLocation> results = searchService.findClientsByNumber("14963");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).clientNumber()).isEqualTo("00014963");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private SearchCriteria emptyCriteria() {
        return new SearchCriteria(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private SearchCriteria criteriaWith(LocalDate startDate, LocalDate endDate) {
        return new SearchCriteria(null, startDate, endDate, null, null, null, null, null, null, null, null, null);
    }

    private Page<SearchResult> emptyPage() {
        return new PageImpl<>(List.of(), PAGE, 0);
    }
}
