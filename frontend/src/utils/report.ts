import type { LookupItemResponse } from '@/services/lookup.service';

export type SelectItem = { id: string; label: string };

export const formatDate = (date: Date): string => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}${m}${d}`;
};

export const formatYearMonth = (date: Date): string => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  return `${y}${m}`;
};

export const downloadBlob = (blob: Blob, filename: string): void => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => window.URL.revokeObjectURL(url), 0);
};

export const HARDCODED_MODELLING_CODE_ITEMS: LookupItemResponse[] = [
  { code: 'P', description: 'Production' },
  { code: 'M1', description: 'Scenario 1' },
  { code: 'M2', description: 'Scenario 2' },
  { code: 'M3', description: 'Scenario 3' },
];

export const TIME_FRAME_ITEMS: SelectItem[] = Array.from({ length: 12 }, (_, i) => {
  const val = String(i + 1).padStart(2, '0');
  return { id: val, label: val };
});

// timeFrame N = last day of the month that is (N-1) months after startDate's month.
// e.g. start = March 15 + timeFrame '01' → March 31; start = March 15 + timeFrame '02' → April 30
export const calculateEndDateFromTimeFrame = (startDate: Date, timeFrame: string): Date => {
  const months = Number.parseInt(timeFrame, 10);
  // Build a LOCAL-midnight last-day-of-month date so it stays consistent with
  // startDate (DateInput emits local-midnight) and with the local-component
  // formatters used for display (formatDisplayDateV2) and the request payload
  // (formatDate). Using Date.UTC here would produce a UTC-midnight instant
  // that those local getters then read back one day early in negative-offset
  // timezones (e.g. Pacific), dropping the final day from the report range.
  return new Date(startDate.getFullYear(), startDate.getMonth() + months, 0);
};

export const itemToString = (item: SelectItem | null): string => item?.label ?? '';

export const lookupItemToString = (item: LookupItemResponse | null): string =>
  item ? `${item.code} - ${item.description}` : '';

export const lookupCodeToString = (item: LookupItemResponse | null): string => item?.code ?? '';

export const lookupDescriptionToString = (item: LookupItemResponse | null): string => item?.description ?? '';

export function isNoDataError(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'response' in error &&
    (error as { response: { status: number } }).response?.status === 404
  );
}

export function parseContentDispositionFilename(header: string): string | null {
  const extMatch = header.match(/filename\*\s*=\s*UTF-8'[^']*'([^;\r\n]+)/i);
  if (extMatch) {
    try {
      return decodeURIComponent(extMatch[1].trim());
    } catch {
      // fall through to filename=
    }
  }

  const quotedMatch = header.match(/filename\s*=\s*"([^"]+)"/);
  if (quotedMatch) return quotedMatch[1];

  const plainMatch = header.match(/filename\s*=\s*([^";\r\n]+)/);
  if (plainMatch) return plainMatch[1].trim();

  return null;
}
