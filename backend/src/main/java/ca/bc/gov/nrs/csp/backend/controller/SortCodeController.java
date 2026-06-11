package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.SortCodeApi;
import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.CreateSortCodeRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.SortCodeResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.UpdateSortCodeRequest;
import ca.bc.gov.nrs.csp.backend.service.SortCodeExportService;
import ca.bc.gov.nrs.csp.backend.service.SortCodeService;
import ca.bc.gov.nrs.csp.backend.service.mapper.SortCodeMapper;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class SortCodeController implements SortCodeApi {

    private static final Logger log = LoggerFactory.getLogger(SortCodeController.class);

    private final SortCodeService sortCodeService;
    private final SortCodeMapper sortCodeMapper;
    private final SortCodeExportService sortCodeExportService;

    public SortCodeController(SortCodeService sortCodeService, SortCodeMapper sortCodeMapper,
                              SortCodeExportService sortCodeExportService) {
        this.sortCodeService = sortCodeService;
        this.sortCodeMapper = sortCodeMapper;
        this.sortCodeExportService = sortCodeExportService;
    }

    @Override
    public ResponseEntity<Page<SortCodeResponse>> listAll(Pageable pageable) {
        log.info("GET /api/sort-codes page={} size={} sort={}", pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        return ResponseEntity.ok(sortCodeMapper.toResponsePage(sortCodeService.listAll(pageable)));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'prodSortCode/Add New Row')")
    public ResponseEntity<SortCodeResponse> create(CreateSortCodeRequest request) {
        log.info("POST /api/sort-codes code={}", request.sortCode());
        SortCodeResponse response = sortCodeMapper.toResponse(sortCodeService.create(request));
        return ResponseEntity.created(URI.create("/api/sort-codes/" + response.sortCode())).body(response);
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'prodSortCode/Save')")
    public ResponseEntity<SortCodeResponse> update(String code, UpdateSortCodeRequest request) {
        log.info("PUT /api/sort-codes/{}", code);
        return ResponseEntity.ok(sortCodeMapper.toResponse(sortCodeService.update(code, request)));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'prodSortCode/Delete')")
    public ResponseEntity<Void> delete(String code) {
        log.info("DELETE /api/sort-codes/{}", code);
        sortCodeService.delete(code);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Resource> exportPdf() {
        log.info("GET /api/sort-codes/export/pdf");
        ReportResult result = sortCodeExportService.exportPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }

    @Override
    public ResponseEntity<Resource> exportCsv() {
        log.info("GET /api/sort-codes/export/csv");
        ReportResult result = sortCodeExportService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }
}
