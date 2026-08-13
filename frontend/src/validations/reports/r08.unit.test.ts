import { describe, expect, it } from 'vitest';

import { validateR08 } from './r08';

const empty = () => ({
  dateFrom: null as Date | null,
  dateTo: null as Date | null,
  submissionNumber: '',
  submissionYearMonth: null as Date | null,
  timeFrame: '',
});

const keys = (result: ReturnType<typeof validateR08>) => result.errors.map((e) => e.messageKey);

describe('validateR08', () => {
  it('requires at least one filter', () => {
    const result = validateR08(empty());
    expect(keys(result)).toContain('report.r08.filter.required.error');
  });

  it('accepts a start date as the only filter', () => {
    const result = validateR08({ ...empty(), dateFrom: new Date(2026, 0, 1) });
    expect(result.hasErrors()).toBe(false);
  });

  it('accepts a submission number as the only filter', () => {
    const result = validateR08({ ...empty(), submissionNumber: '123' });
    expect(result.hasErrors()).toBe(false);
  });

  it('accepts a submission year-month as the only filter', () => {
    const result = validateR08({ ...empty(), submissionYearMonth: new Date(2026, 0, 1) });
    expect(result.hasErrors()).toBe(false);
  });

  it('rejects a non-numeric submission number', () => {
    const result = validateR08({ ...empty(), submissionNumber: '12ab' });
    expect(keys(result)).toContain('report.submissionnumber.numeric.error');
  });

  it('rejects a non-numeric time frame', () => {
    const result = validateR08({ ...empty(), dateFrom: new Date(2026, 0, 1), timeFrame: '3x' });
    expect(keys(result)).toContain('report.timeframe.numeric.error');
  });

  it('accepts a numeric time frame', () => {
    const result = validateR08({ ...empty(), dateFrom: new Date(2026, 0, 1), timeFrame: ' 6 ' });
    expect(result.hasErrors()).toBe(false);
  });

  it('ignores a whitespace-only time frame', () => {
    const result = validateR08({ ...empty(), dateFrom: new Date(2026, 0, 1), timeFrame: '   ' });
    expect(result.hasErrors()).toBe(false);
  });

  it('rejects a start date after the end date', () => {
    const result = validateR08({
      ...empty(),
      dateFrom: new Date(2026, 5, 1),
      dateTo: new Date(2026, 0, 1),
    });
    expect(keys(result)).toContain('report.daterange.order.error');
  });

  it('accepts a start date on or before the end date', () => {
    const result = validateR08({
      ...empty(),
      dateFrom: new Date(2026, 0, 1),
      dateTo: new Date(2026, 0, 1),
    });
    expect(result.hasErrors()).toBe(false);
  });

  it('collects every applicable error at once', () => {
    const result = validateR08({
      ...empty(),
      submissionNumber: 'abc',
      timeFrame: 'xyz',
    });
    expect(keys(result)).toEqual(['report.submissionnumber.numeric.error', 'report.timeframe.numeric.error']);
  });
});
