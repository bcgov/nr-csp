import { describe, expect, it } from 'vitest';

import { MessageCollector, splitMessages, ValidationResult, type ValidationMessage } from './validationResult';

describe('MessageCollector / ValidationResult', () => {
  it('resolves a known message key to its template text', () => {
    const collector = new MessageCollector();
    collector.addError('report.startdate.required.error');

    const result = collector.result();
    expect(result.hasErrors()).toBe(true);
    expect(result.errors[0].message).toBe('Start date is required.');
  });

  it('interpolates positional args into the template', () => {
    const collector = new MessageCollector();
    collector.addError('report.client.noselection.error', ['seller']);

    expect(collector.result().errors[0].message).toBe('Select a valid seller client from the suggestion list.');
  });

  it('interpolates multiple args by index', () => {
    const collector = new MessageCollector();
    collector.addError('report.client.number.notfound.error', ['seller', '00012345']);

    expect(collector.result().errors[0].message).toBe('No client found for seller number: 00012345');
  });

  it('leaves the placeholder in place when an arg is missing', () => {
    const collector = new MessageCollector();
    collector.addError('report.client.number.notfound.error', ['seller']);

    expect(collector.result().errors[0].message).toBe('No client found for seller number: {1}');
  });

  it('falls back to the key itself for an unknown message key', () => {
    const collector = new MessageCollector();
    collector.addError('no.such.key');

    expect(collector.result().errors[0].message).toBe('no.such.key');
  });

  it('separates errors from warnings', () => {
    const collector = new MessageCollector();
    collector.addError('report.startdate.required.error');
    collector.addWarning('some.warning.key');

    const result = collector.result();
    expect(result.hasErrors()).toBe(true);
    expect(result.errors).toHaveLength(1);
    expect(result.warnings).toHaveLength(1);
    expect(result.warnings[0].type).toBe('WARNING');
  });

  it('hasErrors is false for a warnings-only result', () => {
    const collector = new MessageCollector();
    collector.addWarning('some.warning.key');

    expect(collector.result().hasErrors()).toBe(false);
  });

  it('an empty ValidationResult has no errors or warnings', () => {
    const result = new ValidationResult([]);
    expect(result.hasErrors()).toBe(false);
    expect(result.errors).toEqual([]);
    expect(result.warnings).toEqual([]);
  });
});

describe('splitMessages', () => {
  const messages: ValidationMessage[] = [
    { messageKey: 'field.a.error', message: 'A is bad', type: 'ERROR' },
    { messageKey: 'field.a.error', message: 'A is bad again', type: 'ERROR' },
    { messageKey: 'form.level.error', message: 'Form is bad', type: 'ERROR' },
    { messageKey: 'some.warning', message: 'Heads up', type: 'WARNING' },
  ];

  it('routes mapped errors to fieldErrors, unmapped to formErrors, warnings aside', () => {
    const split = splitMessages(messages, { 'field.a.error': 'fieldA' });

    expect(split.fieldErrors).toEqual({ fieldA: 'A is bad' });
    expect(split.formErrors).toEqual(['Form is bad']);
    expect(split.warnings).toEqual(['Heads up']);
  });

  it('keeps only the first error per field', () => {
    const split = splitMessages(messages, { 'field.a.error': 'fieldA' });
    expect(split.fieldErrors.fieldA).toBe('A is bad');
  });

  it('returns empty buckets for no messages', () => {
    const split = splitMessages([], {});
    expect(split).toEqual({ fieldErrors: {}, formErrors: [], warnings: [] });
  });
});
