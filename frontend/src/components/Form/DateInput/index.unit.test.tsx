import { render, screen, fireEvent } from '@testing-library/react';
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

describe('DateInput flatpickr setDate guard', () => {
  it('blocks string setDate calls while the input is invalid but lets Dates through', () => {
    const { input } = setup();
    const origSetDate = installFakeFlatpickr(input);

    // An invalid value re-renders (warn state), which installs the guard.
    typeValue(input, '2026-13-05');
    const fp = getFp(input);
    expect(fp?.__cspOrigSetDate).toBe(origSetDate);

    fp?.setDate('2026-01-01');
    expect(origSetDate).not.toHaveBeenCalled();

    const date = new Date(2026, 0, 1);
    fp?.setDate(date, false);
    expect(origSetDate).toHaveBeenCalledWith(date, false, undefined);
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

describe('DateInput external invalid state', () => {
  it('shows the invalidText and suppresses the internal warning', () => {
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
