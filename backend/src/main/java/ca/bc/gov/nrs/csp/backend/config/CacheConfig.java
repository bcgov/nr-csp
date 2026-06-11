package ca.bc.gov.nrs.csp.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String MATURITY_CODES = "maturityCodes";
    public static final String INVOICE_TYPES = "invoiceTypes";
    public static final String INVOICE_STATUSES = "invoiceStatuses";
    public static final String SUBMISSION_STATUSES = "submissionStatuses";
    public static final String SORT_CODES = "sortCodes";
    public static final String SPECIES_CODES = "speciesCodes";
    public static final String GRADE_CODES = "gradeCodes";
    public static final String GRADES_BY_SPECIES = "gradesBySpecies";
    public static final String MODELLING_CODES = "modellingCodes";
    public static final String FOB_CODES = "fobCodes";
    public static final String SPECIES_GRADE_COMBINATIONS = "speciesGradeCombinations";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                MATURITY_CODES,
                INVOICE_TYPES,
                INVOICE_STATUSES,
                SUBMISSION_STATUSES,
                SORT_CODES,
                SPECIES_CODES,
                GRADE_CODES,
                GRADES_BY_SPECIES,
                MODELLING_CODES,
                FOB_CODES,
                SPECIES_GRADE_COMBINATIONS
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(12))
                .maximumSize(1_000));
        return cacheManager;
    }
}
