package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.FlatPriceConversionResponse;
import ca.bc.gov.nrs.csp.backend.exception.ConflictException;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.service.FlatPriceConversionExportService;
import ca.bc.gov.nrs.csp.backend.service.FlatPriceConversionService;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FlatPriceConversionControllerTest {

    @Mock FlatPriceConversionService service;
    @Mock FlatPriceConversionExportService exportService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FlatPriceConversionController(service, exportService))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private FlatPriceConversionResponse sampleResponse() {
        return new FlatPriceConversionResponse(
                1L, "P", "S", "FD", "U", "A", 100,
                LocalDate.of(1990, 1, 1), null, 1,
                null, null, null, null
        );
    }

    // ---------------------------------------------------------------
    // GET /api/flat-price-conversions/export/pdf
    // ---------------------------------------------------------------

    @Test
    void exportPdf_returns200WithPdfContentTypeAndDispositionHeader() throws Exception {
        given(exportService.exportPdf("P", null, null, null, null))
                .willReturn(new ReportResult(new byte[]{1, 2, 3}, "FlatPriceConversions.pdf"));

        mockMvc.perform(get("/api/flat-price-conversions/export/pdf").param("modellingCode", "P"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("FlatPriceConversions.pdf")));
    }

    @Test
    void exportCsv_returns200WithCsvContentTypeAndDispositionHeader() throws Exception {
        given(exportService.exportCsv("P", null, null, null, null))
                .willReturn(new ReportResult("Maturity,Species\nS,FD\n".getBytes(), "FlatPriceConversions.csv"));

        mockMvc.perform(get("/api/flat-price-conversions/export/csv").param("modellingCode", "P"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("FlatPriceConversions.csv")));
    }

    // ---------------------------------------------------------------
    // GET /api/flat-price-conversions
    // ---------------------------------------------------------------

    @Test
    void search_withModellingCode_returns200WithResults() throws Exception {
        given(service.search(eq("P"), any(), any(), any(), any()))
                .willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/flat-price-conversions").param("modellingCode", "P"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modellingCode").value("P"));
    }

    @Test
    void search_missingModellingCode_returns400() throws Exception {
        mockMvc.perform(get("/api/flat-price-conversions"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // POST /api/flat-price-conversions
    // ---------------------------------------------------------------

    @Test
    void create_validRequest_returns201() throws Exception {
        String body = """
                {"modellingCode":"P","details":{"maturity":"S","species":"FD","grade":"U","sortCode":"A","flatPriceConversion":100,"effectiveDate":"1990-01-01","expiryDate":null}}
                """;
        given(service.create(any())).willReturn(sampleResponse());

        mockMvc.perform(post("/api/flat-price-conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/flat-price-conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        String body = """
                {"modellingCode":"P","details":{"maturity":"S","species":"FD","grade":"U","sortCode":"A","flatPriceConversion":100,"effectiveDate":"1990-01-01","expiryDate":null}}
                """;
        given(service.create(any())).willThrow(new ConflictException("duplicate"));

        mockMvc.perform(post("/api/flat-price-conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    // ---------------------------------------------------------------
    // PUT /api/flat-price-conversions/{id}
    // ---------------------------------------------------------------

    @Test
    void update_validRequest_returns200() throws Exception {
        String body = """
                {"revisionCount":1,"details":{"maturity":"S","species":"FD","grade":"U","sortCode":"A","flatPriceConversion":100,"effectiveDate":"1990-01-01","expiryDate":null}}
                """;
        given(service.update(eq(1L), any())).willReturn(sampleResponse());

        mockMvc.perform(put("/api/flat-price-conversions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        String body = """
                {"revisionCount":1,"details":{"maturity":"S","species":"FD","grade":"U","sortCode":"A","flatPriceConversion":100,"effectiveDate":"1990-01-01","expiryDate":null}}
                """;
        given(service.update(eq(99L), any())).willThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(put("/api/flat-price-conversions/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void update_revisionConflict_returns409() throws Exception {
        String body = """
                {"revisionCount":1,"details":{"maturity":"S","species":"FD","grade":"U","sortCode":"A","flatPriceConversion":100,"effectiveDate":"1990-01-01","expiryDate":null}}
                """;
        given(service.update(eq(1L), any())).willThrow(new ConflictException("conflict"));

        mockMvc.perform(put("/api/flat-price-conversions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------
    // DELETE /api/flat-price-conversions/{id}
    // ---------------------------------------------------------------

    @Test
    void delete_found_returns204() throws Exception {
        willDoNothing().given(service).delete(anyLong());

        mockMvc.perform(delete("/api/flat-price-conversions/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("not found")).given(service).delete(99L);

        mockMvc.perform(delete("/api/flat-price-conversions/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // ---------------------------------------------------------------
    // POST /api/flat-price-conversions/copy
    // ---------------------------------------------------------------

    @Test
    void copy_validRequest_returns204() throws Exception {
        String body = """
                {"sourceModellingCode":"P","targetModellingCode":"M1"}
                """;
        willDoNothing().given(service).copy(any());

        mockMvc.perform(post("/api/flat-price-conversions/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void copy_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/flat-price-conversions/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void copy_sourceNotFound_returns404() throws Exception {
        String body = """
                {"sourceModellingCode":"P","targetModellingCode":"M1"}
                """;
        willThrow(new ResourceNotFoundException("not found")).given(service).copy(any());

        mockMvc.perform(post("/api/flat-price-conversions/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void copy_targetConflict_returns409() throws Exception {
        String body = """
                {"sourceModellingCode":"P","targetModellingCode":"M1"}
                """;
        willThrow(new ConflictException("conflict")).given(service).copy(any());

        mockMvc.perform(post("/api/flat-price-conversions/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------
    // DELETE /api/flat-price-conversions/clear/{modellingCode}
    // ---------------------------------------------------------------

    @Test
    void clear_returns204() throws Exception {
        willDoNothing().given(service).clearAll(anyString());

        mockMvc.perform(delete("/api/flat-price-conversions/clear/M1"))
                .andExpect(status().isNoContent());
    }
}
