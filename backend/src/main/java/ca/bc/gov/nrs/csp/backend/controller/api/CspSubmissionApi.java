package ca.bc.gov.nrs.csp.backend.controller.api;

import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionParseResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionSubmitResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submission.SubmissionValidationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "CSP Submissions",
        description = "CSP XML submission intake — structural and business validation as separate endpoints")
@RequestMapping("/api/submissions")
public interface CspSubmissionApi {

    @Operation(summary = "Structural validation: format, ESF envelope, and XSD schema",
            description = "Accepts either a bare <csp:CSPSubmission> document or an ESF-wrapped "
                    + "<esf:ESFSubmission> envelope as a multipart file part named 'file'. Runs "
                    + "format detection, envelope stripping, and schema-bound JAXB validation only — "
                    + "no business rules. Returns 200 with valid=true when the submission is "
                    + "structurally valid, or 422 with the collected errors when it is not.")
    @ApiResponse(responseCode = "200", description = "Submission is structurally valid",
            content = @Content(schema = @Schema(implementation = SubmissionValidationResponse.class)))
    @ApiResponse(responseCode = "422", description = "Submission failed format/envelope/schema validation",
            content = @Content(schema = @Schema(implementation = SubmissionValidationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Missing or unreadable file part",
            content = @Content(schema = @Schema(implementation = SubmissionValidationResponse.class)))
    @PostMapping(value = "/validate/structural", consumes = "multipart/form-data")
    ResponseEntity<SubmissionValidationResponse> validateStructural(
            @RequestParam(value = "file", required = false) MultipartFile file);

    @Operation(summary = "Parse: structural validation plus parsed content for the form",
            description = "Accepts the same multipart file part named 'file'. Runs structural "
                    + "validation (format detection, ESF envelope stripping, XSD/JAXB parse) and, "
                    + "on success, returns the parsed submission content (metadata, invoices and "
                    + "line items) used to populate the upload form. Business rules are NOT run "
                    + "here. Returns 200 with the parsed content when structurally valid, or 422 "
                    + "with the structural errors when it is not.")
    @ApiResponse(responseCode = "200", description = "Submission parsed; content returned",
            content = @Content(schema = @Schema(implementation = SubmissionParseResponse.class)))
    @ApiResponse(responseCode = "422", description = "Submission failed format/envelope/schema validation",
            content = @Content(schema = @Schema(implementation = SubmissionParseResponse.class)))
    @ApiResponse(responseCode = "400", description = "Missing or unreadable file part",
            content = @Content(schema = @Schema(implementation = SubmissionParseResponse.class)))
    @PostMapping(value = "/parse", consumes = "multipart/form-data")
    ResponseEntity<SubmissionParseResponse> parse(
            @RequestParam(value = "file", required = false) MultipartFile file);

    @Operation(summary = "Business-rule validation",
            description = "Accepts the same multipart file part named 'file'. Parses the submission "
                    + "(structural validation runs first because the rules operate on the parsed "
                    + "document), then applies the CSP business rules. Returns 200 with valid=true "
                    + "when accepted (including any non-blocking warnings), or 422 with the rule "
                    + "violations. If the XML fails to parse, the structural errors are returned "
                    + "(422), since business rules cannot run on an unparseable document.")
    @ApiResponse(responseCode = "200", description = "Submission passed business validation",
            content = @Content(schema = @Schema(implementation = SubmissionValidationResponse.class)))
    @ApiResponse(responseCode = "422", description = "Submission failed business rules (or failed to parse)",
            content = @Content(schema = @Schema(implementation = SubmissionValidationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Missing or unreadable file part",
            content = @Content(schema = @Schema(implementation = SubmissionValidationResponse.class)))
    @PostMapping(value = "/validate/business", consumes = "multipart/form-data")
    ResponseEntity<SubmissionValidationResponse> validateBusiness(
            @RequestParam(value = "file", required = false) MultipartFile file);

    @Operation(summary = "Submit: business-validate and persist the submission",
            description = "Accepts the same multipart file part named 'file'. Runs business "
                    + "validation and, only when the submission is fully accepted (no rejected "
                    + "invoices), persists it — a csp_submission row plus one invoice per parsed "
                    + "invoice with its line items — and returns the new submissionId. If any "
                    + "invoice is rejected the submission is NOT saved and the validation messages "
                    + "are returned (422).")
    @ApiResponse(responseCode = "200", description = "Submission saved; submissionId returned",
            content = @Content(schema = @Schema(implementation = SubmissionSubmitResponse.class)))
    @ApiResponse(responseCode = "422", description = "Submission failed business rules and was not saved",
            content = @Content(schema = @Schema(implementation = SubmissionSubmitResponse.class)))
    @ApiResponse(responseCode = "400", description = "Missing or unreadable file part",
            content = @Content(schema = @Schema(implementation = SubmissionSubmitResponse.class)))
    @PostMapping(value = "/submit", consumes = "multipart/form-data")
    ResponseEntity<SubmissionSubmitResponse> submit(
            @RequestParam(value = "file", required = false) MultipartFile file);
}
