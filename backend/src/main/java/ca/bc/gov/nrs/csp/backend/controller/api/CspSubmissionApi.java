package ca.bc.gov.nrs.csp.backend.controller.api;

import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionValidationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "CSP Submissions", description = "CSP XML submission intake — structural validation")
@RequestMapping("/api/submissions")
public interface CspSubmissionApi {

    @Operation(summary = "Validate a CSP submission XML against format, ESF envelope, and XSD schema",
            description = "Accepts either a bare <csp:CSPSubmission> document or an ESF-wrapped "
                    + "<esf:ESFSubmission> envelope as a multipart file part named 'file'. Runs "
                    + "format detection, envelope stripping, and schema-bound JAXB validation. "
                    + "Returns 200 with valid=true when the submission passes, or 422 with the "
                    + "collected errors when it does not. Business-rule validation runs separately.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submission is structurally valid",
                    content = @Content(schema = @Schema(implementation = SubmissionValidationResponse.class))),
            @ApiResponse(responseCode = "422", description = "Submission failed format/envelope/schema validation",
                    content = @Content(schema = @Schema(implementation = SubmissionValidationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing or unreadable file part",
                    content = @Content(schema = @Schema(implementation = SubmissionValidationResponse.class)))
    })
    @PostMapping(value = "/validate", consumes = "multipart/form-data")
    ResponseEntity<SubmissionValidationResponse> validate(
            @RequestParam(value = "file", required = false) MultipartFile file);
}
