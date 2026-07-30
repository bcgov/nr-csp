import { describe, expect, it } from 'vitest';

import { validateR12 } from './r12';

const FROM = new Date(2026, 0, 1);
const TO = new Date(2026, 2, 31);

const keys = (result: ReturnType<typeof validateR12>) => result.errors.map((e) => e.messageKey);

describe('validateR12', () => {
  it('passes when a report year alone is provided', () => {
    const result = validateR12('2026', null, null, '');
    expect(result.hasErrors()).toBe(false);
    expect(result.messages).toEqual([]);
  });

  it('passes when a date range is provided instead of a year', () => {
    const result = validateR12('', FROM, TO, '');
    expect(result.hasErrors()).toBe(false);
  });

  it('requires a start date when no year is provided', () => {
    const result = validateR12('', null, TO, '');
    expect(keys(result)).toContain('report.r12.startdate.required.error');
  });

  it('requires an end date or time frame when no year is provided', () => {
    const result = validateR12('', FROM, null, '');
    expect(keys(result)).toContain('report.r12.enddate.or.timeframe.required.error');
  });

  it('accepts a time frame in place of an end date', () => {
    const result = validateR12('', FROM, null, '3');
    expect(result.hasErrors()).toBe(false);
  });

  it('skips the date requirements entirely when a year is provided', () => {
    const result = validateR12('2026', null, null, '');
    expect(keys(result)).not.toContain('report.r12.startdate.required.error');
    expect(keys(result)).not.toContain('report.r12.enddate.or.timeframe.required.error');
  });

  it('rejects a non-numeric time frame even with a year', () => {
    const result = validateR12('2026', null, null, 'x');
    expect(keys(result)).toContain('report.timeframe.numeric.error');
  });

  it('rejects a start date after the end date', () => {
    const result = validateR12('', TO, FROM, '');
    expect(keys(result)).toContain('report.daterange.order.error');
  });
});
