package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.R10Api;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R10ReportRequest;
import ca.bc.gov.nrs.csp.backend.service.R10Service;
import ca.bc.gov.nrs.csp.backend.service.model.ReportResult;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class R10Controller implements R10Api {

    private static final Logger log = LoggerFactory.getLogger(R10Controller.class);

    private final R10Service r10Service;

    public R10Controller(R10Service r10Service) {
        this.r10Service = r10Service;
    }

    @Override
    public ResponseEntity<Resource> generateR10Report(R10ReportRequest request) {
        ReportResult result = r10Service.generateReport(request);
        String format = request.getReportFormat().getValue();
        log.info("POST   /api/R10 → 200 OK\n       body: {}", request);
        MediaType mediaType = "CSV".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(mediaType)
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }
}
