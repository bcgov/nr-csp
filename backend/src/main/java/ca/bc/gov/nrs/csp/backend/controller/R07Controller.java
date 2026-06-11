package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.R07Api;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R07ReportRequest;
import ca.bc.gov.nrs.csp.backend.service.R07Service;
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
public class R07Controller implements R07Api {

    private static final Logger log = LoggerFactory.getLogger(R07Controller.class);

    private final R07Service r07Service;

    public R07Controller(R07Service r07Service) {
        this.r07Service = r07Service;
    }

    @Override
    public ResponseEntity<Resource> generateR07Report(R07ReportRequest request) {
        ReportResult result = r07Service.generateReport(request);
        String format = request.getReportFormat().getValue();
        log.info("POST   /api/R07 → 200 OK\n       body: {}", request);
        MediaType mediaType = "CSV".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(mediaType)
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }
}
