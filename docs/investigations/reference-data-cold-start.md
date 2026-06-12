# Investigation: CSP Backend Reference Data Cold Start

## Summary

The CSP backend was not protecting the first user after deployment from reference-data load cost. Lookup endpoints and some report code paths queried Oracle on demand, there was no server-side cache, and there was no startup warm-up path. The frontend cached some of this data in-browser, but that did not help the first request into a fresh backend pod or concurrent users across sessions.

## Findings

- Startup had no lookup warm-up hook. `CspApplication` only started Spring, while the observed `@PostConstruct` hooks were limited to JWT and Jasper initialization.
- `LookupController` delegated every request to `LookupService`, and `LookupService` delegated every lookup call directly to `LookupRepository`.
- `LookupRepository` executed live SQL for maturity, invoice type/status, submission status, sort code, species, grade, species-grade combinations, modelling codes, FOB codes, and grades-by-species.
- `R13Service` also depended on lookup methods at runtime when report filters were left empty, so the cold path was not limited to dropdown endpoints.
- There was no backend Spring cache configuration before this change.

## Implemented Fix

- Added Spring caching via `ConcurrentMapCacheManager`.
- Marked reference-data methods in `LookupService` as `@Cacheable`.
- Added `ReferenceDataWarmupService` as an `ApplicationRunner` to preload all shared lookup tables at startup and pre-warm per-species grade lists.
- Added cache eviction on sort-code create, update, and delete so the `sortCodes` lookup cache does not become stale after admin changes.
- Added tests covering cache behavior and warm-up behavior.

## Result

After this change, backend pods load and cache reference data during startup instead of deferring that work to the first user-facing request. The first request after deploy should no longer pay the reference-data lookup penalty, and repeated backend requests now reuse in-memory cached lookup results.
