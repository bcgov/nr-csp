import { describe, it, expect } from 'vitest';

import { countSelectedColumns, validateR13 } from './r13';

// ── countSelectedColumns ───────────────────────────────────────────────────────

describe('countSelectedColumns', () => {
  it('returns 0 for an empty options object', () => {
    expect(countSelectedColumns({})).toBe(0);
  });

  it('counts only truthy values', () => {
    expect(
      countSelectedColumns({
        showSubmissionStatus: true,
        showSubmissionType: true,
        showInvoiceNumber: false,
        showVolume: undefined,
      }),
    ).toBe(2);
  });

  it('returns the total when all options are enabled', () => {
    const all = {
      showSubmissionStatus: true,
      showSubmissionNumber: true,
      showSubmissionMonthYear: true,
      showSubmissionType: true,
    };
    expect(countSelectedColumns(all)).toBe(4);
  });
});

// ── validateR13 ────────────────────────────────────────────────────────────────

const VALID_START = new Date('2026-01-15');
const VALID_END = new Date('2026-03-31');
const VALID_SHOW = { showSubmissionStatus: true, showSubmissionType: true };

describe('validateR13', () => {
  // ── Required fields ────────────────────────────────────────────────────
  it('returns an error when reportName is empty', () => {
    const result = validateR13('', VALID_START, VALID_END, '', VALID_SHOW);
    expect(result.hasErrors()).toBe(true);
    expect(result.errors.some((e) => e.messageKey === 'report.r13.reportname.required.error')).toBe(true);
  });

  it('returns an error when reportName is whitespace-only', () => {
    const result = validateR13('   ', VALID_START, VALID_END, '', VALID_SHOW);
    expect(result.hasErrors()).toBe(true);
    expect(result.errors.some((e) => e.messageKey === 'report.r13.reportname.required.error')).toBe(true);
  });

  it('returns an error when startDate is null', () => {
    const result = validateR13('Report', null, VALID_END, '', VALID_SHOW);
    expect(result.hasErrors()).toBe(true);
    expect(result.errors.some((e) => e.messageKey === 'report.startdate.required.error')).toBe(true);
  });

  // ── endDate / timeFrame — either is required ───────────────────────────
  it('returns an error when both endDate and timeFrame are absent', () => {
    const result = validateR13('Report', VALID_START, null, '', VALID_SHOW);
    expect(result.hasErrors()).toBe(true);
    expect(result.errors.some((e) => e.messageKey === 'report.r13.enddate.or.timeframe.required.error')).toBe(true);
  });

  it('passes when endDate is provided and timeFrame is empty', () => {
    const result = validateR13('Report', VALID_START, VALID_END, '', VALID_SHOW);
    expect(result.hasErrors()).toBe(false);
  });

  it('passes when timeFrame is provided and endDate is null', () => {
    const result = validateR13('Report', VALID_START, null, '03', VALID_SHOW);
    expect(result.hasErrors()).toBe(false);
  });

  // ── Date ordering ──────────────────────────────────────────────────────
  it('returns an error when startDate is after endDate', () => {
    const result = validateR13('Report', new Date('2026-04-01'), new Date('2026-01-01'), '', VALID_SHOW);
    expect(result.hasErrors()).toBe(true);
    expect(result.errors.some((e) => e.messageKey === 'report.daterange.order.error')).toBe(true);
  });

  it('passes when startDate equals endDate', () => {
    const same = new Date('2026-03-31');
    const result = validateR13('Report', same, same, '', VALID_SHOW);
    expect(result.hasErrors()).toBe(false);
  });

  // ── Show options minimum ───────────────────────────────────────────────
  it('returns an error when fewer than 2 columns are selected', () => {
    const result = validateR13('Report', VALID_START, VALID_END, '', { showSubmissionStatus: true });
    expect(result.hasErrors()).toBe(true);
    expect(result.errors.some((e) => e.messageKey === 'report.r13.showcolumns.minimum.error')).toBe(true);
  });

  it('returns an error when no columns are selected', () => {
    const result = validateR13('Report', VALID_START, VALID_END, '', {});
    expect(result.hasErrors()).toBe(true);
    expect(result.errors.some((e) => e.messageKey === 'report.r13.showcolumns.minimum.error')).toBe(true);
  });

  it('passes when exactly 2 columns are selected', () => {
    const result = validateR13('Report', VALID_START, VALID_END, '', VALID_SHOW);
    expect(result.hasErrors()).toBe(false);
  });

  // ── All rules evaluated (no early exit) ───────────────────────────────
  it('collects multiple independent errors in a single pass', () => {
    // Empty name AND no start date AND no end/timeframe — all 3 should be collected
    const result = validateR13('', null, null, '', VALID_SHOW);
    expect(result.errors.length).toBeGreaterThanOrEqual(3);
  });

  // ── Error messages resolve correctly ──────────────────────────────────
  it('includes a resolved human-readable message alongside the key', () => {
    const result = validateR13('', VALID_START, VALID_END, '', VALID_SHOW);
    const error = result.errors.find((e) => e.messageKey === 'report.r13.reportname.required.error');
    expect(error?.message).toBe('Report name is required.');
  });

  // ── Happy path ─────────────────────────────────────────────────────────
  it('returns no errors for a fully valid submission', () => {
    const result = validateR13('My Report', VALID_START, VALID_END, '', VALID_SHOW);
    expect(result.hasErrors()).toBe(false);
    expect(result.errors).toHaveLength(0);
  });

  it('returns no errors when using timeFrame instead of endDate', () => {
    const result = validateR13('My Report', VALID_START, null, '06', VALID_SHOW);
    expect(result.hasErrors()).toBe(false);
  });
});
