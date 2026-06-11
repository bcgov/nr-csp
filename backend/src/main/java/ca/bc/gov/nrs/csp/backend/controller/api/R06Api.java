package ca.bc.gov.nrs.csp.backend.controller.api;

import ca.bc.gov.nrs.csp.backend.controller.dto.error.ApiError;
import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ValidationErrorResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.report.R06ReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Reports", description = "CSP report generation")
@RequestMapping("/api")
public interface R06Api {

    @Operation(summary = "Generate R06 report", description = "Generates the R06 invoice report in PDF or CSV format.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No data found for the given parameters",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Report generation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/R06",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_PDF_VALUE, "text/csv", MediaType.ALL_VALUE})
    ResponseEntity<Resource> generateR06Report(@Valid @RequestBody R06ReportRequest request);
}
