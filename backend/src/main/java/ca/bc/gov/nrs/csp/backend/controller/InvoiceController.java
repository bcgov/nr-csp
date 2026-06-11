package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.InvoiceApi;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ChangeStatusRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.CreateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceSummaryExportRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.InvoiceResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.LineItemRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.UpdateInvoiceRequest;
import ca.bc.gov.nrs.csp.backend.service.InvoiceService;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvoiceController implements InvoiceApi {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Override
    public ResponseEntity<InvoiceResponse> getById(Long id) {
        log.info("GET /api/invoices/{}", id);
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Save')")
    public ResponseEntity<InvoiceResponse> create(CreateInvoiceRequest request) {
        log.info("POST /api/invoices invNumber={} invoiceDate={} invType={}", request.invNumber(), request.invoiceDate(), request.invType());
        return ResponseEntity.ok(invoiceService.create(request));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Save')")
    public ResponseEntity<InvoiceResponse> update(Long id, UpdateInvoiceRequest request) {
        log.info("PUT /api/invoices/{} invNumber={} invType={}", id, request.invNumber(), request.invType());
        return ResponseEntity.ok(invoiceService.update(id, request));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Delete')")
    public ResponseEntity<Void> delete(Long id) {
        log.info("DELETE /api/invoices/{}", id);
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Submit')")
    public ResponseEntity<InvoiceResponse> submit(Long id) {
        log.info("POST /api/invoices/{}/submit", id);
        return ResponseEntity.ok(invoiceService.submit(id));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Duplicate')")
    public ResponseEntity<InvoiceResponse> duplicate(Long id) {
        log.info("POST /api/invoices/{}/duplicate", id);
        return ResponseEntity.ok(invoiceService.duplicate(id));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Approve') or @permissionService.hasPermission(authentication, 'invoiceDetails/Reject') or @permissionService.hasPermission(authentication, 'invoiceDetails/Cancel') or @permissionService.hasPermission(authentication, 'invoiceDetails/Unapprove')")
    public ResponseEntity<InvoiceResponse> changeStatus(Long id, ChangeStatusRequest request) {
        log.info("PATCH /api/invoices/{}/status status={}", id, request.status());
        return ResponseEntity.ok(invoiceService.changeStatus(id, request));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/New Line Item')")
    public ResponseEntity<InvoiceResponse> addLineItem(Long id, LineItemRequest request) {
        log.info("POST /api/invoices/{}/line-items secondSort={} species={} grade={}", id, request.secondSort(), request.species(), request.grade());
        return ResponseEntity.ok(invoiceService.addLineItem(id, request));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Update Group')")
    public ResponseEntity<InvoiceResponse> updateLineItem(Long id, Long lineId, LineItemRequest request) {
        log.info("PATCH /api/invoices/{}/line-items/{} secondSort={} species={} grade={}", id, lineId, request.secondSort(), request.species(), request.grade());
        return ResponseEntity.ok(invoiceService.updateLineItem(id, lineId, request));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'invoiceDetails/Delete')")
    public ResponseEntity<InvoiceResponse> deleteLineItem(Long id, Long lineId) {
        log.info("DELETE /api/invoices/{}/line-items/{}", id, lineId);
        return ResponseEntity.ok(invoiceService.deleteLineItem(id, lineId));
    }

    @Override
    public ResponseEntity<Resource> exportGroupSummaryCsv(InvoiceSummaryExportRequest request) {
        log.info("POST /api/invoices/group-summary/export/csv rows={}", request != null && request.rows() != null ? request.rows().size() : 0);
        return fileResponse(invoiceService.exportCsv(request), MediaType.parseMediaType("text/csv"));
    }

    @Override
    public ResponseEntity<Resource> exportGroupSummaryPdf(InvoiceSummaryExportRequest request) {
        log.info("POST /api/invoices/group-summary/export/pdf rows={}", request != null && request.rows() != null ? request.rows().size() : 0);
        return fileResponse(invoiceService.exportPdf(request), MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<Resource> fileResponse(ReportResult result, MediaType contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(contentType)
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }
}
