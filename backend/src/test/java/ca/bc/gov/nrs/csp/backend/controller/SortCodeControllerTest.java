package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.SortCodeResponse;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ConflictException;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.service.SortCodeExportService;
import ca.bc.gov.nrs.csp.backend.service.SortCodeService;
import ca.bc.gov.nrs.csp.backend.service.mapper.SortCodeMapper;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SortCodeControllerTest {

    @Mock SortCodeService sortCodeService;
    @Mock SortCodeMapper sortCodeMapper;
    @Mock SortCodeExportService sortCodeExportService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SortCodeController(sortCodeService, sortCodeMapper, sortCodeExportService))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private SortCodeResponse sampleResponse(String code) {
        return new SortCodeResponse(code, "Lumber - Cedar",
                LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(9999, Month.DECEMBER, 31), LocalDate.now());
    }

    // ---------------------------------------------------------------
    // GET /api/sort-codes
    // ---------------------------------------------------------------

    @Test
    void listAll_returns200WithPage() throws Exception {
        Page<SortCodeResponse> page = new PageImpl<>(List.of(sampleResponse("A")), PageRequest.of(0, 10), 1);
        given(sortCodeService.listAll(any())).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 1));
        given(sortCodeMapper.toResponsePage(any())).willReturn(page);

        mockMvc.perform(get("/api/sort-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sortCode").value("A"))
                .andExpect(jsonPath("$.content[0].description").value("Lumber - Cedar"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listAll_emptyPage_returns200WithEmptyContent() throws Exception {
        Page<SortCodeResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(sortCodeService.listAll(any())).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        given(sortCodeMapper.toResponsePage(any())).willReturn(emptyPage);

        mockMvc.perform(get("/api/sort-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---------------------------------------------------------------
    // POST /api/sort-codes
    // ---------------------------------------------------------------

    @Test
    void create_validRequest_returns201() throws Exception {
        String body = """
                {"sortCode":"A","description":"Lumber - Cedar","effectiveDate":"1990-01-01","expiryDate":"9999-12-31"}
                """;
        given(sortCodeService.create(any())).willReturn(null);
        given(sortCodeMapper.toResponse(any())).willReturn(sampleResponse("A"));

        mockMvc.perform(post("/api/sort-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sortCode").value("A"));
    }

    @Test
    void create_duplicateSortCode_returns409() throws Exception {
        String body = """
                {"sortCode":"A","description":"Lumber - Cedar","effectiveDate":"1990-01-01","expiryDate":"9999-12-31"}
                """;
        given(sortCodeService.create(any())).willThrow(new ConflictException("Sort code 'A' already exists."));

        mockMvc.perform(post("/api/sort-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Sort code 'A' already exists."));
    }

    @Test
    void create_invalidDateOrder_returns400() throws Exception {
        String body = """
                {"sortCode":"A","description":"Lumber - Cedar","effectiveDate":"2025-01-01","expiryDate":"2024-01-01"}
                """;
        given(sortCodeService.create(any())).willThrow(new BadRequestException("Effective date must not be after expiry date."));

        mockMvc.perform(post("/api/sort-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        String body = """
                {"sortCode":"","description":"","effectiveDate":null,"expiryDate":null}
                """;

        mockMvc.perform(post("/api/sort-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // PUT /api/sort-codes/{code}
    // ---------------------------------------------------------------

    @Test
    void update_validRequest_returns200() throws Exception {
        String body = """
                {"description":"Updated","effectiveDate":"2000-01-01","expiryDate":"9999-12-31"}
                """;
        given(sortCodeService.update(eq("A"), any())).willReturn(null);
        given(sortCodeMapper.toResponse(any())).willReturn(
                new SortCodeResponse("A", "Updated", LocalDate.of(2000, Month.JANUARY, 1), LocalDate.of(9999, Month.DECEMBER, 31), LocalDate.now()));

        mockMvc.perform(put("/api/sort-codes/A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        String body = """
                {"description":"Updated","effectiveDate":"2000-01-01","expiryDate":"9999-12-31"}
                """;
        given(sortCodeService.update(eq("Z"), any()))
                .willThrow(new ResourceNotFoundException("Sort code 'Z' not found."));

        mockMvc.perform(put("/api/sort-codes/Z")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void update_invalidDateOrder_returns400() throws Exception {
        String body = """
                {"description":"Updated","effectiveDate":"2030-01-01","expiryDate":"2020-01-01"}
                """;
        given(sortCodeService.update(eq("A"), any()))
                .willThrow(new BadRequestException("Effective date must not be after expiry date."));

        mockMvc.perform(put("/api/sort-codes/A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // ---------------------------------------------------------------
    // DELETE /api/sort-codes/{code}
    // ---------------------------------------------------------------

    @Test
    void delete_found_returns204() throws Exception {
        willDoNothing().given(sortCodeService).delete("A");

        mockMvc.perform(delete("/api/sort-codes/A"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("Sort code 'Z' not found."))
                .given(sortCodeService).delete("Z");

        mockMvc.perform(delete("/api/sort-codes/Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Sort code 'Z' not found."));
    }

    // ---------------------------------------------------------------
    // GET /api/sort-codes/export/pdf
    // ---------------------------------------------------------------

    @Test
    void exportPdf_returns200WithPdfContentType() throws Exception {
        given(sortCodeExportService.exportPdf())
                .willReturn(new ReportResult(new byte[]{1, 2, 3}, "Sortcodes.pdf"));

        mockMvc.perform(get("/api/sort-codes/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.startsWith("application/pdf");
                })
                .andExpect(result -> {
                    String disposition = result.getResponse().getHeader("Content-Disposition");
                    assert disposition != null && disposition.contains("Sortcodes.pdf");
                });
    }

    // ---------------------------------------------------------------
    // GET /api/sort-codes/export/csv
    // ---------------------------------------------------------------

    @Test
    void exportCsv_returns200WithCsvContentType() throws Exception {
        given(sortCodeExportService.exportCsv())
                .willReturn(new ReportResult("Sort Code,Description\nA,Lumber\n".getBytes(), "Sortcodes.csv"));

        mockMvc.perform(get("/api/sort-codes/export/csv"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.startsWith("text/csv");
                })
                .andExpect(result -> {
                    String disposition = result.getResponse().getHeader("Content-Disposition");
                    assert disposition != null && disposition.contains("Sortcodes.csv");
                });
    }
}
