import { describe, expect, it } from 'vitest';

import { validateSubmissionMetadata } from './submission';

const valid = () => ({
  submissionClientNumber: '00126920',
  submissionClientLocnCode: '00',
  monthComplete: 'N',
  sellerSubmission: 'Y',
});

const keys = (result: ReturnType<typeof validateSubmissionMetadata>) => result.errors.map((e) => e.messageKey);

describe('validateSubmissionMetadata', () => {
  it('passes for well-formed metadata', () => {
    const result = validateSubmissionMetadata(valid());
    expect(result.hasErrors()).toBe(false);
    expect(result.messages).toEqual([]);
  });

  it('requires the client number', () => {
    const result = validateSubmissionMetadata({ ...valid(), submissionClientNumber: '  ' });
    expect(keys(result)).toContain('submission.client.clientnumber.required.error');
  });

  it('rejects a client number that is not 8 digits', () => {
    const result = validateSubmissionMetadata({ ...valid(), submissionClientNumber: '123' });
    expect(keys(result)).toContain('submission.client.clientnumber.pattern.error');
  });

  it('requires the location code', () => {
    const result = validateSubmissionMetadata({ ...valid(), submissionClientLocnCode: '' });
    expect(keys(result)).toContain('submission.client.locationcode.required.error');
  });

  it('rejects a location code that is not 2 digits', () => {
    const result = validateSubmissionMetadata({ ...valid(), submissionClientLocnCode: '1' });
    expect(keys(result)).toContain('submission.client.locationcode.pattern.error');
  });

  it('rejects a month-complete value other than Y or N', () => {
    const result = validateSubmissionMetadata({ ...valid(), monthComplete: 'X' });
    expect(keys(result)).toContain('submission.client.monthcomplete.pattern.error');
  });

  it('accepts a lower-case y/n for the flags', () => {
    const result = validateSubmissionMetadata({ ...valid(), monthComplete: 'y', sellerSubmission: 'n' });
    expect(result.hasErrors()).toBe(false);
  });

  it('rejects a seller-submission value other than Y or N', () => {
    const result = validateSubmissionMetadata({ ...valid(), sellerSubmission: 'maybe' });
    expect(keys(result)).toContain('submission.client.sellersubmission.pattern.error');
  });
});
