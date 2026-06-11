package ca.bc.gov.nrs.csp.backend.controller.dto.lookup;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single valid (species, grade) pairing from THE.csp_species_grade_xref.")
public record SpeciesGradeCombinationResponse(
        @Schema(description = "Species code", example = "CE")
        String species,
        @Schema(description = "Grade code", example = "4")
        String grade
) {}
