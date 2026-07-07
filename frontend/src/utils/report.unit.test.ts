import { afterAll, beforeAll, describe, expect, it } from 'vitest';

import { formatDate, formatYearMonth } from './report';

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
