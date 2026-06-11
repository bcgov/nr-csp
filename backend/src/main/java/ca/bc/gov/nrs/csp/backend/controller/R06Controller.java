package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.R06Api;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R06ReportRequest;
import ca.bc.gov.nrs.csp.backend.service.R06Service;
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
public class R06Controller implements R06Api {

    private static final Logger log = LoggerFactory.getLogger(R06Controller.class);

    private final R06Service r06Service;

    public R06Controller(R06Service r06Service) {
        this.r06Service = r06Service;
    }

    @Override
    public ResponseEntity<Resource> generateR06Report(R06ReportRequest request) {
        ReportResult result = r06Service.generateReport(request);
        log.info("POST   /api/R06 → 200 OK\n       body: {}", request);
        return buildResponse(result, request.getReportFormat().getValue());
    }

    private ResponseEntity<Resource> buildResponse(ReportResult result, String format) {
        MediaType mediaType = "CSV".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(mediaType)
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }
}
