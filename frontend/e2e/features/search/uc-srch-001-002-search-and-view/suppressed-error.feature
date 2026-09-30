# BUG-001 — a validation error the app computes but shows nobody.
#
# THIS SCENARIO IS EXPECTED TO FAIL, and is tagged @discovered-divergence so it can be filtered out
# of a "is anything newly broken?" run:
#
#     npm run bddgen && npx playwright test --project=chromium --grep-invert @discovered-divergence
#
# It is deliberately NOT skipped, weakened or inverted. The red IS the tracking signal, and it will
# turn green on its own once the app surfaces the message.
#
# WHAT HAPPENS. Opening the pinned APPROVED invoice, the API returns TWO validation errors:
#   GET /api/invoices/200388 -> errors[]
#     invoice.oneofthe.boom.timber.wiegh.requiered.error  "One of Boom Number, Timber Mark or Weigh
#                                                          Slip must have a value."          -> SHOWN
#     invoice.type.invalid.submitter                      "The Invoice submitted by Seller cannot
#                                                          be type PUR."                     -> NOT SHOWN
#
# WHY. `routeServerErrors` sends a message whose key is in MESSAGE_KEY_TO_FIELD to that field as an
# inline error, and everything else to the page banner. `invoice.type.invalid.submitter` maps to
# `invType` — but an APPROVED invoice's header fields are disabled (`canEdit` is false outside
# DFT/PRO/UNA), and Carbon does not render a disabled field's `invalidText`. Measured on this
# record: the Invoice type button is `disabled`, has no invalid class and no `aria-invalid`, and
# the page contains ZERO `.cds--form-requirement` elements — while both banners render fine.
#
# IMPACT (plain language): a reviewer opening a locked invoice is told about some of its problems
# and not others, with nothing to indicate anything is missing. It affects every field-mapped
# validation error on any APPROVED, REJECTED or CANCELLED invoice — not just this one.
#
# NOTE this record is itself interesting: it is a real, APPROVED, historical invoice that is a
# Purchase submitted by the Seller — exactly what the legacy UC-SUBM-001-S01 happy path instructs,
# and exactly what today's validator forbids. See defects.md.

@p1 @UC-SRCH-002 @S01 @discovered-divergence
Feature: Invoice view — validation errors on a locked invoice

  As an Invoice Searcher reviewing a historical record
  I want to be told every validation problem the system found with it
  So that I am not misled into thinking the record is sound

  Background:
    Given I am on the invoice search screen as a CSP ADMIN

  Scenario: Every validation error the system found is shown to the reader
    When I search by the target invoice number
    Then exactly the target invoice is returned
    When I open the target invoice from the results
    Then the page reports the validation problems with this legacy record
    # FAILS TODAY — the backend returned this error for the record, but it is routed to a disabled
    # field and Carbon renders nothing for it.
    And the invoice type validation error is visible somewhere on the page
