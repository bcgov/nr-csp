import { describe, expect, it } from 'vitest';

import { validateSubmissionMetadata, type SubmissionMetadataValues } from './submission';

const valid = (): SubmissionMetadataValues => ({
  submissionClientNumber: '00126920',
  submissionClientLocnCode: '00',
  monthComplete: 'N',
  sellerSubmission: 'Y',
});

const keys = (result: ReturnType<typeof validateSubmissionMetadata>): string[] =>
  result.messages.map((m) => m.messageKey);

describe('validateSubmissionMetadata', () => {
  it('produces no errors for well-formed metadata', () => {
    const result = validateSubmissionMetadata(valid());
    expect(result.hasErrors()).toBe(false);
    expect(result.messages).toEqual([]);
  });

  it('accepts lower-case y/n for both flags (case-insensitive pattern)', () => {
    const result = validateSubmissionMetadata({ ...valid(), monthComplete: 'y', sellerSubmission: 'n' });
    expect(result.hasErrors()).toBe(false);
    expect(result.messages).toEqual([]);
  });

  describe('submissionClientNumber (^\\d{8}$)', () => {
    it('required error when blank/whitespace', () => {
      const result = validateSubmissionMetadata({ ...valid(), submissionClientNumber: '   ' });
      expect(keys(result)).toContain('submission.client.clientnumber.required.error');
      expect(keys(result)).not.toContain('submission.client.clientnumber.pattern.error');
    });

    it('pattern error when present but not 8 digits', () => {
      const result = validateSubmissionMetadata({ ...valid(), submissionClientNumber: '123' });
      expect(keys(result)).toContain('submission.client.clientnumber.pattern.error');
    });

    it('no error when a valid 8-digit number', () => {
      const result = validateSubmissionMetadata({ ...valid(), submissionClientNumber: '12345678' });
      expect(keys(result)).not.toContain('submission.client.clientnumber.required.error');
      expect(keys(result)).not.toContain('submission.client.clientnumber.pattern.error');
    });
  });

  describe('submissionClientLocnCode (^\\d{2}$)', () => {
    it('required error when blank/whitespace', () => {
      const result = validateSubmissionMetadata({ ...valid(), submissionClientLocnCode: '' });
      expect(keys(result)).toContain('submission.client.locationcode.required.error');
      expect(keys(result)).not.toContain('submission.client.locationcode.pattern.error');
    });

    it('pattern error when present but not 2 digits', () => {
      const result = validateSubmissionMetadata({ ...valid(), submissionClientLocnCode: '1' });
      expect(keys(result)).toContain('submission.client.locationcode.pattern.error');
    });

    it('no error when a valid 2-digit code', () => {
      const result = validateSubmissionMetadata({ ...valid(), submissionClientLocnCode: '42' });
      expect(keys(result)).not.toContain('submission.client.locationcode.required.error');
      expect(keys(result)).not.toContain('submission.client.locationcode.pattern.error');
    });
  });

  describe('monthComplete (^[YN]$ case-insensitive)', () => {
    it('required error when blank/whitespace', () => {
      const result = validateSubmissionMetadata({ ...valid(), monthComplete: '  ' });
      expect(keys(result)).toContain('submission.client.monthcomplete.required.error');
      expect(keys(result)).not.toContain('submission.client.monthcomplete.pattern.error');
    });

    it('pattern error when present but not Y/N', () => {
      const result = validateSubmissionMetadata({ ...valid(), monthComplete: 'X' });
      expect(keys(result)).toContain('submission.client.monthcomplete.pattern.error');
    });

    it('no error when a valid Y/N value', () => {
      const result = validateSubmissionMetadata({ ...valid(), monthComplete: 'Y' });
      expect(keys(result)).not.toContain('submission.client.monthcomplete.required.error');
      expect(keys(result)).not.toContain('submission.client.monthcomplete.pattern.error');
    });
  });

  describe('sellerSubmission (^[YN]$ case-insensitive)', () => {
    it('required error when blank/whitespace', () => {
      const result = validateSubmissionMetadata({ ...valid(), sellerSubmission: '\t ' });
      expect(keys(result)).toContain('submission.client.sellersubmission.required.error');
      expect(keys(result)).not.toContain('submission.client.sellersubmission.pattern.error');
    });

    it('pattern error when present but not Y/N', () => {
      const result = validateSubmissionMetadata({ ...valid(), sellerSubmission: 'maybe' });
      expect(keys(result)).toContain('submission.client.sellersubmission.pattern.error');
    });

    it('no error when a valid Y/N value', () => {
      const result = validateSubmissionMetadata({ ...valid(), sellerSubmission: 'N' });
      expect(keys(result)).not.toContain('submission.client.sellersubmission.required.error');
      expect(keys(result)).not.toContain('submission.client.sellersubmission.pattern.error');
    });
  });

  it('reports errors for every invalid field at once', () => {
    const result = validateSubmissionMetadata({
      submissionClientNumber: '',
      submissionClientLocnCode: 'ab',
      monthComplete: '',
      sellerSubmission: 'zz',
    });
    expect(result.hasErrors()).toBe(true);
    expect(keys(result)).toEqual([
      'submission.client.clientnumber.required.error',
      'submission.client.locationcode.pattern.error',
      'submission.client.monthcomplete.required.error',
      'submission.client.sellersubmission.pattern.error',
    ]);
  });
});
