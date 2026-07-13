import { describe, expect, it } from 'vitest';

import { validateR06 } from './r06';

const valid = () => ({
  dateFrom: new Date(2026, 0, 1),
  dateTo: new Date(2026, 2, 31),
  hasInvoiceNumbers: false,
  submissionId: '',
  sellerName: '',
  sellerNumber: '',
  buyerName: '',
  buyerNumber: '',
});

const keys = (result: ReturnType<typeof validateR06>) => result.errors.map((e) => e.messageKey);

describe('validateR06', () => {
  it('passes for a valid date range with no other filters', () => {
    const result = validateR06(valid());
    expect(result.hasErrors()).toBe(false);
    expect(result.messages).toEqual([]);
  });

  it('requires a start date when no invoice numbers are given', () => {
    const result = validateR06({ ...valid(), dateFrom: null });
    expect(keys(result)).toContain('report.r06.startdate.required.error');
  });

  it('requires an end date when no invoice numbers are given', () => {
    const result = validateR06({ ...valid(), dateTo: null });
    expect(keys(result)).toContain('report.r06.enddate.required.error');
  });

  it('does not require dates when invoice numbers are given', () => {
    const result = validateR06({ ...valid(), hasInvoiceNumbers: true, dateFrom: null, dateTo: null });
    expect(result.hasErrors()).toBe(false);
  });

  it('rejects a non-numeric submission id', () => {
    const result = validateR06({ ...valid(), submissionId: '12ab' });
    expect(keys(result)).toContain('report.submissionnumber.numeric.error');
  });

  it('accepts a numeric submission id', () => {
    const result = validateR06({ ...valid(), submissionId: ' 12345 ' });
    expect(result.hasErrors()).toBe(false);
  });

  it('requires a seller selection when only a seller name is typed', () => {
    const result = validateR06({ ...valid(), sellerName: 'ACME' });
    expect(keys(result)).toContain('report.client.noselection.error');
    expect(result.errors[0].message).toContain('seller');
  });

  it('requires a buyer selection when only a buyer name is typed', () => {
    const result = validateR06({ ...valid(), buyerName: 'BUYCO' });
    expect(keys(result)).toContain('report.client.noselection.error');
    expect(result.errors[0].message).toContain('buyer');
  });

  it('accepts typed names once their client numbers are resolved', () => {
    const result = validateR06({
      ...valid(),
      sellerName: 'ACME',
      sellerNumber: '00012345',
      buyerName: 'BUYCO',
      buyerNumber: '00054321',
    });
    expect(result.hasErrors()).toBe(false);
  });

  it('rejects a start date after the end date', () => {
    const result = validateR06({ ...valid(), dateFrom: new Date(2026, 5, 1), dateTo: new Date(2026, 0, 1) });
    expect(keys(result)).toContain('report.daterange.order.error');
  });

  it('collects every applicable error at once', () => {
    const result = validateR06({
      ...valid(),
      dateFrom: null,
      dateTo: null,
      submissionId: 'abc',
      sellerName: 'ACME',
      buyerName: 'BUYCO',
    });
    expect(keys(result)).toEqual([
      'report.r06.startdate.required.error',
      'report.r06.enddate.required.error',
      'report.submissionnumber.numeric.error',
      'report.client.noselection.error',
      'report.client.noselection.error',
    ]);
  });
});
