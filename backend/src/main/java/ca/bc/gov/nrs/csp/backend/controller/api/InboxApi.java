package ca.bc.gov.nrs.csp.backend.controller.api;

import ca.bc.gov.nrs.csp.backend.controller.dto.error.ApiError;
import ca.bc.gov.nrs.csp.backend.controller.dto.inbox.InboxRowResponse;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "Inbox", description = "Submission inbox search")
@RequestMapping("/api")
public interface InboxApi {

    @Operation(
            summary = "Search submission inbox",
            description = """
                    Returns a page of CSP submissions matching the supplied filter criteria. \
                    Each row includes the submission's invoice status counts (Approved, Rejected, Processing, Cancelled). \
                    All criteria are optional; omitting all criteria returns every submission. \
                    Supported sort fields: submissionId, submissionDate, submissionStatus, submissionType, \
                    invTotal, invApproved, invRejected, invProcessing, invCancelled. \
                    Default sort: submissionDate,desc."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged inbox results",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = InboxRowResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria (e.g. date range reversed, unknown sort field)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/inbox")
    ResponseEntity<Page<InboxRowResponse>> searchInbox(
            @Parameter(description = "Submission date range start (inclusive, yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submissionDateFrom,

            @Parameter(description = "Submission date range end (inclusive, yyyy-MM-dd). Time is treated as end-of-day 23:59:59.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submissionDateTo,

            @Parameter(description = "Submitted by role. Allowed values: Buyer, Seller.",
                    schema = @Schema(allowableValues = {"Buyer", "Seller"}))
            @RequestParam(required = false) String submittedBy,

            @Parameter(description = "Submission type. Allowed values: Electronic, Manual.",
                    schema = @Schema(allowableValues = {"Electronic", "Manual"}))
            @RequestParam(required = false) String submissionType,

            @Parameter(description = "Submission status code from CSP_SUBMISSION_STATUS_CODE (e.g. INB, COM).")
            @RequestParam(required = false) String submissionStatus,

            @Parameter(description = "Invoice number prefix match (case-insensitive, max 15 characters).")
            @RequestParam(required = false) String invoiceNum,

            @Parameter(description = "Submitter client number (set by client autocomplete selection).")
            @RequestParam(required = false) String submitterClientNum,

            @Parameter(description = "Submitter location code (set by client autocomplete selection, paired with submitterClientNum).")
            @RequestParam(required = false) String submitterLocNum,

            @Parameter(description = "Free-text keyword search (case-insensitive contains match) across submission ID, status, type, and date.")
            @RequestParam(required = false) String keyword,

            @ParameterObject @PageableDefault(size = 100, sort = "submissionDate", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    );
}
