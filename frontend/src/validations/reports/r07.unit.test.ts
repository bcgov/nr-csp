import { describe, expect, it } from 'vitest';

import { validateR07 } from './r07';

const empty = () => ({
  reportingYearMonth: null as Date | null,
  dateFrom: null as Date | null,
  dateTo: null as Date | null,
  sellerNumber: '',
  buyerNumber: '',
  submissionNumber: '',
  timeFrame: '',
});

const keys = (result: ReturnType<typeof validateR07>) => result.errors.map((e) => e.messageKey);

describe('validateR07', () => {
  it('requires at least one filter', () => {
    const result = validateR07(empty());
    expect(keys(result)).toContain('report.r07.filter.required.error');
  });

  it('accepts a reporting year-month as the only filter', () => {
    const result = validateR07({ ...empty(), reportingYearMonth: new Date(2026, 0, 1) });
    expect(result.hasErrors()).toBe(false);
  });

  it('accepts a start date as the only filter', () => {
    const result = validateR07({ ...empty(), dateFrom: new Date(2026, 0, 1) });
    expect(result.hasErrors()).toBe(false);
  });

  it('accepts a seller number as the only filter', () => {
    const result = validateR07({ ...empty(), sellerNumber: '00012345' });
    expect(result.hasErrors()).toBe(false);
  });

  it('accepts a buyer number as the only filter', () => {
    const result = validateR07({ ...empty(), buyerNumber: '00054321' });
    expect(result.hasErrors()).toBe(false);
  });

  it('accepts a submission number as the only filter', () => {
    const result = validateR07({ ...empty(), submissionNumber: '123' });
    expect(result.hasErrors()).toBe(false);
  });

  it('rejects a non-numeric submission number', () => {
    const result = validateR07({ ...empty(), submissionNumber: '12ab' });
    expect(keys(result)).toContain('report.submissionnumber.numeric.error');
  });

  it('rejects a non-numeric time frame', () => {
    const result = validateR07({ ...empty(), dateFrom: new Date(2026, 0, 1), timeFrame: 'abc' });
    expect(keys(result)).toContain('report.timeframe.numeric.error');
  });

  it('accepts a numeric time frame', () => {
    const result = validateR07({ ...empty(), dateFrom: new Date(2026, 0, 1), timeFrame: ' 3 ' });
    expect(result.hasErrors()).toBe(false);
  });

  it('rejects a start date after the end date', () => {
    const result = validateR07({
      ...empty(),
      dateFrom: new Date(2026, 5, 1),
      dateTo: new Date(2026, 0, 1),
    });
    expect(keys(result)).toContain('report.daterange.order.error');
  });

  it('accepts a start date on or before the end date', () => {
    const result = validateR07({
      ...empty(),
      dateFrom: new Date(2026, 0, 1),
      dateTo: new Date(2026, 0, 1),
    });
    expect(result.hasErrors()).toBe(false);
  });

  it('skips the date order check when the end date is missing', () => {
    const result = validateR07({ ...empty(), dateFrom: new Date(2026, 5, 1) });
    expect(keys(result)).not.toContain('report.daterange.order.error');
  });
});
