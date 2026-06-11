package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.api.R08Api;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R08ReportRequest;
import ca.bc.gov.nrs.csp.backend.service.R08Service;
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
public class R08Controller implements R08Api {

    private static final Logger log = LoggerFactory.getLogger(R08Controller.class);

    private final R08Service r08Service;

    public R08Controller(R08Service r08Service) {
        this.r08Service = r08Service;
    }

    @Override
    public ResponseEntity<Resource> generateR08Report(R08ReportRequest request) {
        ReportResult result = r08Service.generateReport(request);
        String format = request.getReportFormat().getValue();
        
        log.info("POST   /api/R08 → 200 OK\n       body: {}", request);
        MediaType mediaType = "CSV".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(mediaType)
                .contentLength(result.data().length)
                .body(new ByteArrayResource(result.data()));
    }
}
