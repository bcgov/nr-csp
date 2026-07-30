import { afterAll, beforeAll, describe, expect, it } from 'vitest';

import { formatCurrency, formatDisplayDate, formatDisplayDateV2, formatIsoDate, formatNumber } from './format';

// ── formatNumber ────────────────────────────────────────────────────────────

describe('formatNumber', () => {
  it('adds thousands separators with no decimals by default', () => {
    expect(formatNumber(33330)).toBe('33,330');
  });

  it('honours the decimals argument', () => {
    expect(formatNumber(50.985, 3)).toBe('50.985');
    expect(formatNumber(1234567.5, 2)).toBe('1,234,567.50');
  });

  it('renders an em-dash for null / undefined / NaN', () => {
    expect(formatNumber(null)).toBe('—');
    expect(formatNumber(undefined)).toBe('—');
    expect(formatNumber(NaN)).toBe('—');
  });
});

// ── formatCurrency ──────────────────────────────────────────────────────────

describe('formatCurrency', () => {
  it('prefixes a "$" and forces two decimals', () => {
    expect(formatCurrency(312)).toBe('$312.00');
    expect(formatCurrency(11393.61)).toBe('$11,393.61');
  });

  it('renders an em-dash for null / undefined / NaN', () => {
    expect(formatCurrency(null)).toBe('—');
    expect(formatCurrency(undefined)).toBe('—');
    expect(formatCurrency(NaN)).toBe('—');
  });
});

// ── formatDisplayDate ───────────────────────────────────────────────────────

describe('formatDisplayDate', () => {
  it('formats a yyyy-mm-dd string with a full month name', () => {
    expect(formatDisplayDate('2026-01-01')).toBe('January 1, 2026');
    expect(formatDisplayDate('2026-06-06')).toBe('June 6, 2026');
  });

  it('returns an em-dash for null / undefined / empty', () => {
    expect(formatDisplayDate(null)).toBe('—');
    expect(formatDisplayDate(undefined)).toBe('—');
    expect(formatDisplayDate('')).toBe('—');
  });
});

// ── Timezone safety ─────────────────────────────────────────────────────────
//
// DateInput emits local-midnight Date objects; formatIsoDate / formatDisplayDateV2
// read LOCAL calendar components so the day the user picked survives serialization
// in any timezone. These suites pin that contract by forcing the host timezone to
// a positive offset (Tokyo, UTC+9) and a negative one (Vancouver, UTC-8). A naive
// `toISOString().slice(0, 10)` implementation would shift the day back under Tokyo
// and fail here — which is exactly the regression we want to catch.

const runTimezoneContract = (tz: string) => {
  describe(`timezone contract under ${tz}`, () => {
    const original = process.env.TZ;
    beforeAll(() => {
      process.env.TZ = tz;
    });
    afterAll(() => {
      process.env.TZ = original;
    });

    it('formatIsoDate keeps the picked calendar day', () => {
      // Local midnight on the 1st — the worst case for a UTC shift.
      expect(formatIsoDate(new Date(2026, 5, 1))).toBe('2026-06-01');
      expect(formatIsoDate(new Date(2026, 0, 1))).toBe('2026-01-01');
      expect(formatIsoDate(new Date(2026, 11, 31))).toBe('2026-12-31');
    });

    it('formatDisplayDateV2 keeps the picked calendar day', () => {
      expect(formatDisplayDateV2(new Date(2026, 5, 1))).toBe('2026-06-01');
      expect(formatDisplayDateV2(new Date(2026, 11, 31))).toBe('2026-12-31');
    });

    it('zero-pads single-digit months and days', () => {
      expect(formatIsoDate(new Date(2026, 2, 5))).toBe('2026-03-05');
      expect(formatDisplayDateV2(new Date(2026, 2, 5))).toBe('2026-03-05');
    });
  });
};

runTimezoneContract('Asia/Tokyo'); // UTC+9 — exposes UTC-shift bugs
runTimezoneContract('America/Vancouver'); // UTC-8/-7 — the BC users' zone
