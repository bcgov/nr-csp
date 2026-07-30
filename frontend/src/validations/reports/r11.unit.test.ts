import { describe, expect, it } from 'vitest';

import { validateR11 } from './r11';

const FROM = new Date(2026, 0, 1);
const TO = new Date(2026, 2, 31);

const keys = (result: ReturnType<typeof validateR11>) => result.errors.map((e) => e.messageKey);

describe('validateR11', () => {
  it('passes with a start date and a modeling code', () => {
    const result = validateR11(FROM, TO, 'P', '');
    expect(result.hasErrors()).toBe(false);
    expect(result.messages).toEqual([]);
  });

  it('requires a start date', () => {
    const result = validateR11(null, TO, 'P', '');
    expect(keys(result)).toContain('report.startdate.required.error');
  });

  it('requires a modeling code (report type)', () => {
    const result = validateR11(FROM, TO, '', '');
    expect(keys(result)).toContain('report.r11.reporttype.required.error');
  });

  it('rejects a non-numeric time frame', () => {
    const result = validateR11(FROM, TO, 'P', '3x');
    expect(keys(result)).toContain('report.timeframe.numeric.error');
  });

  it('accepts a numeric time frame', () => {
    const result = validateR11(FROM, null, 'P', '06');
    expect(result.hasErrors()).toBe(false);
  });

  it('rejects a start date after the end date', () => {
    const result = validateR11(TO, FROM, 'P', '');
    expect(keys(result)).toContain('report.daterange.order.error');
  });

  it('collects every violated rule at once', () => {
    const result = validateR11(null, null, '', 'nope');
    expect(keys(result)).toEqual([
      'report.startdate.required.error',
      'report.r11.reporttype.required.error',
      'report.timeframe.numeric.error',
    ]);
  });
});
