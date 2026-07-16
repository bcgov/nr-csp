import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/config/api/request';
import {
  parseSubmission,
  submissionErrorBody,
  submitSubmission,
  validateSubmissionBusiness,
  type SubmissionEdits,
} from '@/services/cspSubmission.service';

vi.mock('@/config/api/request', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}));

const makeFile = () => new File(['<xml/>'], 'submission.xml', { type: 'text/xml' });

const EDITS: SubmissionEdits = {
  submissionClientNumber: '00012345',
  submissionClientLocnCode: '00',
  monthComplete: '2026-05',
  sellerSubmission: 'Seller',
};

beforeEach(() => {
  vi.clearAllMocks();
});

// ── Endpoint fetchers ─────────────────────────────────────────────────────────

describe('cspSubmission endpoint fetchers', () => {
  it('parseSubmission POSTs the file as FormData to /submissions/parse', async () => {
    const response = { valid: true, code: 'OK', message: '', errors: [], submission: null };
    vi.mocked(apiClient.post).mockResolvedValue({ data: response });
    const file = makeFile();

    await expect(parseSubmission(file)).resolves.toEqual(response);

    expect(apiClient.post).toHaveBeenCalledWith('/submissions/parse', expect.any(FormData));
    const form = vi.mocked(apiClient.post).mock.calls[0][1] as FormData;
    expect(form.get('file')).toBe(file);
  });

  it('validateSubmissionBusiness POSTs the file as FormData to /submissions/validate/business', async () => {
    const response = {
      valid: true,
      code: 'OK',
      message: '',
      acceptedInvoices: [],
      rejectedInvoices: [],
      errors: [],
    };
    vi.mocked(apiClient.post).mockResolvedValue({ data: response });
    const file = makeFile();

    await expect(validateSubmissionBusiness(file)).resolves.toEqual(response);

    expect(apiClient.post).toHaveBeenCalledWith('/submissions/validate/business', expect.any(FormData));
    const form = vi.mocked(apiClient.post).mock.calls[0][1] as FormData;
    expect(form.get('file')).toBe(file);
  });

  it('submitSubmission POSTs the file plus edit fields as FormData to /submissions/submit', async () => {
    const response = {
      valid: true,
      code: 'OK',
      message: '',
      submissionId: 42,
      acceptedInvoices: [],
      rejectedInvoices: [],
      errors: [],
    };
    vi.mocked(apiClient.post).mockResolvedValue({ data: response });
    const file = makeFile();

    await expect(submitSubmission(file, EDITS)).resolves.toEqual(response);

    expect(apiClient.post).toHaveBeenCalledWith('/submissions/submit', expect.any(FormData));
    const form = vi.mocked(apiClient.post).mock.calls[0][1] as FormData;
    expect(form.get('file')).toBe(file);
    expect(form.get('submissionClientNumber')).toBe(EDITS.submissionClientNumber);
    expect(form.get('submissionClientLocnCode')).toBe(EDITS.submissionClientLocnCode);
    expect(form.get('monthComplete')).toBe(EDITS.monthComplete);
    expect(form.get('sellerSubmission')).toBe(EDITS.sellerSubmission);
  });
});

// ── Error helper ──────────────────────────────────────────────────────────────

describe('submissionErrorBody', () => {
  it('returns the envelope when response.data is an object with an errors key', () => {
    const envelope = { valid: false, code: 'VALIDATION', message: 'bad', errors: [] };
    expect(submissionErrorBody({ response: { data: envelope } })).toEqual(envelope);
  });

  it('returns null when response.data has no errors key', () => {
    expect(submissionErrorBody({ response: { data: { message: 'plain' } } })).toBeNull();
  });

  it('returns null when there is no response', () => {
    expect(submissionErrorBody(undefined)).toBeNull();
    expect(submissionErrorBody(new Error('network'))).toBeNull();
  });
});
