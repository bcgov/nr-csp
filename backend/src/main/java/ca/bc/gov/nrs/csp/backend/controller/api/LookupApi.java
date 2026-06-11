package ca.bc.gov.nrs.csp.backend.controller.api;

import ca.bc.gov.nrs.csp.backend.controller.dto.error.ApiError;
import ca.bc.gov.nrs.csp.backend.controller.dto.lookup.LookupItemResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.lookup.SpeciesGradeCombinationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Lookup", description = "Reference data for dropdown options")
@RequestMapping("/api/lookup")
public interface LookupApi {

    @Operation(summary = "Get maturity codes", description = "Returns all valid values from LOG_SALE_TYPE_CODE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maturity codes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LookupItemResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/maturity")
    ResponseEntity<List<LookupItemResponse>> getMaturityCodes();

    @Operation(summary = "Get invoice type codes", description = "Returns all valid values from CSP_INVOICE_TYPE_CODE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice type codes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LookupItemResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/type")
    ResponseEntity<List<LookupItemResponse>> getInvoiceTypes();

    @Operation(summary = "Get status codes", description = "Returns all valid values from LOG_SALE_ENTRY_STATUS_CODE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status codes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LookupItemResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/status")
    ResponseEntity<List<LookupItemResponse>> getInvoiceStatuses();

    @Operation(summary = "Get submission status codes", description = "Returns all valid values from CSP_SUBMISSION_STATUS_CODE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submission status codes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LookupItemResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/submission-status")
    ResponseEntity<List<LookupItemResponse>> getSubmissionStatuses();

@Operation(summary = "Get sort codes", description = "Returns all active values from LOG_SALE_SORT_CODE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sort codes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LookupItemResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/sort-code")
    ResponseEntity<List<LookupItemResponse>> getSortCodes();

    @Operation(summary = "Get species codes", description = "Returns all active values from LOG_SALE_SPECIES_CODE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Species codes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LookupItemResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/species")
    ResponseEntity<List<LookupItemResponse>> getSpeciesCodes();

    @Operation(summary = "Get grade codes", description = "Returns all active values from LOG_SALE_GRADE_CODE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade codes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LookupItemResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/grade")
    ResponseEntity<List<LookupItemResponse>> getGradeCodes();

    @Operation(summary = "Get grade codes by species", description = "Returns distinct active grades for the given species via CSP_SPECIES_GRADE_XREF.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade codes for the specified species",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LookupItemResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/modelling-code")
    ResponseEntity<List<LookupItemResponse>> getModellingCodes();

    @Operation(summary = "Get FOB location codes", description = "Returns all currently-effective values from LOG_SALE_FOB_LOCATION_CODE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FOB location codes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LookupItemResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/fob")
    ResponseEntity<List<LookupItemResponse>> getFobCodes();
    @GetMapping("/grade-by-species/{species}")
    ResponseEntity<List<LookupItemResponse>> getGradesBySpecies(
            @Parameter(description = "Species code to filter grades by", example = "FD")
            @PathVariable String species);

    @Operation(summary = "Get all valid species/grade combinations",
            description = "Returns every active row in THE.csp_species_grade_xref as a flat list of (species, grade) pairs. " +
                    "Designed to be loaded once per session so the frontend can filter the Species and Grade dropdowns " +
                    "against each other in real time without per-change round-trips.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Species/grade combinations",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SpeciesGradeCombinationResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/species-grade-combinations")
    ResponseEntity<List<SpeciesGradeCombinationResponse>> getSpeciesGradeCombinations();
}
