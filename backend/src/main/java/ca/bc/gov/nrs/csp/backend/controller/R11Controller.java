package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.R11Api;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R11ReportRequest;
import ca.bc.gov.nrs.csp.backend.service.R11Service;
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
public class R11Controller implements R11Api {

    private static final Logger log = LoggerFactory.getLogger(R11Controller.class);

    private final R11Service r11Service;

    public R11Controller(R11Service r11Service) {
        this.r11Service = r11Service;
    }

    @Override
    public ResponseEntity<Resource> generateR11Report(R11ReportRequest request) {
        ReportResult result = r11Service.generateReport(request);
        String format = request.getReportFormat().getValue();
        log.info("POST   /api/R11 → 200 OK\n       body: {}", request);
        MediaType mediaType = "CSV".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(mediaType)
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }
}
