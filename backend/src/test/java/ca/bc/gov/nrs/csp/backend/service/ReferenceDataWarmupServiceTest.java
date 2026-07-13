package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReferenceDataWarmupServiceTest {

    @Mock
    LookupService lookupService;

    ReferenceDataWarmupService warmupService;

    @BeforeEach
    void setUp() {
        warmupService = new ReferenceDataWarmupService(lookupService);
    }

    @Test
    void run_warmsGlobalAndPerSpeciesLookups() {
        List<LookupItem> species = List.of(
                new LookupItem("FIR", "Fir"),
                new LookupItem("CED", "Cedar")
        );
        given(lookupService.getSpeciesCodes()).willReturn(species);

        warmupService.run(new DefaultApplicationArguments(new String[0]));

        verify(lookupService).getMaturityCodes();
        verify(lookupService).getInvoiceTypes();
        verify(lookupService).getInvoiceStatuses();
        verify(lookupService).getSubmissionStatuses();
        verify(lookupService).getSortCodes();
        verify(lookupService).getSpeciesCodes();
        verify(lookupService).getGradeCodes();
        verify(lookupService).getModellingCodes();
        verify(lookupService).getFobCodes();
        verify(lookupService).getSpeciesGradeCombinations();
        verify(lookupService).getGradesBySpecies("FIR");
        verify(lookupService).getGradesBySpecies("CED");
    }
}
