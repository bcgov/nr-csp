package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.R12Api;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R12ReportRequest;
import ca.bc.gov.nrs.csp.backend.service.R12Service;
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
public class R12Controller implements R12Api {

    private static final Logger log = LoggerFactory.getLogger(R12Controller.class);

    private final R12Service r12Service;

    public R12Controller(R12Service r12Service) {
        this.r12Service = r12Service;
    }

    @Override
    public ResponseEntity<Resource> generateR12Report(R12ReportRequest request) {
        ReportResult result = r12Service.generateReport(request);
        String format = request.getReportFormat().getValue();
        log.info("POST   /api/R12 → 200 OK\n       body: {}", request);
        MediaType mediaType = "CSV".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(mediaType)
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }
}
