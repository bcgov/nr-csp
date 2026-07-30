import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import {
  downloadBlob,
  formatDate,
  formatYearMonth,
  isNoDataError,
  itemToString,
  lookupCodeToString,
  lookupDescriptionToString,
  lookupItemToString,
  parseContentDispositionFilename,
  TIME_FRAME_ITEMS,
} from './report';

// formatDate / formatYearMonth build the date strings sent to the backend for the
// R07 / R08 / R13 reports (including R13's calculated end date). They read LOCAL
// calendar components, so the day the user picked must survive in any timezone.
// Forcing a positive offset (Tokyo, UTC+9) and a negative one (Vancouver, UTC-8)
// catches any regression to a UTC-based serializer, which would shift the day —
// e.g. dropping the final day from an R13 report range.

const runTimezoneContract = (tz: string) => {
  describe(`request-payload date serialization under ${tz}`, () => {
    const original = process.env.TZ;
    beforeAll(() => {
      process.env.TZ = tz;
    });
    afterAll(() => {
      process.env.TZ = original;
    });

    it('formatDate emits yyyymmdd for the picked calendar day', () => {
      expect(formatDate(new Date(2026, 5, 1))).toBe('20260601');
      // Last-day-of-month, the R13 calculated-end-date case.
      expect(formatDate(new Date(2026, 2, 31))).toBe('20260331');
      expect(formatDate(new Date(2026, 0, 1))).toBe('20260101');
    });

    it('formatYearMonth emits yyyymm for the picked month', () => {
      expect(formatYearMonth(new Date(2026, 5, 1))).toBe('202606');
      expect(formatYearMonth(new Date(2026, 11, 31))).toBe('202612');
    });
  });
};

runTimezoneContract('Asia/Tokyo'); // UTC+9 — exposes UTC-shift bugs
runTimezoneContract('America/Vancouver'); // UTC-8/-7 — the BC users' zone

describe('parseContentDispositionFilename', () => {
  it('prefers the RFC 5987 filename* form and decodes it', () => {
    expect(parseContentDispositionFilename("attachment; filename*=UTF-8''R06%20June%202026.pdf")).toBe(
      'R06 June 2026.pdf',
    );
  });

  it('falls back to the quoted filename when filename* decoding fails', () => {
    expect(parseContentDispositionFilename('attachment; filename*=UTF-8\'\'%E0%A4%A; filename="fallback.pdf"')).toBe(
      'fallback.pdf',
    );
  });

  it('reads a quoted filename', () => {
    expect(parseContentDispositionFilename('attachment; filename="report.csv"')).toBe('report.csv');
  });

  it('reads an unquoted filename and trims it', () => {
    expect(parseContentDispositionFilename('attachment; filename= plain.pdf ')).toBe('plain.pdf');
  });

  it('returns null when no filename is present', () => {
    expect(parseContentDispositionFilename('attachment')).toBeNull();
    expect(parseContentDispositionFilename('')).toBeNull();
  });
});

describe('downloadBlob', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('creates a temporary anchor, clicks it, and revokes the object URL', () => {
    vi.useFakeTimers();
    const createObjectURL = vi.fn().mockReturnValue('blob:mock-url');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { ...window.URL, createObjectURL, revokeObjectURL } as unknown as typeof URL);
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    downloadBlob(new Blob(['data']), 'R06_result.pdf');

    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(clickSpy).toHaveBeenCalledTimes(1);
    // The anchor must be cleaned out of the DOM again.
    expect(document.querySelector('a[download]')).toBeNull();

    vi.runAllTimers();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
    vi.unstubAllGlobals();
  });
});

describe('isNoDataError', () => {
  it('is true only for an axios-like 404 response', () => {
    expect(isNoDataError({ response: { status: 404 } })).toBe(true);
    expect(isNoDataError({ response: { status: 500 } })).toBe(false);
    expect(isNoDataError(new Error('plain'))).toBe(false);
    expect(isNoDataError(null)).toBe(false);
    expect(isNoDataError('404')).toBe(false);
  });
});

describe('select-item helpers', () => {
  const item = { code: 'O', description: 'Old Growth' };

  it('itemToString returns the label or an empty string', () => {
    expect(itemToString({ id: '01', label: '01' })).toBe('01');
    expect(itemToString(null)).toBe('');
  });

  it('lookupItemToString formats "code - description" or an empty string', () => {
    expect(lookupItemToString(item)).toBe('O - Old Growth');
    expect(lookupItemToString(null)).toBe('');
  });

  it('lookupCodeToString / lookupDescriptionToString handle null items', () => {
    expect(lookupCodeToString(item)).toBe('O');
    expect(lookupCodeToString(null)).toBe('');
    expect(lookupDescriptionToString(item)).toBe('Old Growth');
    expect(lookupDescriptionToString(null)).toBe('');
  });

  it('TIME_FRAME_ITEMS lists the zero-padded months 01 through 12', () => {
    expect(TIME_FRAME_ITEMS).toHaveLength(12);
    expect(TIME_FRAME_ITEMS[0]).toEqual({ id: '01', label: '01' });
    expect(TIME_FRAME_ITEMS[11]).toEqual({ id: '12', label: '12' });
  });
});
