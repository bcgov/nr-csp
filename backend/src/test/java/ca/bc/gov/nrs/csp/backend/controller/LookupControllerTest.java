package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.lookup.LookupItemResponse;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.repository.LookupRepository;
import ca.bc.gov.nrs.csp.backend.service.LookupService;
import ca.bc.gov.nrs.csp.backend.service.mapper.LookupMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LookupControllerTest {

    @Mock LookupService lookupService;
    @Mock LookupMapper lookupMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LookupController(lookupService, lookupMapper))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .build();
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/maturity
    // ---------------------------------------------------------------

    @Test
    void getMaturityCodes_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("O", "Old Growth"),
                new LookupItemResponse("S", "Second Growth")
        );
        given(lookupService.getMaturityCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/maturity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("O"))
                .andExpect(jsonPath("$[0].description").value("Old Growth"))
                .andExpect(jsonPath("$[1].code").value("S"));
    }

    @Test
    void getMaturityCodes_returns200_withEmptyList() throws Exception {
        given(lookupService.getMaturityCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/maturity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getMaturityCodes_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getMaturityCodes()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/maturity"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/type
    // ---------------------------------------------------------------

    @Test
    void getInvoiceTypes_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("SAL", "Sales"),
                new LookupItemResponse("PUR", "Purchase")
        );
        given(lookupService.getInvoiceTypes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SAL"))
                .andExpect(jsonPath("$[0].description").value("Sales"))
                .andExpect(jsonPath("$[1].code").value("PUR"));
    }

    @Test
    void getInvoiceTypes_returns200_withEmptyList() throws Exception {
        given(lookupService.getInvoiceTypes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getInvoiceTypes_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getInvoiceTypes()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/type"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/status
    // ---------------------------------------------------------------

    @Test
    void getInvoiceStatuses_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("APP", "Approved"),
                new LookupItemResponse("REJ", "Rejected")
        );
        given(lookupService.getInvoiceStatuses()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("APP"))
                .andExpect(jsonPath("$[0].description").value("Approved"))
                .andExpect(jsonPath("$[1].code").value("REJ"));
    }

    @Test
    void getInvoiceStatuses_returns200_withEmptyList() throws Exception {
        given(lookupService.getInvoiceStatuses()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getInvoiceStatuses_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getInvoiceStatuses()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/submission-status
    // ---------------------------------------------------------------

    @Test
    void getSubmissionStatuses_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("COM", "Complete"),
                new LookupItemResponse("INB", "Inbox")
        );
        given(lookupService.getSubmissionStatuses()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/submission-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("COM"))
                .andExpect(jsonPath("$[0].description").value("Complete"))
                .andExpect(jsonPath("$[1].code").value("INB"));
    }

    @Test
    void getSubmissionStatuses_returns200_withEmptyList() throws Exception {
        given(lookupService.getSubmissionStatuses()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/submission-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSubmissionStatuses_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getSubmissionStatuses()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/submission-status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/sort-code
    // ---------------------------------------------------------------

    @Test
    void getSortCodes_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("01", "Sort 01"),
                new LookupItemResponse("02", "Sort 02")
        );
        given(lookupService.getSortCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/sort-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("01"))
                .andExpect(jsonPath("$[0].description").value("Sort 01"))
                .andExpect(jsonPath("$[1].code").value("02"));
    }

    @Test
    void getSortCodes_returns200_withEmptyList() throws Exception {
        given(lookupService.getSortCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/sort-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSortCodes_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getSortCodes()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/sort-code"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/species
    // ---------------------------------------------------------------

    @Test
    void getSpeciesCodes_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("CE", "Cedar"),
                new LookupItemResponse("FI", "Fir")
        );
        given(lookupService.getSpeciesCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CE"))
                .andExpect(jsonPath("$[0].description").value("Cedar"))
                .andExpect(jsonPath("$[1].code").value("FI"));
    }

    @Test
    void getSpeciesCodes_returns200_withEmptyList() throws Exception {
        given(lookupService.getSpeciesCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSpeciesCodes_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getSpeciesCodes()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/species"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/grade
    // ---------------------------------------------------------------

    @Test
    void getGradeCodes_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("4", "Grade 4"),
                new LookupItemResponse("6", "Grade 6")
        );
        given(lookupService.getGradeCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/grade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("4"))
                .andExpect(jsonPath("$[0].description").value("Grade 4"))
                .andExpect(jsonPath("$[1].code").value("6"));
    }

    @Test
    void getGradeCodes_returns200_withEmptyList() throws Exception {
        given(lookupService.getGradeCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/grade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getGradeCodes_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getGradeCodes()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/grade"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void getGradesBySpecies_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("A", "Grade A"),
                new LookupItemResponse("B", "Grade B")
        );
        given(lookupService.getGradesBySpecies("FD")).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/grade-by-species/FD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("A"))
                .andExpect(jsonPath("$[0].description").value("Grade A"))
                .andExpect(jsonPath("$[1].code").value("B"));
    }

    @Test
    void getGradesBySpecies_returns200_withEmptyList() throws Exception {
        given(lookupService.getGradesBySpecies("ZZ")).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/grade-by-species/ZZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getGradesBySpecies_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getGradesBySpecies("FD")).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/grade-by-species/FD"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/modelling-code
    // ---------------------------------------------------------------

    @Test
    void getModellingCodes_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("M1", "Model One"),
                new LookupItemResponse("M2", "Model Two")
        );
        given(lookupService.getModellingCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/modelling-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("M1"))
                .andExpect(jsonPath("$[0].description").value("Model One"))
                .andExpect(jsonPath("$[1].code").value("M2"));
    }

    @Test
    void getModellingCodes_returns200_withEmptyList() throws Exception {
        given(lookupService.getModellingCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/modelling-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getModellingCodes_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getModellingCodes()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/modelling-code"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/fob
    // ---------------------------------------------------------------

    @Test
    void getFobCodes_returns200_withList() throws Exception {
        List<LookupItemResponse> response = List.of(
                new LookupItemResponse("BW", "Barge or Water"),
                new LookupItemResponse("TR", "Truck")
        );
        given(lookupService.getFobCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(response);

        mockMvc.perform(get("/api/lookup/fob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BW"))
                .andExpect(jsonPath("$[0].description").value("Barge or Water"))
                .andExpect(jsonPath("$[1].code").value("TR"));
    }

    @Test
    void getFobCodes_returns200_withEmptyList() throws Exception {
        given(lookupService.getFobCodes()).willReturn(List.of());
        given(lookupMapper.toResponseList(any())).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/fob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getFobCodes_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getFobCodes()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/fob"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------------
    // GET /api/lookup/species-grade-combinations
    // ---------------------------------------------------------------

    @Test
    void getSpeciesGradeCombinations_returns200_withList() throws Exception {
        given(lookupService.getSpeciesGradeCombinations()).willReturn(List.of(
                new LookupRepository.SpeciesGradeCombo("CE", "4"),
                new LookupRepository.SpeciesGradeCombo("FI", "6")
        ));

        mockMvc.perform(get("/api/lookup/species-grade-combinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].species").value("CE"))
                .andExpect(jsonPath("$[0].grade").value("4"))
                .andExpect(jsonPath("$[1].species").value("FI"))
                .andExpect(jsonPath("$[1].grade").value("6"));
    }

    @Test
    void getSpeciesGradeCombinations_returns200_withEmptyList() throws Exception {
        given(lookupService.getSpeciesGradeCombinations()).willReturn(List.of());

        mockMvc.perform(get("/api/lookup/species-grade-combinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSpeciesGradeCombinations_returns500_whenServiceThrows() throws Exception {
        given(lookupService.getSpeciesGradeCombinations()).willThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/lookup/species-grade-combinations"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

}
