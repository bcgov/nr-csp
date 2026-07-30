package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.lookup.LookupItemResponse;
import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LookupMapperTest {

    private final LookupMapper mapper = new LookupMapperImpl();

    // ---------------------------------------------------------------
    // toResponse()
    // ---------------------------------------------------------------

    @Test
    void toResponse_mapsAllFields() {
        LookupItemResponse response = mapper.toResponse(new LookupItem("SAL", "Sales"));

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("SAL");
        assertThat(response.description()).isEqualTo("Sales");
    }

    @Test
    void toResponse_nullFields_areMappedAsNull() {
        LookupItemResponse response = mapper.toResponse(new LookupItem(null, null));

        assertThat(response).isNotNull();
        assertThat(response.code()).isNull();
        assertThat(response.description()).isNull();
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
        List<LookupItemResponse> responses = mapper.toResponseList(List.of(
                new LookupItem("SAL", "Sales"),
                new LookupItem("TRD", "Trade")
        ));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).code()).isEqualTo("SAL");
        assertThat(responses.get(0).description()).isEqualTo("Sales");
        assertThat(responses.get(1).code()).isEqualTo("TRD");
        assertThat(responses.get(1).description()).isEqualTo("Trade");
    }

    @Test
    void toResponseList_emptyList_returnsEmptyList() {
        assertThat(mapper.toResponseList(List.of())).isEmpty();
    }

    @Test
    void toResponseList_nullInput_returnsNull() {
        assertThat(mapper.toResponseList(null)).isNull();
    }
}
