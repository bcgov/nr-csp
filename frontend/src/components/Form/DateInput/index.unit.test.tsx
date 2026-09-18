import { render, screen, fireEvent } from '@testing-library/react';
import { useState } from 'react';
import { describe, it, expect, vi } from 'vitest';

import DateInput from './index';

import type React from 'react';

type FlatpickrLike = {
  setDate: (date: unknown, triggerChange?: boolean, format?: string) => void;
  __cspOrigSetDate?: (date: unknown, triggerChange?: boolean, format?: string) => void;
};

const getFp = (input: HTMLInputElement): FlatpickrLike | undefined =>
  (input as HTMLInputElement & { _flatpickr?: FlatpickrLike })._flatpickr;

const installFakeFlatpickr = (input: HTMLInputElement): ReturnType<typeof vi.fn> => {
  const setDate = vi.fn();
  (input as HTMLInputElement & { _flatpickr?: FlatpickrLike })._flatpickr = { setDate };
  return setDate;
};

const setup = (overrides: Partial<React.ComponentProps<typeof DateInput>> = {}) => {
  const onChange = vi.fn();
  const utils = render(<DateInput id="date-input" labelText="Scale date" onChange={onChange} {...overrides} />);
  const input = screen.getByLabelText('Scale date') as HTMLInputElement;
  return { onChange, input, ...utils };
};

const typeValue = (input: HTMLInputElement, value: string) => {
  fireEvent.input(input, { target: { value } });
};

// Enter is the commit gesture Carbon's fixEventsPlugin and flatpickr both hook,
// each of which hands the raw text to flatpickr's rolling parser.
const pressEnter = (input: HTMLInputElement) => {
  fireEvent.keyDown(input, { key: 'Enter', keyCode: 13, code: 'Enter' });
};

const lastArgs = (onChange: ReturnType<typeof vi.fn>): Date[] => onChange.mock.calls.at(-1)?.[0] as Date[];

describe('DateInput rendering', () => {
  it('renders the label and the default yyyy-mm-dd placeholder', () => {
    const { input } = setup();
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('placeholder', 'yyyy-mm-dd');
  });

  it('uses the yyyy-mm placeholder for the Y-m format', () => {
    const { input } = setup({ dateFormat: 'Y-m' });
    expect(input).toHaveAttribute('placeholder', 'yyyy-mm');
  });

  it('prefers an explicit placeholder over the derived one', () => {
    const { input } = setup({ placeholder: 'pick a date' });
    expect(input).toHaveAttribute('placeholder', 'pick a date');
  });
});

describe('DateInput typed values (Y-m-d)', () => {
  it('emits a local-midnight date for a valid typed value', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-12-25');
    const dates = lastArgs(onChange);
    expect(dates).toHaveLength(1);
    expect(dates[0].getFullYear()).toBe(2026);
    expect(dates[0].getMonth()).toBe(11);
    expect(dates[0].getDate()).toBe(25);
    expect(dates[0].getHours()).toBe(0);
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
  });

  it('emits an empty array and no warning while a date is mid-typed', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-12');
    expect(lastArgs(onChange)).toEqual([]);
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
  });

  it('warns on an out-of-range month and emits an empty array', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-13-05');
    expect(lastArgs(onChange)).toEqual([]);
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
  });

  it('warns on a day that does not exist in the month', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-02-30');
    expect(lastArgs(onChange)).toEqual([]);
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
  });

  it('clears the warning and emits an empty array when the field is emptied', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-13-05');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    typeValue(input, '');
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([]);
  });

  it('recovers from an invalid value once a valid one is typed', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-13-05');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    typeValue(input, '2026-11-05');
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([new Date(2026, 10, 5)]);
  });

  it('re-displays an autofilled slash-separated date through flatpickr setDate', () => {
    const { input, onChange } = setup();
    const setDate = installFakeFlatpickr(input);
    typeValue(input, '2026/12/25');
    expect(lastArgs(onChange)).toEqual([new Date(2026, 11, 25)]);
    expect(setDate).toHaveBeenCalledTimes(1);
    expect(setDate.mock.calls[0][0]).toEqual(new Date(2026, 11, 25));
    expect(setDate.mock.calls[0][1]).toBe(false);
  });

  it('does not round-trip a native-shape value through flatpickr', () => {
    const { input } = setup();
    const setDate = installFakeFlatpickr(input);
    typeValue(input, '2026-12-2');
    expect(setDate).not.toHaveBeenCalled();
  });

  it('stops blur events from reaching other listeners without crashing', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-12-25');
    fireEvent.blur(input);
    expect(lastArgs(onChange)).toEqual([new Date(2026, 11, 25)]);
  });
});

