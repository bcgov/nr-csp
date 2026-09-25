import { mergeTests } from '@playwright/test';
import { createBdd } from 'playwright-bdd';

import { globalTest } from './global';
import { exampleTest } from './example';
import { inboxTest } from './inbox';

/**
 * ============================================================================
 * SINGLE COMPOSITION ROOT for the whole BDD suite.
 * ============================================================================
 *
 * Each domain owns its own fixtures file (`./<domain>`); this merges them into the single `test` that
 * `createBdd` binds Given/When/Then to. Step files import Given/When/Then/expect FROM HERE, never from
 * 'playwright-bdd' or '@playwright/test' directly — that is what guarantees every step definition
 * receives the same declared fixtures. We build on playwright-bdd's `test` (not @playwright/test's) so
 * the generated tests keep BDD metadata; all standard Playwright fixtures (page, request, …) still
 * apply. Every fixture stays LAZY (Playwright only instantiates the ones a scenario touches) and
 * per-scenario (no shared mutable module state), so scenarios stay order-independent and parallel-safe.
 *
 * WHY THE SPLIT: this started as ONE file holding every domain's fixtures. That makes the composition
 * root a merge-conflict hotspot the moment two people add coverage for two different domains at once —
 * a problem a sibling NR suite hit for real once its equivalent file reached ~1,460 lines. Splitting
 * from the start keeps parallel domain work cheap.
 *
 * ---- HOW TO ADD A DOMAIN ---------------------------------------------------
 *   1. Copy `./example.ts` to `./<domain>.ts` and rename its exports
 *      (`exampleTest` -> `<domain>Test`, `ExampleFixtures` -> `<Domain>Fixtures`).
 *   2. Add `<domain>Test` to `mergeTests` below, and re-export any type a step file needs.
 *   3. Put a fixture in `./global` ONLY if more than one domain uses it. A `world` field goes in the
 *      `World` union there, grouped under its domain's comment.
 * Two authors adding two domains then touch one shared line each (the `mergeTests` list) instead of
 * appending to the same 200-line body.
 */
export const test = mergeTests(globalTest, exampleTest, inboxTest);

export type { World } from './global';
export type { InboxFixtures } from './inbox';
// TODO(author): re-export the types your step files need, e.g.
//   export type { ExampleCleanup } from './example';

export const { Given, When, Then } = createBdd(test);
export { expect } from '@playwright/test';
