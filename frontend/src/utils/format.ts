/**
 * Format a number with thousands separators (using the en-US locale, so a
 * comma is used) and a fixed number of decimal places. Empty / null /
 * undefined / NaN values render as an em-dash so empty cells stay readable.
 *
 * @example formatNumber(33330)        // "33,330"
 * @example formatNumber(50.985, 3)    // "50.985"
 * @example formatNumber(1234567.5, 2) // "1,234,567.50"
 * @example formatNumber(null)         // "—"
 */
export const formatNumber = (value: number | null | undefined, decimals = 0): string => {
  if (value === null || value === undefined || Number.isNaN(value)) return '—';
  return Number(value).toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
};

/**
 * Format a number as a USD-style currency string with a leading "$" and two
 * decimal places. Falls back to an em-dash for null / undefined / NaN.
 *
 * @example formatCurrency(312)      // "$312.00"
 * @example formatCurrency(11393.61) // "$11,393.61"
 * @example formatCurrency(null)     // "—"
 */
export const formatCurrency = (value: number | null | undefined): string => {
  if (value === null || value === undefined || Number.isNaN(value)) return '—';
  return `$${formatNumber(value, 2)}`;
};

/**
 * Format a `yyyy-mm-dd` date string as a human-readable date, e.g. "Jan 6, 2026".
 * Constructs the Date from parts to avoid UTC timezone shifts.
 */
export const formatDisplayDate = (dateStr: string): string => {
  const [year, month, day] = dateStr.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString('en-CA', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
};

/**
 * Format a Date as an ISO `yyyy-MM-dd` string using its LOCAL calendar
 * components. Use this instead of `Date.toISOString().slice(0, 10)` for
 * date-only values — `toISOString()` converts to UTC, which shifts a
 * local-midnight date to the previous day in positive UTC offsets. Reading
 * local components returns the day the user actually picked in any timezone.
 *
 * @example formatIsoDate(new Date(2026, 5, 1)) // "2026-06-01"
 */
export const formatIsoDate = (date: Date): string => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

/**
 * Format a Date as a display-friendly `MM/dd/yyyy` string using LOCAL
 * calendar components. This preserves the user's picked calendar day instead
 * of applying UTC shifts.
 *
 * @example formatDisplayDateV2(new Date(2026, 5, 1)) // "06/01/2026"
 */
export const formatDisplayDateV2 = (date: Date): string => {
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const y = date.getFullYear();
  return `${m}/${d}/${y}`;
};
