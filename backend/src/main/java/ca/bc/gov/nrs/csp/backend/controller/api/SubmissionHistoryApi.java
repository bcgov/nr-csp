package ca.bc.gov.nrs.csp.backend.controller.api;

import ca.bc.gov.nrs.csp.backend.controller.dto.error.ApiError;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionDetailResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionHistoryRowResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.submissionhistory.SubmissionInvoiceCommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Submission History", description = "Submission history list and detail")
@RequestMapping("/api")
public interface SubmissionHistoryApi {

    @Operation(
            summary = "List submission history",
            description = """
                    Returns a page of CSP submissions, one row per submission. \
                    Supported sort fields: submissionDate, submittedBy, clientName, submissionStatus. \
                    Default sort: submissionDate,desc."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged submission history results",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SubmissionHistoryRowResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid request (e.g. unknown sort field)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/submission-history")
    ResponseEntity<Page<SubmissionHistoryRowResponse>> listSubmissionHistory(
            @ParameterObject @PageableDefault(size = 100, sort = "submissionDate", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(
            summary = "Get submission detail",
            description = "Returns a single submission's header, contact metadata, invoices, and line items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submission detail",
                    content = @Content(schema = @Schema(implementation = SubmissionDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Submission not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/submission-history/{id}")
    ResponseEntity<SubmissionDetailResponse> getSubmissionDetail(
            @Parameter(description = "CSP submission id.") @PathVariable Long id
    );

    @Operation(
            summary = "Get submission invoice comments",
            description = "Returns each invoice in the submission with its status and reviewer comment, "
                    + "backing the Submission History expanded row's \"Invoice comments\" sub-table."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Per-invoice status and comments",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SubmissionInvoiceCommentResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/submission-history/{id}/invoices")
    ResponseEntity<java.util.List<SubmissionInvoiceCommentResponse>> getSubmissionInvoiceComments(
            @Parameter(description = "CSP submission id.") @PathVariable Long id
    );
}
