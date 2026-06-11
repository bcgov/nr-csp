package ca.bc.gov.nrs.csp.backend.controller.api;

import ca.bc.gov.nrs.csp.backend.controller.dto.error.ApiError;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.CopyFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.CreateFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.FlatPriceConversionResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.UpdateFlatPriceConversionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Flat Price Conversion", description = "Table maintenance – flat price conversion records")
@RequestMapping("/api/flat-price-conversions")
public interface FlatPriceConversionApi {

    @Operation(summary = "Search flat price conversions", description = "Returns all flat price conversion records matching the given filters. modellingCode is required; all other parameters are optional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FlatPriceConversionResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    ResponseEntity<List<FlatPriceConversionResponse>> search(
            @Parameter(description = "Modelling code (required) – \"P\" (production) or \"M1\"/\"M2\"/\"M3\" (modelling scenarios)", example = "P", required = true)
            @RequestParam String modellingCode,
            @Parameter(description = "Maturity code filter (optional)", example = "S")
            @RequestParam(required = false) String maturity,
            @Parameter(description = "Sort code filter (optional)", example = "A")
            @RequestParam(required = false) String sortCode,
            @Parameter(description = "Species code filter (optional)", example = "FD")
            @RequestParam(required = false) String species,
            @Parameter(description = "Grade code filter (optional)", example = "U")
            @RequestParam(required = false) String grade
    );

    @Operation(summary = "Create a flat price conversion record")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(schema = @Schema(implementation = FlatPriceConversionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Record already exists",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    ResponseEntity<FlatPriceConversionResponse> create(@Valid @RequestBody CreateFlatPriceConversionRequest request);

    @Operation(summary = "Update a flat price conversion record")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = FlatPriceConversionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Record not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict – record was modified by another user",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    ResponseEntity<FlatPriceConversionResponse> update(
            @Parameter(description = "Record id", example = "42") @PathVariable Long id,
            @Valid @RequestBody UpdateFlatPriceConversionRequest request
    );

    @Operation(summary = "Delete a flat price conversion record")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Record not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Record id", example = "42") @PathVariable Long id
    );

    @Operation(summary = "Copy flat price conversion records", description = "Copies all records from one modelling scenario to another.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Copy successful"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Source modelling code not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Target modelling code already has records",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/copy")
    ResponseEntity<Void> copy(@Valid @RequestBody CopyFlatPriceConversionRequest request);

    @Operation(summary = "Clear all flat price conversion records for a modelling scenario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cleared"),
            @ApiResponse(responseCode = "404", description = "Modelling code not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/clear/{modellingCode}")
    ResponseEntity<Void> clear(
            @Parameter(description = "Modelling code to clear", example = "M1") @PathVariable String modellingCode
    );

    @Operation(summary = "Export flat price conversions as PDF")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF file"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Export failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<Resource> exportPdf(
            @Parameter(description = "Modelling code (required)", example = "P", required = true)
            @RequestParam String modellingCode,
            @Parameter(description = "Maturity code filter (optional)")
            @RequestParam(required = false) String maturity,
            @Parameter(description = "Sort code filter (optional)")
            @RequestParam(required = false) String sortCode,
            @Parameter(description = "Species code filter (optional)")
            @RequestParam(required = false) String species,
            @Parameter(description = "Grade code filter (optional)")
            @RequestParam(required = false) String grade
    );

    @Operation(summary = "Export flat price conversions as CSV")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV file"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Export failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping(value = "/export/csv", produces = "text/csv")
    ResponseEntity<Resource> exportCsv(
            @Parameter(description = "Modelling code (required)", example = "P", required = true)
            @RequestParam String modellingCode,
            @Parameter(description = "Maturity code filter (optional)")
            @RequestParam(required = false) String maturity,
            @Parameter(description = "Sort code filter (optional)")
            @RequestParam(required = false) String sortCode,
            @Parameter(description = "Species code filter (optional)")
            @RequestParam(required = false) String species,
            @Parameter(description = "Grade code filter (optional)")
            @RequestParam(required = false) String grade
    );
}