describe('DateInput typed values (Y-m)', () => {
  it('emits the first of the month for a valid yyyy-mm value', () => {
    const { input, onChange } = setup({ dateFormat: 'Y-m' });
    typeValue(input, '2026-05');
    expect(lastArgs(onChange)).toEqual([new Date(2026, 4, 1)]);
  });

  it('warns on an out-of-range month', () => {
    const { input, onChange } = setup({ dateFormat: 'Y-m' });
    typeValue(input, '2026-00');
    expect(lastArgs(onChange)).toEqual([]);
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
  });

  it('treats an incomplete value as mid-typing without warning', () => {
    const { input, onChange } = setup({ dateFormat: 'Y-m' });
    typeValue(input, '2026');
    expect(lastArgs(onChange)).toEqual([]);
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
  });

  it('re-displays an autofilled yyyy/mm value through flatpickr setDate', () => {
    const { input, onChange } = setup({ dateFormat: 'Y-m' });
    const setDate = installFakeFlatpickr(input);
    typeValue(input, '2026/05');
    expect(lastArgs(onChange)).toEqual([new Date(2026, 4, 1)]);
    expect(setDate.mock.calls[0][0]).toEqual(new Date(2026, 4, 1));
  });
});

describe('DateInput commit on Enter', () => {
  // Flatpickr's parser rolls overflow forward, so before the guard "2026-02-51"
  // was silently committed as 2026-03-23 with the warning cleared.
  it('keeps an out-of-range day rejected instead of rolling it over', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-02-51');
    pressEnter(input);
    expect(input.value).toBe('2026-02-51');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([]);
    expect(onChange.mock.calls.flatMap((c) => c[0] as Date[])).toEqual([]);
  });

  it('keeps an out-of-range month rejected instead of rolling it into the next year', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-13-05');
    pressEnter(input);
    expect(input.value).toBe('2026-13-05');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([]);
  });

  it('keeps a day that does not exist in the month rejected', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-02-30');
    pressEnter(input);
    expect(input.value).toBe('2026-02-30');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([]);
  });

  it('flags an incomplete value rather than inventing the missing parts', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-1');
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
    pressEnter(input);
    expect(input.value).toBe('2026-1');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([]);
  });

  it('flags text that is not a date at all', () => {
    const { input, onChange } = setup();
    typeValue(input, 'not a date');
    pressEnter(input);
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([]);
  });

  it('commits a valid value', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-12-25');
    pressEnter(input);
    expect(input.value).toBe('2026-12-25');
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([new Date(2026, 11, 25)]);
  });

  it('flags an out-of-range month on Enter for the Y-m format', () => {
    const { input, onChange } = setup({ dateFormat: 'Y-m' });
    typeValue(input, '2026-13');
    pressEnter(input);
    expect(input.value).toBe('2026-13');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([]);
  });

  it('recovers once the value is corrected and re-committed', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-02-51');
    pressEnter(input);
    typeValue(input, '2026-02-05');
    pressEnter(input);
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([new Date(2026, 1, 5)]);
  });
});

describe('DateInput commit on blur', () => {
  it('flags an incomplete value left in the field', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-1');
    fireEvent.blur(input);
    expect(input.value).toBe('2026-1');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([]);
  });

  it('leaves an invalid value rejected rather than rolling it over', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-02-51');
    fireEvent.blur(input);
    expect(input.value).toBe('2026-02-51');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(lastArgs(onChange)).toEqual([]);
  });

  it('does not warn when the field is left empty', () => {
    const { input, onChange } = setup();
    fireEvent.blur(input);
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
    // Blur is not an edit: pages read any onChange as one (clearing the field's
    // validation error, unlinking R11's time frame), so visiting a field and
    // leaving must report nothing at all.
    expect(onChange).not.toHaveBeenCalled();
  });

  it('does not re-report a value that has not changed', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-12-25');
    expect(onChange).toHaveBeenCalledTimes(1);

    fireEvent.blur(input);

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(lastArgs(onChange)).toEqual([new Date(2026, 11, 25)]);
  });

  it('does not report anything when an invalid value is blurred twice', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-02-51');
    const callsAfterTyping = onChange.mock.calls.length;

    fireEvent.blur(input);
    fireEvent.blur(input);

    expect(onChange).toHaveBeenCalledTimes(callsAfterTyping);
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
  });
});

