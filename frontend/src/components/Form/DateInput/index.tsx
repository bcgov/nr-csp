import { DatePicker, DatePickerInput } from '@carbon/react';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { flushSync } from 'react-dom';

import type { FC } from 'react';

import './index.scss';

interface DateInputProps {
  id: string;
  labelText: React.ReactNode;
  placeholder?: string;
  dateFormat?: string;
  hideLabel?: boolean;
  size?: 'sm' | 'md' | 'lg';
  value?: string | Date | Array<string | Date>;
  onChange?: (dates: Date[]) => void;
  invalid?: boolean;
  invalidText?: string;
  disabled?: boolean;
}

type ParseResult = Date | 'invalid' | null;

// Build a LOCAL-midnight Date. DateInput emits local-midnight dates for every
// entry path — Flatpickr calendar picks already come back at local midnight,
// and the incoming `value` normaliser (`toDate`) parses ISO strings the same
// way — so all three stay consistent. Consumers can then format with local
// Y/M/D (see `formatIsoDate`) and get the day the user actually picked in any
// timezone, instead of the UTC shift `toISOString()` would introduce.
const buildLocalDate = (year: number, month: number, day: number): ParseResult => {
  if (month < 1 || month > 12) return 'invalid';
  const date = new Date(year, month - 1, day);
  if (date.getMonth() !== month - 1 || date.getDate() !== day) return 'invalid';
  return date;
};

// Parse a raw input string. Accepts the field's native display format AND the
// shapes a browser is likely to autofill (ISO `yyyy-mm-dd`, `yyyy/mm/dd`, and
// `yyyy-mm` for the year/month variant) so autofilled values aren't discarded.
// Surrounding whitespace is trimmed, the way Flatpickr's own parser does, so a
// date pasted out of a spreadsheet or an email isn't rejected over a stray space.
const parseDateInput = (rawVal: string, dateFormat: string): ParseResult => {
  const val = rawVal.trim();
  if (dateFormat === 'Y-m') {
    // Native `yyyy-mm` and autofilled `yyyy/mm`.
    const m = /^(\d{4})[/-](\d{1,2})$/.exec(val);
    if (m) return buildLocalDate(+m[1], +m[2], 1);
    return null;
  }
  // Native Y-m-d (`yyyy-mm-dd`) and autofilled ISO (`yyyy/mm/dd`).
  const m = /^(\d{4})[/-](\d{1,2})[/-](\d{1,2})$/.exec(val);
  if (m) return buildLocalDate(+m[1], +m[2], +m[3]);
  return null;
};

// Flatpickr may be handed a bare date or an array of them. Carbon commits the
// typed text on Enter through its fixEventsPlugin as an ARRAY of strings
// (`setDate([input.value], true, format)`), so both shapes have to be inspected.
const asList = (date: unknown): unknown[] => (Array.isArray(date) ? date : [date]);

const hasUnparseableString = (date: unknown, dateFormat: string): boolean =>
  asList(date).some(
    (v) => typeof v === 'string' && v.trim() !== '' && !(parseDateInput(v, dateFormat) instanceof Date),
  );

const hasString = (date: unknown): boolean => asList(date).some((v) => typeof v === 'string');

// A primitive stand-in for the `value` prop, so an effect can watch it without
// re-firing on every render just because the parent built a fresh Date object.
const valueToKey = (value: DateInputProps['value']): string => {
  if (value === undefined) return '';
  const one = (v: string | Date): string => (v instanceof Date ? String(v.getTime()) : v);
  return Array.isArray(value) ? value.map(one).filter(Boolean).join(',') : one(value);
};

const INVALID_DATE_TEXT = 'Invalid date';

// Which message the field shows while it is in the error state. An externally
// supplied message (form or server validation) wins — it is the more specific
// of the two — and the field's own parse failure fills in otherwise.
const resolveInvalidText = (
  invalid: boolean | undefined,
  invalidText: string | undefined,
  parseFailed: boolean,
): string | undefined => {
  if (invalid && invalidText) return invalidText;
  if (parseFailed) return INVALID_DATE_TEXT;
  return invalidText;
};

interface FlatpickrInstance {
  setDate: (date: unknown, triggerChange?: boolean, format?: string) => void;
  close?: () => void;
  __cspOrigSetDate?: FlatpickrInstance['setDate'];
}

const getFlatpickr = (input: Element | null | undefined): FlatpickrInstance | undefined =>
  (input as (Element & { _flatpickr?: FlatpickrInstance }) | null | undefined)?._flatpickr;

