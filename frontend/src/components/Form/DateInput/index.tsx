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
const parseDateInput = (val: string, dateFormat: string): ParseResult => {
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

interface FlatpickrInstance {
  setDate: (date: unknown, triggerChange?: boolean, format?: string) => void;
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

  const resolvedPlaceholder = placeholder ?? (dateFormat === 'Y-m' ? 'yyyy-mm' : 'yyyy-mm-dd');

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
      // Passing a Date (not a string) bypasses the setDate guard installed below.
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

    const captureHandler = (e: Event) => {
      e.stopImmediatePropagation();
      flushSync(() => validateValue((e.target as HTMLInputElement).value));
    };

    const blurHandler = (e: Event) => {
      e.stopImmediatePropagation();
    };

    el.addEventListener('input', captureHandler, true);
    el.addEventListener('blur', blurHandler, true);
    return () => {
      el.removeEventListener('input', captureHandler, true);
      el.removeEventListener('blur', blurHandler, true);
    };
  }, [validateValue]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const el = container.querySelector('input') as HTMLInputElement | null;
    if (!el) return;
    const fp = getFlatpickr(el);
    if (!fp || fp.__cspOrigSetDate) return;

    const originalSetDate = fp.setDate;
    fp.__cspOrigSetDate = originalSetDate;
    fp.setDate = function (date: unknown, triggerChange?: boolean, format?: string) {
      if (typeof date === 'string' && inputInvalidRef.current) return;
      originalSetDate.call(fp, date, triggerChange, format);
    };
  });

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

  return (
    <div ref={containerRef}>
      <DatePicker
        datePickerType="single"
        dateFormat={dateFormat}
        className="date-input"
        style={{ width: '100%' }}
        value={normalisedValue}
        invalid={invalid}
        warn={!invalid && inputInvalid}
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
          invalidText={invalidText}
          warnText="Invalid date"
          disabled={disabled}
        />
      </DatePicker>
    </div>
  );
};

export default DateInput;
