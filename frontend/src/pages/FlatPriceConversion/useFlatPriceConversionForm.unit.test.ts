import { describe, it, expect } from 'vitest';
import { validateFlatPriceConversionForm } from './useFlatPriceConversionForm';
import type { FormValues } from './useFlatPriceConversionForm';

const valid: FormValues = {
  species: 'FD',
  grade: 'U',
  sortCode: 'A',
  maturity: 'S',
  flatPriceConversion: '100',
  effectiveDate: '2024-01-01',
  expiryDate: '',
};

describe('validateFlatPriceConversionForm', () => {
  it('returns no errors for valid values', () => {
    expect(validateFlatPriceConversionForm(valid)).toEqual({});
  });

  it('requires species', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, species: '' });
    expect(errors.species).toMatch(/required/i);
  });

  it('requires grade', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, grade: '' });
    expect(errors.grade).toMatch(/required/i);
  });

  it('requires sortCode', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, sortCode: '' });
    expect(errors.sortCode).toMatch(/required/i);
  });

  it('requires maturity', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, maturity: '' });
    expect(errors.maturity).toMatch(/required/i);
  });

  it('requires flatPriceConversion', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, flatPriceConversion: '' });
    expect(errors.flatPriceConversion).toMatch(/required/i);
  });

  it('rejects flatPriceConversion below 1', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, flatPriceConversion: '0' });
    expect(errors.flatPriceConversion).toMatch(/1.*999|between/i);
  });

  it('rejects flatPriceConversion above 999', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, flatPriceConversion: '1000' });
    expect(errors.flatPriceConversion).toMatch(/1.*999|between/i);
  });

  it('rejects non-integer flatPriceConversion', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, flatPriceConversion: '1.5' });
    expect(errors.flatPriceConversion).toMatch(/1.*999|between/i);
  });

  it('rejects non-numeric flatPriceConversion', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, flatPriceConversion: 'abc' });
    expect(errors.flatPriceConversion).toMatch(/1.*999|between/i);
  });

  it('requires effectiveDate', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, effectiveDate: '' });
    expect(errors.effectiveDate).toMatch(/required/i);
  });

  it('rejects expiryDate before effectiveDate', () => {
    const errors = validateFlatPriceConversionForm({
      ...valid,
      effectiveDate: '2024-06-01',
      expiryDate: '2024-01-01',
    });
    expect(errors.expiryDate).toMatch(/before|after/i);
  });

  it('accepts expiryDate equal to effectiveDate', () => {
    const errors = validateFlatPriceConversionForm({
      ...valid,
      effectiveDate: '2024-01-01',
      expiryDate: '2024-01-01',
    });
    expect(errors.expiryDate).toBeUndefined();
  });

  it('accepts expiryDate after effectiveDate', () => {
    const errors = validateFlatPriceConversionForm({
      ...valid,
      effectiveDate: '2024-01-01',
      expiryDate: '2025-01-01',
    });
    expect(errors.expiryDate).toBeUndefined();
  });

  it('accepts empty expiryDate', () => {
    const errors = validateFlatPriceConversionForm({ ...valid, expiryDate: '' });
    expect(errors.expiryDate).toBeUndefined();
  });
});
