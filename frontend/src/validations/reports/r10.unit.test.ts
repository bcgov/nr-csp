import { describe, expect, it } from 'vitest';

import { validateR10 } from './r10';

const valid = () => ({
  dateFrom: new Date(2026, 0, 1),
  dateTo: new Date(2026, 2, 31),
  timeFrame: '',
  sellerName: '',
  sellerNumber: '',
  buyerName: '',
  buyerNumber: '',
});

const keys = (result: ReturnType<typeof validateR10>) => result.errors.map((e) => e.messageKey);

describe('validateR10', () => {
  it('passes for a valid date range with no client filters', () => {
    const result = validateR10(valid());
    expect(result.hasErrors()).toBe(false);
    expect(result.messages).toEqual([]);
  });

  it('requires a start date', () => {
    const result = validateR10({ ...valid(), dateFrom: null });
    expect(keys(result)).toContain('report.startdate.required.error');
  });

  it('requires an end date or a time frame', () => {
    const result = validateR10({ ...valid(), dateTo: null, timeFrame: '' });
    expect(keys(result)).toContain('report.r10.enddate.or.timeframe.required.error');
  });

  it('accepts a time frame in place of an end date', () => {
    const result = validateR10({ ...valid(), dateTo: null, timeFrame: '3' });
    expect(result.hasErrors()).toBe(false);
  });

  it('rejects a non-numeric time frame', () => {
    const result = validateR10({ ...valid(), timeFrame: 'abc' });
    expect(keys(result)).toContain('report.timeframe.numeric.error');
  });

  it('requires a seller selection when only a seller name is typed', () => {
    const result = validateR10({ ...valid(), sellerName: 'ACME' });
    expect(keys(result)).toContain('report.client.noselection.error');
    expect(result.errors[0].message).toBe('Select a valid seller client from the suggestion list.');
  });

  it('requires a buyer selection when only a buyer name is typed', () => {
    const result = validateR10({ ...valid(), buyerName: 'BUYCO' });
    expect(keys(result)).toContain('report.client.noselection.error');
    expect(result.errors[0].message).toBe('Select a valid buyer client from the suggestion list.');
  });

  it('accepts a typed name once its client number is resolved', () => {
    const result = validateR10({ ...valid(), sellerName: 'ACME', sellerNumber: '00012345' });
    expect(result.hasErrors()).toBe(false);
  });

  it('rejects a start date after the end date', () => {
    const result = validateR10({ ...valid(), dateFrom: new Date(2026, 5, 1), dateTo: new Date(2026, 0, 1) });
    expect(keys(result)).toContain('report.daterange.order.error');
  });
});
