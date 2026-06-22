package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.config.CacheConfig;
import ca.bc.gov.nrs.csp.backend.repository.LookupRepository;
import ca.bc.gov.nrs.csp.backend.service.model.LookupItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(classes = {
        CacheConfig.class,
        LookupService.class,
        LookupServiceCachingTest.TestConfig.class
})
class LookupServiceCachingTest {

    @Configuration
    static class TestConfig {
        @Bean
        LookupRepository lookupRepository() {
            return Mockito.mock(LookupRepository.class);
        }
    }

    @Autowired
    LookupService lookupService;

    @Autowired
    LookupRepository lookupRepository;

    @BeforeEach
    void setUp() {
        Mockito.reset(lookupRepository);
    }

    @Test
    void getMaturityCodes_usesCacheAfterFirstCall() {
        given(lookupRepository.findMaturityCodes()).willReturn(List.of(new LookupItem("O", "Old Growth")));

        lookupService.getMaturityCodes();
        lookupService.getMaturityCodes();

        verify(lookupRepository, times(1)).findMaturityCodes();
    }

    @Test
    void getGradesBySpecies_cachesPerSpeciesKey() {
        given(lookupRepository.findGradesBySpecies("FIR")).willReturn(List.of(new LookupItem("A", "Grade A")));
        given(lookupRepository.findGradesBySpecies("CED")).willReturn(List.of(new LookupItem("B", "Grade B")));

        lookupService.getGradesBySpecies("FIR");
        lookupService.getGradesBySpecies("FIR");
        lookupService.getGradesBySpecies("CED");

        verify(lookupRepository, times(1)).findGradesBySpecies("FIR");
        verify(lookupRepository, times(1)).findGradesBySpecies("CED");
    }
}
