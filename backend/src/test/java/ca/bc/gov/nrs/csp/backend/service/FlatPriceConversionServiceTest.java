package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.CopyFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.CreateFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.FlatPriceConversionDetails;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.FlatPriceConversionResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.flatpriceconversion.UpdateFlatPriceConversionRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ConflictException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.exception.UnprocessableEntityException;
import ca.bc.gov.nrs.csp.backend.repository.FlatPriceConversionRepository;
import ca.bc.gov.nrs.csp.backend.service.model.FlatPriceConversion;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlatPriceConversionServiceTest {

    @Mock
    FlatPriceConversionRepository repository;

    @Mock
    CommonValidation commonValidation;

    @InjectMocks
    FlatPriceConversionService service;

    // ---------------------------------------------------------------
    // Test data helpers
    // ---------------------------------------------------------------

    private static final LocalDate DATE = LocalDate.of(2020, 1, 1);
    private static final LocalDate LATER_DATE = LocalDate.of(2025, 12, 31);

    private FlatPriceConversionDetails sampleDetails() {
        return new FlatPriceConversionDetails("S", "FD", "U", "A", 100, DATE, null);
    }

    private FlatPriceConversion sampleDomain() {
        return new FlatPriceConversion(1L, "P", "S", "FD", "U", "A", 100, DATE, null, 1, "JSMITH", DATE, "JDOE", DATE);
    }

    // ---------------------------------------------------------------
    // search()
    // ---------------------------------------------------------------

    @Test
    void search_returnsMappedResults_whenRepositoryReturnsRecords() {
        given(repository.search("P", null, null, null, null))
                .willReturn(List.of(sampleDomain()));

        List<FlatPriceConversionResponse> results = service.search("P", null, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).modellingCode()).isEqualTo("P");
        assertThat(results.get(0).id()).isEqualTo(1L);
    }

    @Test
    void search_throwsBadRequest_whenModellingCodeIsBlank() {
        assertThatThrownBy(() -> service.search("  ", null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Modelling code is required");
    }

    @Test
    void search_throwsBadRequest_whenModellingCodeIsNull() {
        assertThatThrownBy(() -> service.search(null, null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Modelling code is required");
    }

    // ---------------------------------------------------------------
    // create()
    // ---------------------------------------------------------------

    @Test
    void create_happyPath_insertsAndReturnsCreatedRecord() {
        FlatPriceConversionDetails details = sampleDetails();
        CreateFlatPriceConversionRequest request = new CreateFlatPriceConversionRequest("P", details);

        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(true);
        given(commonValidation.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.of(42L));
        given(commonValidation.isValidSortCode("A", DATE)).willReturn(true);
        given(commonValidation.isValidMaturity("S", DATE)).willReturn(true);
        given(commonValidation.isDuplicateFlatPriceConversion("P", "A", 42L, "S", DATE, null)).willReturn(false);

        FlatPriceConversion inserted = sampleDomain();
        given(repository.search("P", "S", "A", "FD", "U")).willReturn(List.of(inserted));

        FlatPriceConversionResponse response = service.create(request);

        verify(repository).insert(any(FlatPriceConversion.class), eq(42L), anyString());
        assertThat(response).isNotNull();
        assertThat(response.modellingCode()).isEqualTo("P");
        assertThat(response.effectiveDate()).isEqualTo(DATE);
    }

    @Test
    void create_throwsBadRequest_whenDateRangeIsInvalid() {
        FlatPriceConversionDetails details = new FlatPriceConversionDetails("S", "FD", "U", "A", 100, LATER_DATE, DATE);
        CreateFlatPriceConversionRequest request = new CreateFlatPriceConversionRequest("P", details);

        given(commonValidation.isValidDateRange(LATER_DATE, DATE)).willReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Effective date must not be after expiry date");

        verify(repository, never()).insert(any(), anyLong(), anyString());
    }

    @Test
    void create_throwsUnprocessableEntity_whenSpeciesGradeCombinationIsInvalid() {
        CreateFlatPriceConversionRequest request = new CreateFlatPriceConversionRequest("P", sampleDetails());

        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Invalid species/grade combination");
    }

    @Test
    void create_throwsUnprocessableEntity_whenXrefIdNotFound() {
        CreateFlatPriceConversionRequest request = new CreateFlatPriceConversionRequest("P", sampleDetails());

        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(true);
        given(commonValidation.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Species/grade combination not found");
    }

    @Test
    void create_throwsUnprocessableEntity_whenSortCodeNotActiveOnEffectiveDate() {
        CreateFlatPriceConversionRequest request = new CreateFlatPriceConversionRequest("P", sampleDetails());

        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(true);
        given(commonValidation.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.of(42L));
        given(commonValidation.isValidSortCode("A", DATE)).willReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Sort code is not active on the effective date");
    }

    @Test
    void create_throwsUnprocessableEntity_whenMaturityNotActiveOnEffectiveDate() {
        CreateFlatPriceConversionRequest request = new CreateFlatPriceConversionRequest("P", sampleDetails());

        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(true);
        given(commonValidation.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.of(42L));
        given(commonValidation.isValidSortCode("A", DATE)).willReturn(true);
        given(commonValidation.isValidMaturity("S", DATE)).willReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Maturity code is not active on the effective date");
    }

    @Test
    void create_throwsConflict_whenDuplicateRecordExists() {
        CreateFlatPriceConversionRequest request = new CreateFlatPriceConversionRequest("P", sampleDetails());

        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(true);
        given(commonValidation.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.of(42L));
        given(commonValidation.isValidSortCode("A", DATE)).willReturn(true);
        given(commonValidation.isValidMaturity("S", DATE)).willReturn(true);
        given(commonValidation.isDuplicateFlatPriceConversion("P", "A", 42L, "S", DATE, null)).willReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    // ---------------------------------------------------------------
    // update()
    // ---------------------------------------------------------------

    @Test
    void update_happyPath_updatesAndReturnsRecord() {
        FlatPriceConversionDetails details = sampleDetails();
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, details);
        FlatPriceConversion existing = sampleDomain();
        FlatPriceConversion afterUpdate = new FlatPriceConversion(1L, "P", "S", "FD", "U", "A", 100, DATE, null, 2, "JSMITH", DATE, "system", DATE);

        given(repository.findById(1L))
                .willReturn(Optional.of(existing))
                .willReturn(Optional.of(afterUpdate));
        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(true);
        given(commonValidation.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.of(42L));
        given(commonValidation.isValidSortCode("A", DATE)).willReturn(true);
        given(commonValidation.isValidMaturity("S", DATE)).willReturn(true);
        given(commonValidation.isDuplicateFlatPriceConversion("P", "A", 42L, "S", DATE, 1L)).willReturn(false);

        FlatPriceConversionResponse response = service.update(1L, request);

        verify(repository).update(eq(1L), any(FlatPriceConversion.class), eq(42L), anyString());
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void update_throwsBadRequest_whenIdIsNull() {
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, sampleDetails());

        assertThatThrownBy(() -> service.update(null, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Id is required");
    }

    @Test
    void update_throwsBadRequest_whenIdIsZero() {
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, sampleDetails());

        assertThatThrownBy(() -> service.update(0L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Id is required");
    }

    @Test
    void update_throwsBadRequest_whenIdIsNegative() {
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, sampleDetails());

        assertThatThrownBy(() -> service.update(-1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Id is required");
    }

    @Test
    void update_throwsResourceNotFound_whenRecordNotFound() {
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, sampleDetails());
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_throwsConflict_whenRevisionCountMismatch() {
        FlatPriceConversion existing = sampleDomain(); // revisionCount = 1
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(99, sampleDetails()); // wrong count
        given(repository.findById(1L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("modified by another user");
    }

    @Test
    void update_throwsBadRequest_whenDateRangeIsInvalid() {
        FlatPriceConversionDetails details = new FlatPriceConversionDetails("S", "FD", "U", "A", 100, LATER_DATE, DATE);
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, details);
        FlatPriceConversion existing = sampleDomain();
        given(repository.findById(1L)).willReturn(Optional.of(existing));
        given(commonValidation.isValidDateRange(LATER_DATE, DATE)).willReturn(false);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Effective date must not be after expiry date");
    }

    @Test
    void update_throwsUnprocessableEntity_whenSpeciesGradeInvalid() {
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, sampleDetails());
        FlatPriceConversion existing = sampleDomain();
        given(repository.findById(1L)).willReturn(Optional.of(existing));
        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(false);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Invalid species/grade combination");
    }

    @Test
    void update_throwsUnprocessableEntity_whenXrefNotFound() {
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, sampleDetails());
        FlatPriceConversion existing = sampleDomain();
        given(repository.findById(1L)).willReturn(Optional.of(existing));
        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(true);
        given(commonValidation.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Species/grade combination not found");
    }

    @Test
    void update_throwsUnprocessableEntity_whenSortCodeNotActive() {
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, sampleDetails());
        FlatPriceConversion existing = sampleDomain();
        given(repository.findById(1L)).willReturn(Optional.of(existing));
        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(true);
        given(commonValidation.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.of(42L));
        given(commonValidation.isValidSortCode("A", DATE)).willReturn(false);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Sort code is not active on the effective date");
    }

    @Test
    void update_throwsUnprocessableEntity_whenMaturityNotActive() {
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, sampleDetails());
        FlatPriceConversion existing = sampleDomain();
        given(repository.findById(1L)).willReturn(Optional.of(existing));
        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(true);
        given(commonValidation.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.of(42L));
        given(commonValidation.isValidSortCode("A", DATE)).willReturn(true);
        given(commonValidation.isValidMaturity("S", DATE)).willReturn(false);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Maturity code is not active on the effective date");
    }

    @Test
    void update_throwsConflict_whenDuplicateExcludingSelf() {
        UpdateFlatPriceConversionRequest request = new UpdateFlatPriceConversionRequest(1, sampleDetails());
        FlatPriceConversion existing = sampleDomain();
        given(repository.findById(1L)).willReturn(Optional.of(existing));
        given(commonValidation.isValidDateRange(DATE, null)).willReturn(true);
        given(commonValidation.isValidSpeciesGradeCombination("FD", "U")).willReturn(true);
        given(commonValidation.findSpeciesGradeXrefId("FD", "U", DATE)).willReturn(Optional.of(42L));
        given(commonValidation.isValidSortCode("A", DATE)).willReturn(true);
        given(commonValidation.isValidMaturity("S", DATE)).willReturn(true);
        given(commonValidation.isDuplicateFlatPriceConversion("P", "A", 42L, "S", DATE, 1L)).willReturn(true);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    // ---------------------------------------------------------------
    // delete()
    // ---------------------------------------------------------------

    @Test
    void delete_happyPath_callsAuditDeleteThenDeleteById() {
        given(repository.findById(1L)).willReturn(Optional.of(sampleDomain()));

        service.delete(1L);

        verify(repository).auditDelete(eq(1L), anyString());
        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsBadRequest_whenIdIsNull() {
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Id is required");
    }

    @Test
    void delete_throwsBadRequest_whenIdIsZero() {
        assertThatThrownBy(() -> service.delete(0L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Id is required");
    }

    @Test
    void delete_throwsBadRequest_whenIdIsNegative() {
        assertThatThrownBy(() -> service.delete(-5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Id is required");
    }

    @Test
    void delete_throwsResourceNotFound_whenRecordNotFound() {
        given(repository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ---------------------------------------------------------------
    // copy()
    // ---------------------------------------------------------------

    @Test
    void copy_happyPath_callsRepositoryCopy() {
        CopyFlatPriceConversionRequest request = new CopyFlatPriceConversionRequest("P", "M1");
        given(repository.existsByModellingCode("P")).willReturn(true);
        given(repository.existsByModellingCode("M1")).willReturn(false);

        service.copy(request);

        verify(repository).copy(eq("P"), eq("M1"), anyString());
    }

    @Test
    void copy_throwsBadRequest_whenSourceModellingCodeIsBlank() {
        CopyFlatPriceConversionRequest request = new CopyFlatPriceConversionRequest("  ", "M1");

        assertThatThrownBy(() -> service.copy(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Source modelling code is required");
    }

    @Test
    void copy_throwsBadRequest_whenTargetModellingCodeIsBlank() {
        CopyFlatPriceConversionRequest request = new CopyFlatPriceConversionRequest("P", "  ");

        assertThatThrownBy(() -> service.copy(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Target modelling code is required");
    }

    @Test
    void copy_throwsBadRequest_whenSourceEqualsTarget() {
        CopyFlatPriceConversionRequest request = new CopyFlatPriceConversionRequest("P", "P");

        assertThatThrownBy(() -> service.copy(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Source and target modelling codes must differ");
    }

    @Test
    void copy_throwsResourceNotFound_whenSourceHasNoRecords() {
        CopyFlatPriceConversionRequest request = new CopyFlatPriceConversionRequest("P", "M1");
        given(repository.existsByModellingCode("P")).willReturn(false);

        assertThatThrownBy(() -> service.copy(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("P");
    }

    @Test
    void copy_throwsConflict_whenTargetAlreadyHasRecords() {
        CopyFlatPriceConversionRequest request = new CopyFlatPriceConversionRequest("P", "M1");
        given(repository.existsByModellingCode("P")).willReturn(true);
        given(repository.existsByModellingCode("M1")).willReturn(true);

        assertThatThrownBy(() -> service.copy(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("M1");
    }

    // ---------------------------------------------------------------
    // clearAll()
    // ---------------------------------------------------------------

    @Test
    void clearAll_happyPath_callsRepositoryClearAll() {
        service.clearAll("P");

        verify(repository).clearAll("P");
    }

    @Test
    void clearAll_throwsBadRequest_whenModellingCodeIsBlank() {
        assertThatThrownBy(() -> service.clearAll("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Modelling code is required");
    }

    @Test
    void clearAll_throwsBadRequest_whenModellingCodeIsNull() {
        assertThatThrownBy(() -> service.clearAll(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Modelling code is required");
    }
}
