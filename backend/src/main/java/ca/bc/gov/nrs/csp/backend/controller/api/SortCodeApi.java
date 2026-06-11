package ca.bc.gov.nrs.csp.backend.controller.api;

import ca.bc.gov.nrs.csp.backend.controller.dto.error.ApiError;
import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.CreateSortCodeRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.SortCodeResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.UpdateSortCodeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Sort Code", description = "Table maintenance – production sort codes")
@RequestMapping("/api/sort-codes")
public interface SortCodeApi {

    @Operation(summary = "List sort codes", description = "Returns a page of sort codes. Supports server-side pagination and sorting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SortCodeResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    ResponseEntity<Page<SortCodeResponse>> listAll(@ParameterObject @PageableDefault(size = 10) Pageable pageable);

    @Operation(summary = "Create a sort code")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(schema = @Schema(implementation = SortCodeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Sort code already exists",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    ResponseEntity<SortCodeResponse> create(@Valid @RequestBody CreateSortCodeRequest request);

    @Operation(summary = "Update a sort code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = SortCodeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sort code not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{code}")
    ResponseEntity<SortCodeResponse> update(
            @Parameter(description = "Sort code to update", example = "A") @PathVariable String code,
            @Valid @RequestBody UpdateSortCodeRequest request);

    @Operation(summary = "Delete a sort code")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Sort code not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{code}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Sort code to delete", example = "A") @PathVariable String code);

    @Operation(summary = "Export all sort codes as PDF")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF file"),
            @ApiResponse(responseCode = "500", description = "Export failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<Resource> exportPdf();

    @Operation(summary = "Export all sort codes as CSV")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV file"),
            @ApiResponse(responseCode = "500", description = "Export failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping(value = "/export/csv", produces = "text/csv")
    ResponseEntity<Resource> exportCsv();
}
