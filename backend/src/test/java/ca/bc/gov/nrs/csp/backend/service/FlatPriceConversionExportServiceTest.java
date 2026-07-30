package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.repository.FlatPriceConversionRepository;
import ca.bc.gov.nrs.csp.backend.service.model.FlatPriceConversion;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FlatPriceConversionExportServiceTest {

    @Mock
    FlatPriceConversionRepository repository;

    @Mock
    LookupService lookupService;

    @InjectMocks
    FlatPriceConversionExportService service;

    private static final FlatPriceConversion SAMPLE = new FlatPriceConversion(
        1L, "P", "S", "FD", "U", "A", 100,
        LocalDate.of(1990, Month.JANUARY, 1), null,
        1, "JSMITH", LocalDate.of(2024, Month.JANUARY, 15), "JDOE", LocalDate.of(2024, Month.JUNE, 1)
    );

    @BeforeEach
    void setup() {
        given(lookupService.getMaturityCodes()).willReturn(Collections.emptyList());
    }

    @Test
    void exportPdf_returnsPdfBytesWithCorrectFilename() {
        given(repository.search("P", null, null, null, null)).willReturn(List.of(SAMPLE));

        ReportResult result = service.exportPdf("P", null, null, null, null);

        assertThat(result.filename()).isEqualTo("FlatPriceConversions.pdf");
        assertThat(result.data()).isNotEmpty()
                .startsWith(new byte[]{'%', 'P', 'D', 'F'});
    }

    @Test
    void exportCsv_returnsWellFormedCsvWithCorrectFilename() {
        given(repository.search("P", "S", null, "FD", null)).willReturn(List.of(SAMPLE));

        ReportResult result = service.exportCsv("P", "S", null, "FD", null);

        String csv = new String(result.data(), StandardCharsets.UTF_8);
        assertThat(result.filename()).isEqualTo("FlatPriceConversions.csv");
        assertThat(csv)
                .startsWith("Maturity,Species,Sort Code,Grade,Flat Price Conversion,Effective Date,Expiry Date\n")
                .contains("S,FD,A,U,100,1990-01-01,\n");
    }

    @Test
    void exportCsv_emptyResults_returnsHeaderRowOnly() {
        given(repository.search("M1", null, null, null, null)).willReturn(List.of());

        ReportResult result = service.exportCsv("M1", null, null, null, null);

        String csv = new String(result.data(), StandardCharsets.UTF_8);
        assertThat(csv.lines().count()).isEqualTo(1L);
    }

    @Test
    void exportPdf_emptyResults_returnsValidPdf() {
        given(repository.search("M2", null, null, null, null)).willReturn(List.of());

        ReportResult result = service.exportPdf("M2", null, null, null, null);

        assertThat(result.data()).isNotEmpty()
                .startsWith(new byte[]{'%'});
    }
}
