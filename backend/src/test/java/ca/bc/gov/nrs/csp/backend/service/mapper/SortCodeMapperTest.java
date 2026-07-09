package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.SortCodeResponse;
import ca.bc.gov.nrs.csp.backend.service.model.SortCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SortCodeMapperTest {

    private final SortCodeMapper mapper = new SortCodeMapperImpl();

    private static SortCode sortCode() {
        return new SortCode(
                "A",
                "Lumber - Cedar",
                LocalDate.of(1990, Month.JANUARY, 1),
                LocalDate.of(9999, Month.DECEMBER, 31),
                LocalDate.of(2026, Month.JULY, 8)
        );
    }

    // ---------------------------------------------------------------
    // toResponse()
    // ---------------------------------------------------------------

    @Test
    void toResponse_mapsAllFields() {
        SortCodeResponse response = mapper.toResponse(sortCode());

        assertThat(response).isNotNull();
        assertThat(response.sortCode()).isEqualTo("A");
        assertThat(response.description()).isEqualTo("Lumber - Cedar");
        assertThat(response.effectiveDate()).isEqualTo(LocalDate.of(1990, Month.JANUARY, 1));
        assertThat(response.expiryDate()).isEqualTo(LocalDate.of(9999, Month.DECEMBER, 31));
        assertThat(response.updateTimestamp()).isEqualTo(LocalDate.of(2026, Month.JULY, 8));
    }

    @Test
    void toResponse_nullFields_areMappedAsNull() {
        SortCodeResponse response = mapper.toResponse(new SortCode(null, null, null, null, null));

        assertThat(response).isNotNull();
        assertThat(response.sortCode()).isNull();
        assertThat(response.description()).isNull();
        assertThat(response.effectiveDate()).isNull();
        assertThat(response.expiryDate()).isNull();
        assertThat(response.updateTimestamp()).isNull();
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
        SortCode other = new SortCode(
                "B",
                "Lumber - Fir",
                LocalDate.of(2000, Month.JUNE, 15),
                LocalDate.of(2030, Month.JUNE, 15),
                LocalDate.of(2026, Month.JANUARY, 1)
        );

        List<SortCodeResponse> responses = mapper.toResponseList(List.of(sortCode(), other));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).sortCode()).isEqualTo("A");
        assertThat(responses.get(1).sortCode()).isEqualTo("B");
        assertThat(responses.get(1).description()).isEqualTo("Lumber - Fir");
        assertThat(responses.get(1).effectiveDate()).isEqualTo(LocalDate.of(2000, Month.JUNE, 15));
        assertThat(responses.get(1).expiryDate()).isEqualTo(LocalDate.of(2030, Month.JUNE, 15));
        assertThat(responses.get(1).updateTimestamp()).isEqualTo(LocalDate.of(2026, Month.JANUARY, 1));
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
    // toResponsePage() — default method
    // ---------------------------------------------------------------

    @Test
    void toResponsePage_mapsContentAndPreservesPaging() {
        Page<SortCode> page = new PageImpl<>(List.of(sortCode()), PageRequest.of(1, 5), 11);

        Page<SortCodeResponse> mapped = mapper.toResponsePage(page);

        assertThat(mapped.getTotalElements()).isEqualTo(11);
        assertThat(mapped.getNumber()).isEqualTo(1);
        assertThat(mapped.getSize()).isEqualTo(5);
        assertThat(mapped.getContent()).hasSize(1);
        assertThat(mapped.getContent().getFirst().sortCode()).isEqualTo("A");
        assertThat(mapped.getContent().getFirst().description()).isEqualTo("Lumber - Cedar");
    }

    @Test
    void toResponsePage_emptyPage_returnsEmptyPage() {
        Page<SortCode> page = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);

        assertThat(mapper.toResponsePage(page).getContent()).isEmpty();
    }
}
