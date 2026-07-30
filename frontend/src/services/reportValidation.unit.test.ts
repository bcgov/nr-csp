import { describe, expect, it } from 'vitest';

import { parseReportValidationError } from '@/services/reportValidation';

const VALIDATION_BODY = {
  code: 'VALIDATION',
  message: 'Validation failed',
  errors: [{ messageKey: 'report.startdate.required.error', args: [], type: 'ERROR', message: 'Start date required' }],
};

describe('parseReportValidationError', () => {
  it('returns [] when the error has no response data', async () => {
    await expect(parseReportValidationError(new Error('network'))).resolves.toEqual([]);
    await expect(parseReportValidationError(undefined)).resolves.toEqual([]);
  });

  it('returns the errors from a plain JSON validation body', async () => {
    const error = { response: { data: VALIDATION_BODY } };
    await expect(parseReportValidationError(error)).resolves.toEqual(VALIDATION_BODY.errors);
  });

  it('parses a Blob body (blob responseType) into validation errors', async () => {
    const blob = new Blob([JSON.stringify(VALIDATION_BODY)], { type: 'application/json' });
    const error = { response: { data: blob } };

    await expect(parseReportValidationError(error)).resolves.toEqual(VALIDATION_BODY.errors);
  });

  it('returns [] for a Blob that is not valid JSON', async () => {
    const blob = new Blob(['%PDF-1.7 not json'], { type: 'application/pdf' });
    const error = { response: { data: blob } };

    await expect(parseReportValidationError(error)).resolves.toEqual([]);
  });

  it('returns [] when the body is not a validation error shape', async () => {
    const error = { response: { data: { message: 'plain server error' } } };
    await expect(parseReportValidationError(error)).resolves.toEqual([]);
  });
});
