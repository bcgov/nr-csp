package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import ca.bc.gov.nrs.csp.backend.service.model.SortCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SortCodeExportServiceTest {

    @Mock SortCodeService sortCodeService;

    @InjectMocks SortCodeExportService service;

    private static final SortCode SAMPLE = new SortCode(
        "SC1",
        "Douglas Fir",
        LocalDate.of(2020, Month.JANUARY, 1),
        LocalDate.of(2099, Month.DECEMBER, 31),
        LocalDate.of(2024, Month.JUNE, 1)
    );

    private static final SortCode NULL_DATES = new SortCode(
        "SC2",
        "Value with, comma",
        LocalDate.of(2020, Month.JANUARY, 1),
        null,
        null
    );

    @Test
    void exportPdf_returnsFilenameWithPdfExtension() {
        given(sortCodeService.listAll()).willReturn(List.of(SAMPLE));

        ReportResult result = service.exportPdf();

        assertThat(result.filename()).isEqualTo("Sortcodes.pdf");
    }

    @Test
    void exportPdf_returnsPdfMagicBytes() {
        given(sortCodeService.listAll()).willReturn(List.of(SAMPLE));

        ReportResult result = service.exportPdf();

        assertThat(result.data()).isNotEmpty();
        assertThat(result.data()[0]).isEqualTo((byte) '%');
        assertThat(result.data()[1]).isEqualTo((byte) 'P');
        assertThat(result.data()[2]).isEqualTo((byte) 'D');
        assertThat(result.data()[3]).isEqualTo((byte) 'F');
    }

    @Test
    void exportPdf_emptyList_returnsValidPdf() {
        given(sortCodeService.listAll()).willReturn(List.of());

        ReportResult result = service.exportPdf();

        assertThat(result.data()).isNotEmpty();
        assertThat(result.data()[0]).isEqualTo((byte) '%');
    }

    @Test
    void exportCsv_returnsFilenameWithCsvExtension() {
        given(sortCodeService.listAll()).willReturn(List.of(SAMPLE));

        ReportResult result = service.exportCsv();

        assertThat(result.filename()).isEqualTo("Sortcodes.csv");
    }

    @Test
    void exportCsv_containsHeaderRow() {
        given(sortCodeService.listAll()).willReturn(List.of(SAMPLE));

        ReportResult result = service.exportCsv();

        String csv = new String(result.data(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("Sort Code,Description,Effective Date,Expiry Date\n");
    }

    @Test
    void exportCsv_containsSortCodeData() {
        given(sortCodeService.listAll()).willReturn(List.of(SAMPLE));

        ReportResult result = service.exportCsv();

        String csv = new String(result.data(), StandardCharsets.UTF_8);
        assertThat(csv).contains("SC1,Douglas Fir,2020-01-01,2099-12-31");
    }

    @Test
    void exportCsv_emptyList_returnsHeaderOnly() {
        given(sortCodeService.listAll()).willReturn(List.of());

        ReportResult result = service.exportCsv();

        String csv = new String(result.data(), StandardCharsets.UTF_8);
        assertThat(csv.lines().count()).isEqualTo(1L);
    }

    @Test
    void exportCsv_nullDates_rendersEmptyStrings() {
        given(sortCodeService.listAll()).willReturn(List.of(NULL_DATES));

        ReportResult result = service.exportCsv();

        String csv = new String(result.data(), StandardCharsets.UTF_8);
        assertThat(csv).contains("SC2");
        assertThat(csv).contains("2020-01-01,");
    }

    @Test
    void exportCsv_descriptionWithComma_isQuoted() {
        given(sortCodeService.listAll()).willReturn(List.of(NULL_DATES));

        ReportResult result = service.exportCsv();

        String csv = new String(result.data(), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"Value with, comma\"");
    }
}
