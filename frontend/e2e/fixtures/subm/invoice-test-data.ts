import { daysAgo } from '../common/dates';

/**
 * SUBM domain (manual invoice submission) — known-good data pinned from the SEEDED database and
 * from the app's own reference-data endpoints. Nothing here is invented: every code and client was
 * read back from the running stack, with the discovery command in the comment above it.
 *
 * Seed image: ghcr.io/cgi-bc/nr-mof-oracle-csp-real-test-data-seeded:2026-09-23
 *
 * If the image is rebuilt from a fresh extract these may move. `preflight/anchors.setup.ts` asserts
 * the reference-table counts, and the SUBM anchors below are re-asserted there too, so a drift
 * fails fast with one clear message instead of a dozen mid-scenario reds.
 */

/**
 * ---------------------------------------------------------------------------
 * CLIENTS ARE RESOLVED AT RUNTIME, NOT PINNED HERE
 * ---------------------------------------------------------------------------
 * This repository is PUBLIC. Real forest-client names and numbers are production records, so none
 * are committed — the suite looks up whatever the seeded database holds and asserts against what it
 * resolved, rather than against literals. That is also a slightly stronger test: it cannot pass by
 * agreeing with a stale hardcoded expectation.
 *
 * `CLIENT_SEARCH_TERM` is a generic corporate suffix, not a client identity. It is deliberately
 * long enough to clear the autocomplete's own gate — `AutoCompleteInput` only queries at
 * `debounce.length >= 2`.
 *
 * WHAT THE VALIDATORS REQUIRE of the pair, and why any two rows from this lookup satisfy it:
 *   * each must be a real (client number, location) combination —
 *     `InvoiceValidator.checkSubmiterClient` rejects anything else. `/api/clients` returns
 *     client+location rows, so every row IS such a combination;
 *   * the two must DIFFER — `isSameSellerAndBuyer` rejects an invoice whose submitter and other
 *     party share a number and location. `pickTwoDistinctClients` guarantees that.
 *
 * Verified end to end: resolving two clients this way and POSTing an invoice returns 200 in DFT
 * with only the expected submit-reminder warning.
 */
export const CLIENT_SEARCH_TERM = 'LTD';

/** One client+location row as `/api/clients` returns it. */
export type ResolvedClient = {
  clientNumber: string;
  clientName: string;
  clientLocnCode: string;
};

/**
 * Pick two DISTINCT clients from a `/api/clients` result.
 *
 * Distinct by client NUMBER, not by row: the lookup returns one row per client *location*, so the
 * same client can appear several times and taking rows 0 and 1 blindly could hand back the same
 * client twice — which the submitter-vs-other-party rule then rejects.
 *
 * Pure and order-preserving, so the choice is deterministic for a given database.
 */
export const pickTwoDistinctClients = (rows: ResolvedClient[]): [ResolvedClient, ResolvedClient] => {
  const byNumber = new Map<string, ResolvedClient>();
  for (const row of rows) {
    if (!byNumber.has(row.clientNumber)) byNumber.set(row.clientNumber, row);
  }
  const distinct = [...byNumber.values()];
  if (distinct.length < 2) {
    throw new Error(
      `[test data] "${CLIENT_SEARCH_TERM}" resolved only ${distinct.length} distinct client(s); the ` +
        `invoice journey needs two (submitter and other party must differ). Widen ` +
        `CLIENT_SEARCH_TERM in fixtures/subm/invoice-test-data.ts.`,
    );
  }
  return [distinct[0], distinct[1]];
};

/**
 * Header reference codes, all read from the app's own lookup endpoints.
 *
 * Provenance:
 *   curl -s http://localhost:8080/api/lookup/type      -> ADJ, CAN, PUR, SAL
 *   curl -s http://localhost:8080/api/lookup/maturity  -> C, E, M, O, S
 *   curl -s http://localhost:8080/api/lookup/fob       -> 145 rows, incl. {"code":"ALBE","description":"Alberni"}
 *
 * INVOICE TYPE vs SUBMITTED BY — the one pairing rule that decides this journey's data:
 * `InvoiceValidator.checkSenderBuyerForInvoiceType` rejects Seller+PUR and Buyer+SAL with
 * `invoice.type.invalid.submitter`. A PURCHASE invoice is therefore submitted by the BUYER.
 * (The legacy Gherkin's happy path pairs Purchase with Seller, which its own UC-SUBM-001-S06
 * exception slice says must fail — see defects.md, SPEC-001.)
 */
export const invoiceHeaderCodes = {
  /** csp_invoice_type_code — "Purchase". */
  purchaseTypeCode: 'PUR',
  purchaseTypeLabel: 'Purchase',
  /** The only Submitted-by value valid with a Purchase invoice. */
  submittedBy: 'Buyer',
  /** csp_maturity_code — "Old Growth". Also the page's default for a new invoice. */
  maturityCode: 'O',
  maturityLabel: 'Old Growth',
  /** csp_fob_code — "Alberni". */
  fobCode: 'ALBE',
} as const;

