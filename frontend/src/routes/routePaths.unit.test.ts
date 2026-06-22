import { describe, it, expect } from 'vitest';

import { ROUTES } from './routePaths';

describe('ROUTES', () => {
  it('LANDING is "/"', () => {
    expect(ROUTES.LANDING).toBe('/');
  });

  it('SEARCH is "/search"', () => {
    expect(ROUTES.SEARCH).toBe('/search');
  });

  it('SORT_CODE is "/sort-code"', () => {
    expect(ROUTES.SORT_CODE).toBe('/sort-code');
  });

  it('FLAT_PRICE_CONVERSION is "/table-maintenance/flat-price-conversion"', () => {
    expect(ROUTES.FLAT_PRICE_CONVERSION).toBe('/table-maintenance/flat-price-conversion');
  });

  it('NOT_FOUND is "*"', () => {
    expect(ROUTES.NOT_FOUND).toBe('*');
  });

  it('has exactly the expected keys', () => {
    expect(Object.keys(ROUTES)).toEqual([
      'LANDING',
      'LOGOUT',
      'SEARCH',
      'INVOICE',
      'SORT_CODE',
      'FLAT_PRICE_CONVERSION',
      'R06_INVOICE_PRINT_OUT',
      'R07_RECONCILIATION',
      'R08_INVOICE_AUDIT',
      'R10_LOG_SALES_SPECIES',
      'R11_AMV',
      'R12_CFPA_EXTRACT',
      'R13_AD_HOC',
      'NOT_FOUND',
    ]);
  });

  it('all values are non-empty strings', () => {
    Object.values(ROUTES).forEach((v) => {
      expect(typeof v).toBe('string');
      expect(v.length).toBeGreaterThan(0);
    });
  });
});
