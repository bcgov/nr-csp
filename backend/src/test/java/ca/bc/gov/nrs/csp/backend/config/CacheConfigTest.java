package ca.bc.gov.nrs.csp.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new CacheConfig().cacheManager();
    }

    @Test
    void cacheManager_isCaffeineCacheManager() {
        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
    }

    @Test
    void cacheManager_configuresAllExpectedCaches() {
        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder(
                CacheConfig.MATURITY_CODES,
                CacheConfig.INVOICE_TYPES,
                CacheConfig.INVOICE_STATUSES,
                CacheConfig.SUBMISSION_STATUSES,
                CacheConfig.SORT_CODES,
                CacheConfig.SPECIES_CODES,
                CacheConfig.GRADE_CODES,
                CacheConfig.GRADES_BY_SPECIES,
                CacheConfig.MODELLING_CODES,
                CacheConfig.FOB_CODES,
                CacheConfig.SPECIES_GRADE_COMBINATIONS);
    }

    @Test
    void cacheManager_doesNotCreateCachesDynamically() {
        assertThat(cacheManager.getCache("someUnknownCache")).isNull();
    }

    @Test
    void caches_expireAfterTwelveHoursOfWrite() {
        Cache<Object, Object> nativeCache = nativeCache(CacheConfig.MATURITY_CODES);

        assertThat(nativeCache.policy().expireAfterWrite()).isPresent();
        assertThat(nativeCache.policy().expireAfterWrite().orElseThrow().getExpiresAfter())
                .isEqualTo(Duration.ofHours(12));
    }

    @Test
    void caches_haveMaximumSizeOfOneThousand() {
        Cache<Object, Object> nativeCache = nativeCache(CacheConfig.SPECIES_CODES);

        assertThat(nativeCache.policy().eviction()).isPresent();
        assertThat(nativeCache.policy().eviction().orElseThrow().getMaximum()).isEqualTo(1_000);
    }

    private Cache<Object, Object> nativeCache(String cacheName) {
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();
        return cache.getNativeCache();
    }
}