describe('DateInput flatpickr setDate guard', () => {
  it('refuses text it cannot parse but lets Dates through', () => {
    const { input } = setup();
    const origSetDate = installFakeFlatpickr(input);

    typeValue(input, '2026-13-05');
    const fp = getFp(input);
    expect(fp?.__cspOrigSetDate).toBe(origSetDate);

    fp?.setDate('2026-02-51');
    expect(origSetDate).not.toHaveBeenCalled();

    const date = new Date(2026, 0, 1);
    fp?.setDate(date, false);
    expect(origSetDate).toHaveBeenCalledWith(date, false, undefined);
  });

  // The guard judges the payload, not the field's error state. A page pushing a
  // new value arrives while that state still describes the text being replaced,
  // so weighing the two together would drop the value on the floor.
  it('lets a date the page pushes through while the field is showing an error', () => {
    const { input } = setup();
    const origSetDate = installFakeFlatpickr(input);
    typeValue(input, '2026-13-05');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();

    getFp(input)?.setDate('2026-01-01', false);

    expect(origSetDate).toHaveBeenCalledWith('2026-01-01', false, undefined);
  });

  it('allows string setDate calls once the input becomes valid again', () => {
    const { input } = setup();
    const origSetDate = installFakeFlatpickr(input);
    typeValue(input, '2026-13-05');
    typeValue(input, '2026-11-05');
    const fp = getFp(input);
    fp?.setDate('2026-11-05', false);
    expect(origSetDate).toHaveBeenCalledWith('2026-11-05', false, undefined);
  });

  it('blocks day-overflow text handed over as an array, the shape Carbon commits on Enter', () => {
    const { input } = setup();
    const origSetDate = installFakeFlatpickr(input);
    typeValue(input, '2026-12-25');
    const fp = getFp(input);
    expect(fp?.__cspOrigSetDate).toBe(origSetDate);
    origSetDate.mockClear();

    fp?.setDate(['2026-02-51'], true, 'Y-m-d');
    fp?.setDate('2026-02-51', true, 'Y-m-d');
    fp?.setDate(['2026-1'], true, 'Y-m-d');
    expect(origSetDate).not.toHaveBeenCalled();

    fp?.setDate(['2026-02-05'], true, 'Y-m-d');
    expect(origSetDate).toHaveBeenCalledWith(['2026-02-05'], true, 'Y-m-d');
  });

  it('installs the guard on every render, not only once something is typed', () => {
    const { input, rerender } = setup();
    const origSetDate = installFakeFlatpickr(input);

    // No interaction at all — a re-render alone has to be enough.
    rerender(<DateInput id="date-input" labelText="Scale date" />);

    expect(getFp(input)?.__cspOrigSetDate).toBe(origSetDate);
  });

  it('is installed without waiting for a re-render', () => {
    const { input } = setup();
    const origSetDate = installFakeFlatpickr(input);
    // A valid value causes no state change, so nothing re-renders the component.
    typeValue(input, '2026-12-25');
    expect(getFp(input)?.__cspOrigSetDate).toBe(origSetDate);
  });

  it('does not re-patch setDate on subsequent renders', () => {
    const { input } = setup();
    const origSetDate = installFakeFlatpickr(input);
    typeValue(input, '2026-13-05');
    const patched = getFp(input)?.setDate;
    typeValue(input, '2026-12-05');
    typeValue(input, '2026-13-05');
    expect(getFp(input)?.setDate).toBe(patched);
    expect(getFp(input)?.__cspOrigSetDate).toBe(origSetDate);
  });
});

