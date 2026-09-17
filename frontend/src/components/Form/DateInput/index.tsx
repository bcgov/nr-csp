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

// What the field is currently holding, as the Enter and blur paths both need to
// know it: nothing to judge yet, something committable, or something to reject.
type EntryState = 'empty' | 'valid' | 'rejected';

const entryState = (text: string, dateFormat: string): EntryState => {
  if (!text.trim()) return 'empty';
  return parseDateInput(text, dateFormat) instanceof Date ? 'valid' : 'rejected';
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

type NormalisedValue = Date | string | Array<Date | string> | undefined;

const normaliseValue = (value: DateInputProps['value']): NormalisedValue =>
  value === undefined ? undefined : Array.isArray(value) ? value.map(toDate) : toDate(value);

// A primitive stand-in for a normalised value, so it can be compared against
// what this field last reported and watched by an effect without re-firing on
// every render just because the parent built a fresh Date object. Pages hold
// the date in whichever shape suits them — R11 keeps a Date, Search and Inbox
// keep an ISO string — and `toDate` has already reconciled the two by here.
const timeKey = (v: Date | string): string => (v instanceof Date ? String(v.getTime()) : v);

const keyOf = (v: NormalisedValue): string => {
  if (v === undefined) return '';
  return Array.isArray(v) ? v.map(timeKey).filter(Boolean).join(',') : timeKey(v);
};

const keyOfReport = (dates: Date[]): string => dates.map((d) => String(d.getTime())).join(',');

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

// Flatpickr's supported parsing hook, which Carbon forwards straight to it.
// Everything below is a second line of defence for the entry paths a user
// actually takes; this is the first, and the only one covering parses that
// happen before any of it is in place — Flatpickr resolves `defaultDate` while
// it is being constructed, a commit before this component can reach in. On its
// own it is not enough: Flatpickr answers a rejected parse by clearing the
// field, discarding the entry and the error with it, which is why the entry
// paths are still headed off before they get here.
const strictParseDate =
  (dateFormat: string) =>
  (date: string, format?: string): Date | undefined => {
    const parsed = parseDateInput(date, format || dateFormat);
    return parsed instanceof Date ? parsed : undefined;
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
  const containerRef = useRef<HTMLDivElement>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  // The setDate guard is installed once, so it reads the format from a ref
  // (kept in step by the effect below) rather than closing over the prop value
  // of the render that happened to install it.
  const dateFormatRef = useRef(dateFormat);

  const resolvedPlaceholder = placeholder ?? (dateFormat === 'Y-m' ? 'yyyy-mm' : 'yyyy-mm-dd');

  const normalisedValue = normaliseValue(value);
  const incomingKey = keyOf(normalisedValue);

  // Everything this field reports goes through here, so the value a controlled
  // page feeds back can be recognised as this field's own words returning.
  // `null` means it has said nothing yet, which is not the same as having
  // reported "no date" — otherwise a page clearing a value it set itself would
  // look like an echo and be ignored.
  const [lastReportedKey, setLastReportedKey] = useState<string | null>(null);
  const report = useCallback((dates: Date[]) => {
    setLastReportedKey(keyOfReport(dates));
    onChangeRef.current?.(dates);
  }, []);

  // Carbon hands whatever `value` it is given straight to Flatpickr, which
  // rewrites the input's text — and blanks it outright when that value is empty
  // while Flatpickr holds a selection. Applied to the value a controlled page
  // feeds back after this field reports a change, that reformats the entry
  // under the caret mid-typing ("2020-02-3" becomes "2020-02-03") and then wipes
  // the lot on the keystroke that makes it invalid, so a rejected date vanishes
  // instead of showing its error. While the user is typing, though, the page has
  // nothing new to say — it is repeating what this field told it a keystroke
  // ago. So an echo of this field's own last report is withheld and Carbon keeps
  // the value it already had; anything the page actually originates (R11's end
  // date auto-filled from the time frame, a restored filter) still goes through.
  // `seen` is the page's own last word, tracked separately from the value handed
  // over, because withholding an echo deliberately leaves the two out of step.
  const [picker, setPicker] = useState(() => ({ value: normalisedValue, seen: incomingKey, clears: 0 }));
  if (incomingKey !== picker.seen) {
    const isEcho = incomingKey === lastReportedKey;
    setPicker((prev) => ({
      value: isEcho ? prev.value : normalisedValue,
      seen: incomingKey,
      clears: !isEcho && !incomingKey ? prev.clears + 1 : prev.clears,
    }));
    if (!isEcho) {
      // Applied, so whatever this field last said about the date is superseded.
      setLastReportedKey(null);
      // The value replaces whatever the user typed, so a parse failure raised
      // against that old text no longer describes the field — otherwise it sits
      // there red over a date it is displaying correctly. Only a value with a
      // date in it clears the error: a clear leaves the field empty, with
      // nothing to be wrong about, and this runs before Carbon hands the value
      // to Flatpickr, so the guard below never sees a stale error state.
      if (incomingKey) setInputInvalid(false);
    }
  }
  const pickerValue = picker.value;

  // Carbon empties the input only when the value it is holding changes to
  // empty, and withholding an echo can mean it never held the date at all — so
  // a page clearing the field outright (the invoice form resets every field
  // when the URL's invoice id changes) would leave the text sitting there,
  // showing a date the page no longer has. Empty it here instead. This only
  // runs for a clear the page originated: one of this field's own is withheld
  // above, and with it the error state that would otherwise be left stranded.
  useEffect(() => {
    if (!picker.clears) return;
    const el = containerRef.current?.querySelector('input');
    if (el?.value) el.value = '';
  }, [picker.clears]);

  // Flatpickr is created by Carbon's DatePicker one commit AFTER this component
  // first mounts (its init effect waits on internal `hasInput` state), so the
  // instance isn't there to patch during our own mount effect. Installing is
  // therefore attempted from both directions — every render below, and every
  // handler before it does anything — and is a no-op once it has taken.
  const ensureSetDateGuard = useCallback(() => {
    const fp = getFlatpickr(containerRef.current?.querySelector('input'));
    if (!fp || fp.__cspOrigSetDate) return;

    const originalSetDate = fp.setDate;
    fp.__cspOrigSetDate = originalSetDate;
    fp.setDate = function (date: unknown, triggerChange?: boolean, format?: string) {
      // Refuse raw text Flatpickr would roll over into a different date, and
      // nothing else: a string this field's own parser accepts says the same
      // thing Flatpickr would, and Dates (the calendar's picks, the reformat
      // round-trip, a value from the page) carry no text to misread. Judging
      // the payload rather than the field's error state matters — a page
      // pushing a new value arrives while that state still describes the text
      // it is replacing, and refusing it would drop the value on the floor.
      if (hasUnparseableString(date, dateFormatRef.current)) return;
      originalSetDate.call(fp, date, triggerChange, format);
    };
  }, []);

  const validateValue = useCallback(
    (val: string) => {
      if (!val) {
        setInputInvalid(false);
        report([]);
        return;
      }

      const parsed = parseDateInput(val, dateFormat);

      if (parsed === 'invalid') {
        setInputInvalid(true);
        report([]);
        return;
      }

      setInputInvalid(false);

      if (!parsed) {
        // Unrecognised / incomplete (e.g. mid-typing) — clear without warning.
        report([]);
        return;
      }

      report([parsed]);

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
    [dateFormat, report],
  );

  // Flatpickr exists from the commit after this component's first, so this is a
  // no-op once and then installs it — on every render, like the handlers do, so
  // nothing has to have been typed first for a setDate arriving from elsewhere
  // to be judged. Anything earlier still than that is Flatpickr parsing its own
  // `defaultDate`, which `strictParseDate` covers.
  useEffect(() => {
    ensureSetDateGuard();
  });

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const el = container.querySelector('input') as HTMLInputElement | null;
    if (!el) return;
    dateFormatRef.current = dateFormat;
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
      if (entryState(el.value, dateFormat) !== 'rejected') return;
      setInputInvalid(true);
    };

    const keyDownHandler = (e: Event) => {
      ensureSetDateGuard();
      if ((e as KeyboardEvent).key !== 'Enter') return;
      // Nothing to reject: an empty field leaves Enter alone so Flatpickr can
      // close the calendar, and a value this field accepts is Flatpickr's to
      // commit — that keeps the calendar selection in step with the text.
      if (entryState(el.value, dateFormat) !== 'rejected') return;
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
    setInputInvalid(false);
    report(dates);
  };

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
        value={pickerValue}
        parseDate={strictParseDate(dateFormat)}
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