/**
 * The single line item this journey adds.
 *
 * Provenance:
 *   curl -s http://localhost:8080/api/lookup/sort-code
 *     -> {"code":"S","description":"Standard Sawlogs"}      (secondary sort)
 *   curl -s http://localhost:8080/api/lookup/species-grade-combinations
 *     -> includes {"species":"BA","grade":"D"}              (an ACTIVE pair in csp_species_grade_xref)
 *
 * The species/grade pair must be in that xref or `InvoiceLineValidator.checkSpeciesGradeCombination`
 * rejects it with `invoice.species.grade.combination.error`. The Add New Item modal also filters the
 * Grade list against the chosen Species, so an invalid pair is not even selectable in the UI.
 *
 * `expectedAmount` is what the BACKEND computes and stores — volume x price, HALF_UP to 2dp
 * (`InvoiceTotalsRuleSet.calculate` / `LineAmount.compute`). It is asserted on the API read-back,
 * not merely previewed in the modal.
 */
export const singleLineItem = {
  secondarySortCode: 'S',
  secondarySortLabel: 'S - Standard Sawlogs',
  secondarySortDescription: 'Standard Sawlogs',
  species: 'BA',
  grade: 'D',
  pieces: 10,
  volume: '100.000',
  price: '50.00',
  expectedAmount: 5000,
  /** As the group summary and invoice-totals row render them (utils/format.ts). */
  rendered: {
    pieces: '10',
    volume: '100.000',
    amount: '$5,000.00',
  },
} as const;

/**
 * Invoice date — TODAY-RELATIVE, never a literal.
 *
 * `InvoiceValidator.checkInvoiceDateNotFuture` rejects a future date, and
 * `commonValidation.isValidInvoiceType` / `isValidMaturity` check the code was active ON THIS DATE,
 * so a hardcoded date would eventually age out of a reference window or drift into the future.
 * Yesterday is safely in the past in every timezone the suite runs in.
 */
export const invoiceDate = (): string => daysAgo(1);

/**
 * A fresh invoice number, unique per scenario.
 *
 * PARALLEL SAFETY: the suite runs `fullyParallel`, and a manual Sale/Purchase invoice that reuses
 * another invoice's number raises the `invoice.number.duplicate.same.type.warning` banner
 * (`InvoiceValidator.checkForInvoiceNumDuplicate`). Each scenario therefore mints its OWN number
 * and cleans it up, rather than sharing a fixture row.
 *
 * FORMAT CONSTRAINTS (`CreateInvoiceRequest.invNumber`): `@Size(max = 15)` and
 * `@Pattern("^[A-Z0-9-]+$")`. The "E2E-" prefix makes any row this suite leaks identifiable in the
 * seeded DB. Suffix = last 8 digits of the epoch ms plus 3 random digits = 15 characters exactly.
 */
export const uniqueInvoiceNumber = (): string => {
  const stamp = String(Date.now()).slice(-8);
  const salt = String(Math.floor(Math.random() * 1000)).padStart(3, '0');
  return `E2E-${stamp}${salt}`;
};

/**
 * A boom number for the journey's invoice.
 *
 * REQUIRED, despite the UI not marking it so: `InvoiceValidator.checkSourceDocumentRefs` rejects a
 * save with none of Boom Numbers / Timber Marks / Weigh Slips populated
 * (`invoice.oneofthe.boom.timber.wiegh.requiered.error`). The page's own `requiredFieldsFilled`
 * gate does NOT include these three, so Save is clickable without them and the rule only fires
 * server-side — logged as BUG-001 in defects.md.
 *
 * UNIQUE PER SCENARIO, for the same parallel-safety reason as the invoice number: reusing a boom
 * number that another live invoice already carries raises
 * `invoice.boomnumber.duplicate.warning` ("The following Boom Numbers are duplicate : ...") from
 * `InvoiceValidator.checkBoomNumberForDuplicates`, which would add an unexpected banner. The random
 * salt matters — two workers starting in the same millisecond would otherwise collide.
 *
 * Max length 20 (`ConstantsCode.MAXTOKENLENGTHFORBOOMNUMBERS`); uppercase because TagInput is
 * configured `uppercase` on this field.
 */
export const boomNumber = (): string => {
  const stamp = String(Date.now()).slice(-8);
  const salt = String(Math.floor(Math.random() * 1000)).padStart(3, '0');
  return `E2EBOOM${stamp}${salt}`;
};

/**
 * SUBM anchors the preflight re-checks, so a re-extracted DB fails fast HERE rather than mid-journey.
 */
export const submAnchors = {
  /** Clients are resolved at runtime (see CLIENT_SEARCH_TERM), so only the term is an anchor. */
  clientSearchTerm: CLIENT_SEARCH_TERM,
  invoiceTypeCode: invoiceHeaderCodes.purchaseTypeCode,
  maturityCode: invoiceHeaderCodes.maturityCode,
  fobCode: invoiceHeaderCodes.fobCode,
  sortCode: singleLineItem.secondarySortCode,
  speciesGradePair: { species: singleLineItem.species, grade: singleLineItem.grade },
} as const;
