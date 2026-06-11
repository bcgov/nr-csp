package ca.bc.gov.nrs.csp.backend.controller.api;

import ca.bc.gov.nrs.csp.backend.controller.dto.error.ApiError;
import ca.bc.gov.nrs.csp.backend.controller.dto.search.ClientLocationResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.search.SearchResultResponse;
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
import java.util.List;

@Tag(name = "Search", description = "Invoice search and client lookup")
@RequestMapping("/api")
public interface SearchApi {

    @Operation(summary = "Search invoices", description = "Returns a page of invoices matching the supplied filter criteria. Supports server-side pagination, sorting, and a cross-field keyword filter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged search results",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SearchResultResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/search")
    ResponseEntity<Page<SearchResultResponse>> search(
            @Parameter(description = "Exact invoice date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invDate,
            @Parameter(description = "Date range start (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date range end (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Submitter client number (numeric, max 8 digits)") @RequestParam(required = false) String submitterClientNum,
            @Parameter(description = "Seller or buyer client number") @RequestParam(required = false) String sellerBuyerClientNum,
            @Parameter(description = "Seller or buyer location code") @RequestParam(required = false) String sellerBuyerLocNum,
            @Parameter(description = "True if the submitter is the seller") @RequestParam(required = false) Boolean sellerSubmitter,
            @Parameter(description = "Invoice number (partial match)") @RequestParam(required = false) String invNumber,
            @Parameter(description = "Invoice status code") @RequestParam(required = false) String invStatus,
            @Parameter(description = "Invoice type code") @RequestParam(required = false) String invType,
            @Parameter(description = "Log sale maturity code") @RequestParam(required = false) String maturity,
            @Parameter(description = "Cross-field keyword filter applied across visible columns") @RequestParam(required = false) String keyword,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(summary = "Client autocomplete", description = "Returns matching clients and locations for autocomplete. Provide either 'name' (partial match) or 'number' (exact, zero-padded to 8 digits).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching clients",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ClientLocationResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or blank search term",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/clients")
    ResponseEntity<List<ClientLocationResponse>> findClients(
            @Parameter(description = "Partial client name") @RequestParam(required = false) String name,
            @Parameter(description = "Client number (numeric, up to 8 digits)") @RequestParam(required = false) String number
    );

}
