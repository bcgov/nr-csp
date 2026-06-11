package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceResponse;
import ca.bc.gov.nrs.csp.backend.exception.ConflictException;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.service.InvoiceService;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc standalone tests for {@link InvoiceController}. They verify the HTTP
 * contract of every endpoint — routing, bean-validation 400s, the success
 * status codes, and the exception-to-status mapping wired up by
 * {@link GlobalApiExceptionHandler} (404 for not-found, 409 for conflict).
 * The service is fully mocked; service behaviour is covered by InvoiceServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    InvoiceService invoiceService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InvoiceController(invoiceService))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .build();
    }

    private InvoiceResponse sampleResponse() {
        return new InvoiceResponse(
                1L, 10L, "INV-001", LocalDate.of(2026, 1, 15), "DFT", "SAL", "M", "FOB01", "SORT01",
                new BigDecimal("100.00"), 10, new BigDecimal("5.0"),
                "1234", "00", "Seller", "1234", "00",
                "5678", "00", "ABC Logging", "Nanaimo", "BC",
                List.of(), List.of(), List.of(),
                null, null, null, null, "user1",
                List.of(), List.of(), List.of());
    }

    // A CreateInvoiceRequest body that satisfies all bean-validation constraints.
    private static final String VALID_CREATE_BODY = """
            {"invNumber":"INV-001","invoiceDate":"2026-01-15","invType":"SAL",
             "submitterClientNum":"1234","submitterLocation":"00","submittedBy":"Seller","manual":true}
            """;

    private static final String VALID_LINE_ITEM_BODY = """
            {"secondSort":"SORT01","species":"SP1","grade":"G1","numOfPieces":50,"price":25.00,"volume":6.25}
            """;

    // ---------------------------------------------------------------
    // GET /api/invoices/{id}
    // ---------------------------------------------------------------

    @Test
    void getById_returns200WithInvoice() throws Exception {
        given(invoiceService.getById(1L)).willReturn(sampleResponse());

        mockMvc.perform(get("/api/invoices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invID").value(1))
                .andExpect(jsonPath("$.invStatus").value("DFT"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        given(invoiceService.getById(99L)).willThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(get("/api/invoices/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // ---------------------------------------------------------------
    // POST /api/invoices
    // ---------------------------------------------------------------

    @Test
    void create_validRequest_returns200() throws Exception {
        given(invoiceService.create(any())).willReturn(sampleResponse());

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invID").value(1));
    }

    // Structurally valid JSON (deserializes cleanly) but every @NotBlank field is
    // blank, so it fails bean validation → 400.
    private static final String BLANK_CREATE_BODY = """
            {"invNumber":" ","invoiceDate":"2026-01-15","invType":" ",
             "submitterClientNum":" ","submitterLocation":" ","submittedBy":" ","manual":true}
            """;

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BLANK_CREATE_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_invalidInvNumberPattern_returns400() throws Exception {
        String body = """
                {"invNumber":"inv 001!","invoiceDate":"2026-01-15","invType":"SAL",
                 "submitterClientNum":"1234","submitterLocation":"00","submittedBy":"Seller","manual":true}
                """;
        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // PUT /api/invoices/{id}
    // ---------------------------------------------------------------

    @Test
    void update_validRequest_returns200() throws Exception {
        given(invoiceService.update(eq(1L), any())).willReturn(sampleResponse());

        mockMvc.perform(put("/api/invoices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invID").value(1));
    }

    @Test
    void update_invalidBody_returns400() throws Exception {
        mockMvc.perform(put("/api/invoices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BLANK_CREATE_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        given(invoiceService.update(eq(99L), any())).willThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(put("/api/invoices/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void update_approvedInvoice_returns409() throws Exception {
        given(invoiceService.update(eq(1L), any())).willThrow(new ConflictException("approved"));

        mockMvc.perform(put("/api/invoices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    // ---------------------------------------------------------------
    // DELETE /api/invoices/{id}
    // ---------------------------------------------------------------

    @Test
    void delete_returns204() throws Exception {
        willDoNothing().given(invoiceService).delete(anyLong());

        mockMvc.perform(delete("/api/invoices/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("not found")).given(invoiceService).delete(99L);

        mockMvc.perform(delete("/api/invoices/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_approvedInvoice_returns409() throws Exception {
        willThrow(new ConflictException("approved")).given(invoiceService).delete(1L);

        mockMvc.perform(delete("/api/invoices/1"))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------
    // POST /api/invoices/{id}/submit
    // ---------------------------------------------------------------

    @Test
    void submit_returns200() throws Exception {
        given(invoiceService.submit(1L)).willReturn(sampleResponse());

        mockMvc.perform(post("/api/invoices/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invID").value(1));
    }

    @Test
    void submit_notDraft_returns409() throws Exception {
        given(invoiceService.submit(1L)).willThrow(new ConflictException("not draft"));

        mockMvc.perform(post("/api/invoices/1/submit"))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------
    // POST /api/invoices/{id}/duplicate
    // ---------------------------------------------------------------

    @Test
    void duplicate_returns200() throws Exception {
        given(invoiceService.duplicate(1L)).willReturn(sampleResponse());

        mockMvc.perform(post("/api/invoices/1/duplicate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invID").value(1));
    }

    @Test
    void duplicate_notFound_returns404() throws Exception {
        given(invoiceService.duplicate(99L)).willThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(post("/api/invoices/99/duplicate"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // PATCH /api/invoices/{id}/status
    // ---------------------------------------------------------------

    @Test
    void changeStatus_validRequest_returns200() throws Exception {
        given(invoiceService.changeStatus(eq(1L), any())).willReturn(sampleResponse());

        mockMvc.perform(patch("/api/invoices/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invID").value(1));
    }

    @Test
    void changeStatus_invalidStatusCode_returns400() throws Exception {
        mockMvc.perform(patch("/api/invoices/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"XXX\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_blankStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/invoices/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_notFound_returns404() throws Exception {
        given(invoiceService.changeStatus(eq(99L), any())).willThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(patch("/api/invoices/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APP\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // POST /api/invoices/{id}/line-items
    // ---------------------------------------------------------------

    @Test
    void addLineItem_validRequest_returns200() throws Exception {
        given(invoiceService.addLineItem(eq(1L), any())).willReturn(sampleResponse());

        mockMvc.perform(post("/api/invoices/1/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LINE_ITEM_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invID").value(1));
    }

    @Test
    void addLineItem_invalidPattern_returns400() throws Exception {
        // species violates ^[A-Z0-9]+$
        mockMvc.perform(post("/api/invoices/1/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"species\":\"sp 1!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addLineItem_notDraft_returns409() throws Exception {
        given(invoiceService.addLineItem(eq(1L), any())).willThrow(new ConflictException("not draft"));

        mockMvc.perform(post("/api/invoices/1/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LINE_ITEM_BODY))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------
    // PATCH /api/invoices/{id}/line-items/{lineId}
    // ---------------------------------------------------------------

    @Test
    void updateLineItem_validRequest_returns200() throws Exception {
        given(invoiceService.updateLineItem(eq(1L), eq(5L), any())).willReturn(sampleResponse());

        mockMvc.perform(patch("/api/invoices/1/line-items/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LINE_ITEM_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invID").value(1));
    }

    @Test
    void updateLineItem_lineNotFound_returns404() throws Exception {
        given(invoiceService.updateLineItem(eq(1L), eq(99L), any()))
                .willThrow(new ResourceNotFoundException("line not found"));

        mockMvc.perform(patch("/api/invoices/1/line-items/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LINE_ITEM_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateLineItem_notDraft_returns409() throws Exception {
        given(invoiceService.updateLineItem(eq(1L), eq(5L), any())).willThrow(new ConflictException("not draft"));

        mockMvc.perform(patch("/api/invoices/1/line-items/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LINE_ITEM_BODY))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------
    // DELETE /api/invoices/{id}/line-items/{lineId}
    // ---------------------------------------------------------------

    @Test
    void deleteLineItem_returns200() throws Exception {
        given(invoiceService.deleteLineItem(1L, 5L)).willReturn(sampleResponse());

        mockMvc.perform(delete("/api/invoices/1/line-items/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invID").value(1));
    }

    @Test
    void deleteLineItem_notFound_returns404() throws Exception {
        given(invoiceService.deleteLineItem(1L, 99L)).willThrow(new ResourceNotFoundException("line not found"));

        mockMvc.perform(delete("/api/invoices/1/line-items/99"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // POST /api/invoices/export/csv | /export/pdf
    // ---------------------------------------------------------------

    @Test
    void exportCsv_returns200WithCsvContentTypeAndDisposition() throws Exception {
        given(invoiceService.exportCsv(any()))
                .willReturn(new ReportResult("Group,Species\n1,FIR\n".getBytes(), "INV-001-summary.csv"));

        mockMvc.perform(post("/api/invoices/export/csv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceNumber\":\"INV-001\",\"rows\":[]}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("INV-001-summary.csv")));
    }

    @Test
    void exportPdf_returns200WithPdfContentTypeAndDisposition() throws Exception {
        given(invoiceService.exportPdf(any()))
                .willReturn(new ReportResult(new byte[]{1, 2, 3}, "INV-001-summary.pdf"));

        mockMvc.perform(post("/api/invoices/export/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceNumber\":\"INV-001\",\"rows\":[]}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("INV-001-summary.pdf")));
    }
}