const DateInput: FC<DateInputProps> = ({
  id,
  labelText,
  placeholder,
  dateFormat = 'Y-m-d',
  hideLabel,
  size = 'md',
  value,
  onChange,
  invalid,
  invalidText,
  disabled,
}: DateInputProps): React.ReactElement => {
  const [inputInvalid, setInputInvalid] = useState(false);
  const inputInvalidRef = useRef(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  // The setDate guard is installed once, so it reads the format from a ref
  // (kept in step by the effect below) rather than closing over the prop value
  // of the render that happened to install it.
  const dateFormatRef = useRef(dateFormat);

  const resolvedPlaceholder = placeholder ?? (dateFormat === 'Y-m' ? 'yyyy-mm' : 'yyyy-mm-dd');

  // A value pushed in by the parent (e.g. R11's end date auto-filled from the
  // time frame) replaces whatever the user typed, so a parse failure raised
  // against the old text no longer describes the field and has to be dropped —
  // otherwise the field sits there red over a date it is displaying correctly.
  // Only a non-empty value clears it: an empty one is usually this component's
  // own `[]` echoing back through a controlled consumer, and clearing on that
  // would wipe the error just raised.
  const valueKey = valueToKey(value);
  useEffect(() => {
    if (!valueKey) return;
    inputInvalidRef.current = false;
    setInputInvalid(false);
  }, [valueKey]);

  // Flatpickr is created by Carbon's DatePicker one commit AFTER this component
  // first mounts (its init effect waits on internal `hasInput` state), so the
  // instance isn't there to patch during our own mount effect. Rather than rely
  // on a later re-render happening to install it, every handler calls this
  // first — by the time any of them run, Flatpickr exists.
  const ensureSetDateGuard = useCallback(() => {
    const fp = getFlatpickr(containerRef.current?.querySelector('input'));
    if (!fp || fp.__cspOrigSetDate) return;

    const originalSetDate = fp.setDate;
    fp.__cspOrigSetDate = originalSetDate;
    fp.setDate = function (date: unknown, triggerChange?: boolean, format?: string) {
      // Two reasons to refuse a payload: it carries raw text Flatpickr would
      // roll over into a different date, or the field is already flagged
      // invalid and this call would overwrite what the user typed. Dates (the
      // calendar's own picks and our reformat round-trip) always pass.
      if (hasUnparseableString(date, dateFormatRef.current)) return;
      if (inputInvalidRef.current && hasString(date)) return;
      originalSetDate.call(fp, date, triggerChange, format);
    };
  }, []);

  const validateValue = useCallback(
    (val: string) => {
      if (!val) {
        inputInvalidRef.current = false;
        setInputInvalid(false);
        onChangeRef.current?.([]);
        return;
      }

      const parsed = parseDateInput(val, dateFormat);

      if (parsed === 'invalid') {
        inputInvalidRef.current = true;
        setInputInvalid(true);
        onChangeRef.current?.([]);
        return;
      }

      inputInvalidRef.current = false;
      setInputInvalid(false);

      if (!parsed) {
        // Unrecognised / incomplete (e.g. mid-typing) — clear without warning.
        onChangeRef.current?.([]);
        return;
      }

      onChangeRef.current?.([parsed]);

      // If the value arrived in a non-native shape (e.g. browser autofill gave
      // a slash-separated date), redisplay it in the field's own dash format.
      // Native-format typing is already in the right shape, so we skip the
      // round-trip to avoid moving the caret while the user types. The month/day
      // are matched with `\d{1,2}` so a mid-typed value (e.g. "2026-12-2" before
      // the final digit) still counts as native and isn't prematurely reformatted
      // — that reformat would jump the caret and corrupt the rest of the input.
      // Passing a Date (not a string) bypasses the setDate guard.
      const isNativeShape = dateFormat === 'Y-m' ? /^\d{4}-\d{1,2}$/.test(val) : /^\d{4}-\d{1,2}-\d{1,2}$/.test(val);
      if (!isNativeShape) {
        const fp = getFlatpickr(containerRef.current?.querySelector('input'));
        fp?.setDate(parsed, false);
      }
    },
    [dateFormat],
  );

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const el = container.querySelector('input') as HTMLInputElement | null;
    if (!el) return;
    dateFormatRef.current = dateFormat;
    // Best effort at mount (Flatpickr may not exist yet) and a catch-up on
    // later renders, e.g. when a `value` prop arrives before the user has
    // touched the field.
    ensureSetDateGuard();

    const captureHandler = (e: Event) => {
      ensureSetDateGuard();
      e.stopImmediatePropagation();
      flushSync(() => validateValue((e.target as HTMLInputElement).value));
    };

    // Enter and blur are where the user declares the value finished. A value the
    // input handler let pass as merely mid-typing ("2026-1", "abc") is a real
    // error at that point, so flag it.
    //
    // This is display only — it deliberately never calls `onChange`. The parent
    // already holds `[]` for anything unparseable (the input handler emits that
    // as the user types, for both 'invalid' and incomplete values), and pages
    // read an `onChange` as a manual edit: R11 unlinks Time frame from the end
    // date on one, and several clear the field's validation error. Blurring a
    // field is not an edit, so it must not emit.
    const commit = () => {
      if (!el.value.trim()) return;
      if (parseDateInput(el.value, dateFormat) instanceof Date) return;
      inputInvalidRef.current = true;
      setInputInvalid(true);
    };

    const keyDownHandler = (e: Event) => {
      ensureSetDateGuard();
      if ((e as KeyboardEvent).key !== 'Enter') return;
      const val = el.value;
      // Nothing typed: leave Enter alone so Flatpickr can close the calendar.
      if (!val.trim()) return;
      // A value we accept is Flatpickr's to commit — it keeps the calendar
      // selection in step with the field.
      if (parseDateInput(val, dateFormat) instanceof Date) return;
      // Anything else must not reach Flatpickr. Both Carbon's fixEventsPlugin
      // (`setDate([input.value], true, format)`) and Flatpickr's own Enter
      // handler feed the raw text to a parser that rolls overflow forward, so
      // "2026-02-51" would come back as 2026-03-23. These handlers attach when
      // Flatpickr initialises — a commit after ours — so stopping the event
      // here runs before either of them sees it.
      e.stopImmediatePropagation();
      // Stopping the event also stops Flatpickr's own Enter handler, the only
      // caller of `close()`. Carbon's separate `keypress` listener still strips
      // the calendar's `open` class, so without this the calendar would be
      // hidden while Flatpickr still thinks it is open — and `open()` bails out
      // on an already-open calendar, leaving it unable to reopen.
      getFlatpickr(el)?.close?.();
      commit();
    };

    const blurHandler = (e: Event) => {
      ensureSetDateGuard();
      e.stopImmediatePropagation();
      commit();
    };

    el.addEventListener('input', captureHandler, true);
    el.addEventListener('keydown', keyDownHandler, true);
    el.addEventListener('blur', blurHandler, true);
    return () => {
      el.removeEventListener('input', captureHandler, true);
      el.removeEventListener('keydown', keyDownHandler, true);
      el.removeEventListener('blur', blurHandler, true);
    };
  }, [validateValue, dateFormat, ensureSetDateGuard]);

  const handleCalendarChange = (dates: Date[]) => {
    inputInvalidRef.current = false;
    setInputInvalid(false);
    onChangeRef.current?.(dates);
  };

  // Normalise the incoming `value` to something Flatpickr can always parse.
  const toDate = (v: string | Date): Date | string => {
    if (v instanceof Date) return v;
    const iso = /^\d{4}-\d{2}-\d{2}$/;
    if (iso.test(v)) {
      const [y, m, d] = v.split('-').map(Number);
      return new Date(y, m - 1, d);
    }
    return v;
  };
  const normalisedValue = value === undefined ? undefined : Array.isArray(value) ? value.map(toDate) : toDate(value);

  // A value the field can't parse is an error, not an advisory: it is never
  // submitted, so it has to read like something the user must fix (red border,
  // error icon, `aria-invalid`) rather than a warning they can carry on past.
  const showInvalid = invalid || inputInvalid;
  const resolvedInvalidText = resolveInvalidText(invalid, invalidText, inputInvalid);

  return (
    <div ref={containerRef}>
      <DatePicker
        datePickerType="single"
        dateFormat={dateFormat}
        className="date-input"
        style={{ width: '100%' }}
        value={normalisedValue}
        invalid={showInvalid}
        onChange={handleCalendarChange}
        disabled={disabled}
      >
        <DatePickerInput
          id={id}
          labelText={labelText}
          placeholder={resolvedPlaceholder}
          hideLabel={hideLabel}
          size={size}
          style={{ width: '100%', maxWidth: '100%' }}
          invalidText={resolvedInvalidText}
          disabled={disabled}
        />
      </DatePicker>
    </div>
  );
};

export default DateInput;
