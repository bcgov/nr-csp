package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.FlatPriceConversionApi;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.CreateFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.CopyFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.FlatPriceConversionResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.UpdateFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.service.FlatPriceConversionExportService;
import ca.bc.gov.nrs.csp.backend.service.FlatPriceConversionService;
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

import java.net.URI;
import java.util.List;

@RestController
public class FlatPriceConversionController implements FlatPriceConversionApi {

    private static final Logger log = LoggerFactory.getLogger(FlatPriceConversionController.class);

    private final FlatPriceConversionService service;
    private final FlatPriceConversionExportService exportService;

    public FlatPriceConversionController(FlatPriceConversionService service,
                                          FlatPriceConversionExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @Override
    public ResponseEntity<List<FlatPriceConversionResponse>> search(String modellingCode, String maturity, String sortCode, String species, String grade) {
        log.info("GET /api/flat-price-conversions modellingCode={}", modellingCode);
        return ResponseEntity.ok(service.search(modellingCode, maturity, sortCode, species, grade));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'prodFlatPriceConv/Add New Row')")
    public ResponseEntity<FlatPriceConversionResponse> create(CreateFlatPriceConversionRequest request) {
        log.info("POST /api/flat-price-conversions modellingCode={}", request.modellingCode());
        FlatPriceConversionResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/flat-price-conversions/" + created.id())).body(created);
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'prodFlatPriceConv/Edit')")
    public ResponseEntity<FlatPriceConversionResponse> update(Long id, UpdateFlatPriceConversionRequest request) {
        log.info("PUT /api/flat-price-conversions/{}", id);
        return ResponseEntity.ok(service.update(id, request));
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'prodFlatPriceConv/Delete')")
    public ResponseEntity<Void> delete(Long id) {
        log.info("DELETE /api/flat-price-conversions/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'modelFlatPriceConv/Copy Data')")
    public ResponseEntity<Void> copy(CopyFlatPriceConversionRequest request) {
        log.info("POST /api/flat-price-conversions/copy {} → {}", request.sourceModellingCode(), request.targetModellingCode());
        service.copy(request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@permissionService.hasPermission(authentication, 'modelFlatPriceConv/Clear All/Create New')")
    public ResponseEntity<Void> clear(String modellingCode) {
        log.info("DELETE /api/flat-price-conversions/clear/{}", modellingCode);
        service.clearAll(modellingCode);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Resource> exportPdf(String modellingCode, String maturity, String sortCode, String species, String grade) {
        log.info("GET /api/flat-price-conversions/export/pdf modellingCode={}", modellingCode);
        ReportResult result = exportService.exportPdf(modellingCode, maturity, sortCode, species, grade);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }

    @Override
    public ResponseEntity<Resource> exportCsv(String modellingCode, String maturity, String sortCode, String species, String grade) {
        log.info("GET /api/flat-price-conversions/export/csv modellingCode={}", modellingCode);
        ReportResult result = exportService.exportCsv(modellingCode, maturity, sortCode, species, grade);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }
}