describe('DateInput incoming value normalisation', () => {
  // Flatpickr resolves `defaultDate` as it is constructed, before anything here
  // can vet it, so a string the field cannot read must never be handed over —
  // Flatpickr's own parser would roll it forward into a date nobody asked for.
  it('shows nothing rather than a rolled-over date for a value it cannot read', () => {
    const { input } = setup({ value: '2026-02-51' });
    expect(input.value).toBe('');
  });

  it('accepts a slash-separated value', () => {
    const { input } = setup({ value: '2026/03/15' });
    expect(input.value).toBe('2026-03-15');
  });

  it('shows an ISO value in the field', () => {
    const { input } = setup({ value: '2026-03-15' });
    expect(input.value).toBe('2026-03-15');
  });

  it('renders with an ISO string value', () => {
    const { input } = setup({ value: '2026-03-15' });
    expect(input).toBeInTheDocument();
  });

  it('renders with a Date value', () => {
    const { input } = setup({ value: new Date(2026, 2, 15) });
    expect(input).toBeInTheDocument();
  });

  it('renders with a non-ISO string value passed through as-is', () => {
    const { input } = setup({ value: '2026/03/15' });
    expect(input).toBeInTheDocument();
  });

  it('renders with an array value', () => {
    const { input } = setup({ value: ['2026-03-15', new Date(2026, 2, 16)] });
    expect(input).toBeInTheDocument();
  });
});

// Mirrors what the report pages, Search, Inbox and the invoice form do: hold the
// date in page state and hand it straight back to the field. `asIsoString` covers
// Search/Inbox, which keep it as `yyyy-mm-dd` rather than a Date.
const Controlled = ({ onChange, asIsoString }: { onChange?: (dates: Date[]) => void; asIsoString?: boolean }) => {
  const [held, setHeld] = useState<Date | string | null>(null);
  const toHeld = (d: Date | undefined) => {
    if (!d) return null;
    if (!asIsoString) return d;
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };
  return (
    <>
      <button onClick={() => setHeld(toHeld(new Date(2026, 4, 20)))}>push</button>
      <button onClick={() => setHeld(null)}>clear</button>
      <DateInput
        id="date-input"
        labelText="Scale date"
        value={held ?? undefined}
        onChange={(dates) => {
          setHeld(toHeld(dates[0]));
          onChange?.(dates);
        }}
      />
    </>
  );
};

describe('DateInput value pushed in by the parent', () => {
  // R11/R12/R07/R08/R10/R13 auto-fill the end date from the time frame. That
  // value replaces the text the user typed, so an error raised against the old
  // text must not be left sitting over the date now on display.
  it('clears a parse failure when a valid value arrives', () => {
    const { input, rerender } = setup({ value: new Date(2026, 0, 1) });
    typeValue(input, '2026-02-51');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();

    rerender(<DateInput id="date-input" labelText="Scale date" value={new Date(2026, 0, 31)} />);

    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
    expect(input).not.toHaveAttribute('aria-invalid');
  });

  // The page feeding a value straight back is how an invalid entry used to
  // vanish: Carbon hands that value to Flatpickr, which rewrites the input's
  // text and blanks it when the value is empty. The field withholds its own
  // echo, so the text belongs to whoever is typing it.
  it('keeps what the user typed when an entry turns invalid mid-typing', () => {
    render(<Controlled />);
    const input = screen.getByLabelText('Scale date') as HTMLInputElement;

    // "2020-02-3" is itself a complete, valid date, so the page stores it and
    // hands it back — which used to reformat the entry under the caret.
    typeValue(input, '2020-02-3');
    expect(input.value).toBe('2020-02-3');

    typeValue(input, '2020-02-33');
    expect(input.value).toBe('2020-02-33');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(input).toHaveAttribute('aria-invalid', 'true');
  });

  it('keeps what the user typed when the page holds the date as an ISO string', () => {
    render(<Controlled asIsoString />);
    const input = screen.getByLabelText('Scale date') as HTMLInputElement;

    typeValue(input, '2020-02-3');
    expect(input.value).toBe('2020-02-3');
    typeValue(input, '2020-02-33');
    expect(input.value).toBe('2020-02-33');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
  });

  it('commits nothing while the entry is invalid', () => {
    const onChange = vi.fn();
    render(<Controlled onChange={onChange} />);
    const input = screen.getByLabelText('Scale date') as HTMLInputElement;

    typeValue(input, '2020-02-3');
    typeValue(input, '2020-02-33');
    pressEnter(input);

    expect(lastArgs(onChange)).toEqual([]);
    expect(onChange.mock.calls.flatMap((c) => c[0] as Date[]).some((d) => d.getMonth() === 2)).toBe(false);
  });

  it('applies a value the page originates over a rejected entry', () => {
    render(<Controlled />);
    const input = screen.getByLabelText('Scale date') as HTMLInputElement;
    typeValue(input, '2026-02-51');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();

    // What R11's end-date auto-fill does when the time frame changes.
    fireEvent.click(screen.getByRole('button', { name: 'push' }));

    expect(input.value).toBe('2026-05-20');
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
  });

  // The invoice form resets every field when the URL's invoice id changes. The
  // date the user typed was never handed to Carbon (it was this field's own
  // echo), so Carbon has nothing to clear and the text has to go another way.
  it('empties the field when the page clears a date the user typed', () => {
    render(<Controlled />);
    const input = screen.getByLabelText('Scale date') as HTMLInputElement;
    typeValue(input, '2026-02-05');
    expect(input.value).toBe('2026-02-05');

    fireEvent.click(screen.getByRole('button', { name: 'clear' }));

    expect(input.value).toBe('');
  });

  it('still empties the field when the page clears a value it set itself', () => {
    render(<Controlled />);
    const input = screen.getByLabelText('Scale date') as HTMLInputElement;
    fireEvent.click(screen.getByRole('button', { name: 'push' }));
    expect(input.value).toBe('2026-05-20');

    fireEvent.click(screen.getByRole('button', { name: 'clear' }));

    expect(input.value).toBe('');
  });
});

