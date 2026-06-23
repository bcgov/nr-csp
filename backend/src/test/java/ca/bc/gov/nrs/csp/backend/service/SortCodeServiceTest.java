package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.CreateSortCodeRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.UpdateSortCodeRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ConflictException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.repository.SortCodeRepository;
import ca.bc.gov.nrs.csp.backend.repository.ValidationLookupRepository;
import ca.bc.gov.nrs.csp.backend.service.model.SortCode;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SortCodeServiceTest {

    @Mock SortCodeRepository sortCodeRepository;
    @Mock ValidationLookupRepository validationLookupRepository;

    SortCodeService sortCodeService;

    @BeforeEach
    void setUp() {
        sortCodeService = new SortCodeService(sortCodeRepository, new CommonValidation(validationLookupRepository));
    }

    private SortCode sampleSortCode(String code) {
        return new SortCode(code, "Lumber - Cedar", LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(9999, Month.DECEMBER, 31), LocalDate.now());
    }

    // ---------------------------------------------------------------
    // listAll()
    // ---------------------------------------------------------------

    @Test
    void listAll_delegatesToRepository() {
        given(sortCodeRepository.findAll()).willReturn(List.of(sampleSortCode("A")));

        List<SortCode> results = sortCodeService.listAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).sortCode()).isEqualTo("A");
    }

    @Test
    void listAll_paged_delegatesToRepository() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<SortCode> page = new PageImpl<>(List.of(sampleSortCode("A")), pageable, 1);
        given(sortCodeRepository.findAll(pageable)).willReturn(page);

        Page<SortCode> result = sortCodeService.listAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).sortCode()).isEqualTo("A");
    }

    // ---------------------------------------------------------------
    // create()
    // ---------------------------------------------------------------

    @Test
    void create_uppercasesSortCode() {
        CreateSortCodeRequest request = new CreateSortCodeRequest(
                "a", "Lumber", LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(9999, Month.DECEMBER, 31));
        given(sortCodeRepository.existsByCode("A")).willReturn(false);
        given(sortCodeRepository.findByCode("A")).willReturn(Optional.of(sampleSortCode("A")));

        sortCodeService.create(request);

        verify(sortCodeRepository).insert(any(SortCode.class));
        verify(sortCodeRepository).findByCode("A");
    }

    @Test
    void create_duplicateSortCode_throwsConflict() {
        CreateSortCodeRequest request = new CreateSortCodeRequest(
                "A", "Lumber", LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(9999, Month.DECEMBER, 31));
        given(sortCodeRepository.existsByCode("A")).willReturn(true);

        assertThatThrownBy(() -> sortCodeService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("'A' already exists");
    }

    @Test
    void create_effectiveDateAfterExpiryDate_throwsBadRequest() {
        CreateSortCodeRequest request = new CreateSortCodeRequest(
                "A", "Lumber", LocalDate.of(2025, Month.JANUARY, 1), LocalDate.of(2024, Month.JANUARY, 1));
        given(sortCodeRepository.existsByCode("A")).willReturn(false);

        assertThatThrownBy(() -> sortCodeService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Effective date must not be after expiry date");
    }

    @Test
    void create_sameEffectiveAndExpiryDate_succeeds() {
        LocalDate same = LocalDate.of(2024, Month.JUNE, 1);
        CreateSortCodeRequest request = new CreateSortCodeRequest("A", "Lumber", same, same);
        given(sortCodeRepository.existsByCode("A")).willReturn(false);
        given(sortCodeRepository.findByCode("A")).willReturn(Optional.of(sampleSortCode("A")));

        sortCodeService.create(request);

        verify(sortCodeRepository).insert(any(SortCode.class));
    }

    // ---------------------------------------------------------------
    // update()
    // ---------------------------------------------------------------

    @Test
    void update_notFound_throwsResourceNotFound() {
        given(sortCodeRepository.existsByCode("Z")).willReturn(false);

        assertThatThrownBy(() -> sortCodeService.update("Z",
                new UpdateSortCodeRequest("desc", LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2030, Month.JANUARY, 1))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("'Z' not found");
    }

    @Test
    void update_effectiveDateAfterExpiryDate_throwsBadRequest() {
        given(sortCodeRepository.existsByCode("A")).willReturn(true);

        assertThatThrownBy(() -> sortCodeService.update("A",
                new UpdateSortCodeRequest("desc", LocalDate.of(2030, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Effective date must not be after expiry date");
    }

    @Test
    void update_uppercasesPathCode() {
        given(sortCodeRepository.existsByCode("A")).willReturn(true);
        given(sortCodeRepository.findByCode("A")).willReturn(Optional.of(sampleSortCode("A")));

        sortCodeService.update("a", new UpdateSortCodeRequest("New desc", LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(9999, Month.DECEMBER, 31)));

        verify(sortCodeRepository).update(eq("A"), any(SortCode.class));
    }

    @Test
    void update_validRequest_returnsUpdatedRecord() {
        SortCode updated = new SortCode("A", "Updated", LocalDate.of(2000, Month.JANUARY, 1), LocalDate.of(9999, Month.DECEMBER, 31), LocalDate.now());
        given(sortCodeRepository.existsByCode("A")).willReturn(true);
        given(sortCodeRepository.findByCode("A")).willReturn(Optional.of(updated));

        SortCode result = sortCodeService.update("A",
                new UpdateSortCodeRequest("Updated", LocalDate.of(2000, Month.JANUARY, 1), LocalDate.of(9999, Month.DECEMBER, 31)));

        assertThat(result.description()).isEqualTo("Updated");
    }

    // ---------------------------------------------------------------
    // delete()
    // ---------------------------------------------------------------

    @Test
    void delete_notFound_throwsResourceNotFound() {
        given(sortCodeRepository.existsByCode("Z")).willReturn(false);

        assertThatThrownBy(() -> sortCodeService.delete("Z"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("'Z' not found");
    }

    @Test
    void delete_found_delegatesToRepository() {
        given(sortCodeRepository.existsByCode("A")).willReturn(true);

        sortCodeService.delete("A");

        verify(sortCodeRepository).delete("A");
    }

    @Test
    void delete_uppercasesPathCode() {
        given(sortCodeRepository.existsByCode("A")).willReturn(true);

        sortCodeService.delete("a");

        verify(sortCodeRepository).delete("A");
    }
}
