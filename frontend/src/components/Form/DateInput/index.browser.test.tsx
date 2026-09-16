import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';

import DateInput from './index';

const setup = (overrides: Partial<React.ComponentProps<typeof DateInput>> = {}) => {
  const onChange = vi.fn();
  render(<DateInput id="d" labelText="Invoice date" onChange={onChange} {...overrides} />);
  return { onChange, input: screen.getByLabelText('Invoice date') as HTMLInputElement };
};

const lastDate = (onChange: ReturnType<typeof vi.fn>): Date | undefined => {
  const call = [...onChange.mock.calls].reverse().find((c) => Array.isArray(c[0]) && c[0].length === 1);
  return call?.[0][0];
};

// What the field ended up handing the form. Typing character by character emits
// intermediate dates ("2026-02-5" is a valid date on the way to "2026-02-51"),
// so only the final call says whether anything was committed.
const committed = (onChange: ReturnType<typeof vi.fn>): Date[] => (onChange.mock.calls.at(-1)?.[0] ?? []) as Date[];

const allEmitted = (onChange: ReturnType<typeof vi.fn>): Date[] =>
  onChange.mock.calls.flatMap((c) => (c[0] ?? []) as Date[]);

describe('DateInput', () => {
  it('renders the label and default placeholder', () => {
    const { input } = setup();
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('placeholder', 'yyyy-mm-dd');
  });

  it('emits a local-midnight date for a valid typed value', async () => {
    const { input, onChange } = setup();
    await userEvent.type(input, '2026-12-25');
    const d = lastDate(onChange);
    expect(d).toBeInstanceOf(Date);
    expect(d?.getFullYear()).toBe(2026);
    expect(d?.getMonth()).toBe(11); // December (0-based)
    expect(d?.getDate()).toBe(25);
  });

  it('flags an out-of-range date as invalid', async () => {
    const { input } = setup();
    await userEvent.type(input, '2026-13-40');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
  });

  it('uses the yyyy-mm placeholder for the Y-m format', () => {
    const { input } = setup({ dateFormat: 'Y-m' });
    expect(input).toHaveAttribute('placeholder', 'yyyy-mm');
  });

  it('shows an externally provided ISO value in the field', () => {
    const { input } = setup({ value: '2026-03-15' });
    expect(input.value).toContain('2026');
  });

  // Regression: pressing Enter used to hand the raw text to flatpickr's parser,
  // which rolled the overflow forward — "2026-02-51" was silently accepted as
  // 2026-03-23 and the inline warning disappeared.
  it('does not roll an out-of-range day over when Enter is pressed', async () => {
    const { input, onChange } = setup();
    await userEvent.type(input, '2026-02-51');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();

    await userEvent.keyboard('{Enter}');

    expect(input.value).toBe('2026-02-51');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    // Flagged as an error the user has to fix, not a warning.
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(document.querySelector('.cds--date-picker-input__wrapper--warn')).toBeNull();
    expect(committed(onChange)).toEqual([]);
    // Nothing rolled over into March on the way through, either.
    expect(allEmitted(onChange).some((d) => d.getMonth() === 2)).toBe(false);
  });

  it('does not invent a date for an incomplete value on Enter', async () => {
    const { input, onChange } = setup();
    await userEvent.type(input, '2026-1');
    await userEvent.keyboard('{Enter}');

    expect(input.value).toBe('2026-1');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(committed(onChange)).toEqual([]);
  });

  it('still commits a valid typed date on Enter', async () => {
    const { input, onChange } = setup();
    await userEvent.type(input, '2026-02-05');
    await userEvent.keyboard('{Enter}');

    expect(input.value).toBe('2026-02-05');
    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
    const d = lastDate(onChange);
    expect(d?.getFullYear()).toBe(2026);
    expect(d?.getMonth()).toBe(1);
    expect(d?.getDate()).toBe(5);
  });

  it('still accepts a date picked from the calendar, and clears an earlier warning', async () => {
    const { input, onChange } = setup();
    await userEvent.type(input, '2026-02-51');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();

    await userEvent.clear(input);
    await userEvent.click(input);
    const day = document.querySelector('.flatpickr-calendar .flatpickr-day:not(.flatpickr-disabled)');
    expect(day).not.toBeNull();
    await userEvent.click(day as HTMLElement);

    expect(screen.queryByText('Invalid date')).not.toBeInTheDocument();
    expect(committed(onChange)).toHaveLength(1);
    expect(input.value).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('flags an invalid date left in the field when focus moves away', async () => {
    const { input, onChange } = setup();
    await userEvent.type(input, '2026-02-51');
    await userEvent.tab();

    expect(input.value).toBe('2026-02-51');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
    expect(committed(onChange)).toEqual([]);
  });
});