describe('DateInput surrounding whitespace', () => {
  it('accepts a pasted value with a trailing space', () => {
    const { input, onChange } = setup();
    typeValue(input, '2026-02-05 ');
    expect(lastArgs(onChange)).toEqual([new Date(2026, 1, 5)]);
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
  });

  it('does not flag a whitespace-only field on commit', () => {
    const { input } = setup();
    typeValue(input, '   ');
    pressEnter(input);
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
  });

  it('still rejects a padded invalid date', () => {
    const { input } = setup();
    typeValue(input, ' 2026-02-51 ');
    pressEnter(input);
    expect(input.value).toBe(' 2026-02-51 ');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
  });
});

describe('DateInput invalid presentation', () => {
  // An unparseable value is never submitted, so it has to read as an error the
  // user must fix — not a warning they can carry on past.
  it('renders a parse failure as an error, not a warning', () => {
    const { input, container } = setup();
    typeValue(input, '2026-02-51');

    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAttribute('data-invalid');
    expect(container.querySelector('.cds--date-picker-input__wrapper--invalid')).not.toBeNull();
    expect(container.querySelector('.cds--date-picker__icon--invalid')).not.toBeNull();
    expect(container.querySelector('.cds--date-picker-input__wrapper--warn')).toBeNull();
    expect(container.querySelector('.cds--date-picker__icon--warn')).toBeNull();
  });

  it('marks the field invalid when an incomplete value is committed', () => {
    const { input, container } = setup();
    typeValue(input, '2026-1');
    expect(input).not.toHaveAttribute('aria-invalid');

    pressEnter(input);

    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(container.querySelector('.cds--date-picker-input__wrapper--warn')).toBeNull();
  });

  it('drops the error state once a valid date is typed', () => {
    const { input, container } = setup();
    typeValue(input, '2026-02-51');
    typeValue(input, '2026-02-05');

    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(container.querySelector('.cds--date-picker-input__wrapper--invalid')).toBeNull();
  });
});

describe('DateInput external invalid state', () => {
  it('shows the external invalidText in place of the internal message', () => {
    const { input } = setup({ invalid: true, invalidText: 'Date is required.' });
    typeValue(input, '2026-13-05');
    expect(screen.getByText('Date is required.')).toBeInTheDocument();
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
  });

  it('renders a disabled input when disabled', () => {
    const { input } = setup({ disabled: true });
    expect(input).toBeDisabled();
  });

  it('works without an onChange handler', () => {
    render(<DateInput id="no-handler" labelText="No handler" />);
    const input = screen.getByLabelText('No handler') as HTMLInputElement;
    typeValue(input, '2026-12-25');
    typeValue(input, '2026-13-05');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
  });
});
