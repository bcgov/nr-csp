import { describe, expect, it } from 'vitest';

import { validate, validateLineItem, type InvoiceFieldValues, type LineItemFieldValues } from './invoice';

const validInvoice = (): InvoiceFieldValues => ({
  invNumber: 'INV-100',
  invDate: '2026-05-19',
  invType: 'SAL',
  submittedBy: 'Seller',
  submitterLocation: '00',
  otherClientLocation: '',
});

const validLineItem = (): LineItemFieldValues => ({
  pieces: '10',
  volume: '25.5',
  price: '100',
  invType: 'SAL',
});

const keys = (result: { errors: { messageKey: string }[] }) => result.errors.map((e) => e.messageKey);

describe('validate (invoice fields)', () => {
  it('passes for a fully valid invoice', () => {
    const result = validate(validInvoice());
    expect(result.hasErrors()).toBe(false);
    expect(result.messages).toEqual([]);
  });

  it.each([
    { field: 'invNumber', value: '  ', key: 'invoice.client.invnumber.required.error' },
    { field: 'invNumber', value: 'inv 100!', key: 'invoice.client.invnumber.pattern.error' },
    { field: 'invDate', value: '', key: 'invoice.client.invdate.required.error' },
    { field: 'invType', value: '', key: 'invoice.client.invtype.required.error' },
    { field: 'invType', value: 'sal1', key: 'invoice.client.invtype.pattern.error' },
    { field: 'submittedBy', value: '', key: 'invoice.client.submittedby.required.error' },
    { field: 'submittedBy', value: 'Broker', key: 'invoice.client.submittedby.pattern.error' },
    { field: 'submitterLocation', value: '', key: 'invoice.client.submitterlocation.required.error' },
    { field: 'submitterLocation', value: '7', key: 'invoice.client.submitterlocation.pattern.error' },
    { field: 'otherClientLocation', value: 'AB', key: 'invoice.client.otherlocation.pattern.error' },
  ])('flags $key when $field is "$value"', ({ field, value, key }) => {
    const result = validate({ ...validInvoice(), [field]: value });
    expect(keys(result)).toContain(key);
  });

  it('accepts an empty optional other-client location', () => {
    const result = validate({ ...validInvoice(), otherClientLocation: '' });
    expect(result.hasErrors()).toBe(false);
  });

  it('accepts a two-digit other-client location', () => {
    const result = validate({ ...validInvoice(), otherClientLocation: '01' });
    expect(result.hasErrors()).toBe(false);
  });
});

describe('validateLineItem', () => {
  it('passes for valid numeric inputs', () => {
    const result = validateLineItem(validLineItem());
    expect(result.hasErrors()).toBe(false);
    expect(result.messages).toEqual([]);
  });

  it('passes when all inputs are blank (structural checks only)', () => {
    const result = validateLineItem({ pieces: '', volume: ' ', price: '', invType: 'SAL' });
    expect(result.hasErrors()).toBe(false);
  });

  it.each([
    { field: 'pieces', value: '1.5', key: 'invoice.client.pieces.integer.error' },
    { field: 'pieces', value: 'ten', key: 'invoice.client.pieces.integer.error' },
    { field: 'pieces', value: '0', key: 'invoice.client.pieces.positive.error' },
    { field: 'pieces', value: '-2', key: 'invoice.client.pieces.positive.error' },
    { field: 'volume', value: 'abc', key: 'invoice.client.volume.numeric.error' },
    { field: 'volume', value: '-1', key: 'invoice.client.volume.negative.error' },
    { field: 'price', value: 'abc', key: 'invoice.client.price.numeric.error' },
    { field: 'price', value: '-1', key: 'invoice.client.price.negative.error' },
  ])('flags $key when $field is "$value"', ({ field, value, key }) => {
    const result = validateLineItem({ ...validLineItem(), [field]: value });
    expect(keys(result)).toContain(key);
  });

  it('skips the sign rules for adjustment invoices but keeps the numeric rules', () => {
    const result = validateLineItem({ pieces: '-2', volume: '-1', price: '-1', invType: 'ADJ' });
    expect(result.hasErrors()).toBe(false);

    const stillNumeric = validateLineItem({ pieces: '1.5', volume: 'abc', price: 'abc', invType: 'ADJ' });
    expect(keys(stillNumeric)).toEqual([
      'invoice.client.pieces.integer.error',
      'invoice.client.volume.numeric.error',
      'invoice.client.price.numeric.error',
    ]);
  });
});
