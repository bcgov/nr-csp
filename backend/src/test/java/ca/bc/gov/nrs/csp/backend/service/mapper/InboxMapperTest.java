package ca.bc.gov.nrs.csp.backend.service.mapper;

import ca.bc.gov.nrs.csp.backend.controller.dto.inbox.InboxRowResponse;
import ca.bc.gov.nrs.csp.backend.service.model.InboxRow;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InboxMapperTest {

    private final InboxMapper mapper = new InboxMapperImpl();

    private static InboxRow inboxRow() {
        return new InboxRow(
                100L,                        // cspSubmissionId (dropped — not on the response)
                200456L,                     // coastalLogSaleId
                "SUB-123",                   // submissionId
                LocalDate.of(2026, Month.MAY, 19),   // submissionDate
                "In Progress",               // submissionStatus
                "Electronic",                // submissionType
                10,                          // invTotal
                4,                           // invApproved
                2,                           // invRejected
                3,                           // invProcessing
                1                            // invCancelled
        );
    }

    // ---------------------------------------------------------------
    // toResponse()
    // ---------------------------------------------------------------

    @Test
    void toResponse_mapsAllFields() {
        InboxRowResponse response = mapper.toResponse(inboxRow());

        assertThat(response).isNotNull();
        assertThat(response.coastalLogSaleId()).isEqualTo(200456L);
        assertThat(response.submissionId()).isEqualTo("SUB-123");
        assertThat(response.submissionDate()).isEqualTo(LocalDate.of(2026, Month.MAY, 19));
        assertThat(response.submissionStatus()).isEqualTo("In Progress");
        assertThat(response.submissionType()).isEqualTo("Electronic");
        assertThat(response.invTotal()).isEqualTo(10);
        assertThat(response.invApproved()).isEqualTo(4);
        assertThat(response.invRejected()).isEqualTo(2);
        assertThat(response.invProcessing()).isEqualTo(3);
        assertThat(response.invCancelled()).isEqualTo(1);
    }

    @Test
    void toResponse_nullFields_areMappedAsNull() {
        InboxRowResponse response = mapper.toResponse(
                new InboxRow(null, null, null, null, null, null, null, null, null, null, null));

        assertThat(response).isNotNull();
        assertThat(response.coastalLogSaleId()).isNull();
        assertThat(response.submissionId()).isNull();
        assertThat(response.submissionDate()).isNull();
        assertThat(response.submissionStatus()).isNull();
        assertThat(response.submissionType()).isNull();
        assertThat(response.invTotal()).isNull();
        assertThat(response.invApproved()).isNull();
        assertThat(response.invRejected()).isNull();
        assertThat(response.invProcessing()).isNull();
        assertThat(response.invCancelled()).isNull();
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
        InboxRow manual = new InboxRow(
                101L, 200457L, null, LocalDate.of(2026, Month.JUNE, 1),
                "Completed", "Manual", 1, 1, 0, 0, 0);

        List<InboxRowResponse> responses = mapper.toResponseList(List.of(inboxRow(), manual));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).coastalLogSaleId()).isEqualTo(200456L);
        assertThat(responses.get(1).coastalLogSaleId()).isEqualTo(200457L);
        assertThat(responses.get(1).submissionId()).isNull();
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
    // toResponsePage() — default method
    // ---------------------------------------------------------------

    @Test
    void toResponsePage_mapsContentAndPreservesPaging() {
        Page<InboxRow> page = new PageImpl<>(List.of(inboxRow()), PageRequest.of(2, 10), 35);

        Page<InboxRowResponse> mapped = mapper.toResponsePage(page);

        assertThat(mapped.getTotalElements()).isEqualTo(35);
        assertThat(mapped.getNumber()).isEqualTo(2);
        assertThat(mapped.getSize()).isEqualTo(10);
        assertThat(mapped.getContent()).hasSize(1);
        assertThat(mapped.getContent().getFirst().submissionId()).isEqualTo("SUB-123");
    }

    @Test
    void toResponsePage_emptyPage_returnsEmptyPage() {
        Page<InboxRow> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        assertThat(mapper.toResponsePage(page).getContent()).isEmpty();
    }
}
