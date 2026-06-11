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

describe('DateInput', () => {
  it('renders the label and default placeholder', () => {
    const { input } = setup();
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('placeholder', 'mm/dd/yyyy');
  });

  it('emits a local-midnight date for a valid typed value', async () => {
    const { input, onChange } = setup();
    await userEvent.type(input, '12/25/2026');
    const d = lastDate(onChange);
    expect(d).toBeInstanceOf(Date);
    expect(d?.getFullYear()).toBe(2026);
    expect(d?.getMonth()).toBe(11); // December (0-based)
    expect(d?.getDate()).toBe(25);
  });

  it('flags an out-of-range date as invalid', async () => {
    const { input } = setup();
    await userEvent.type(input, '13/40/2026');
    expect(screen.getByText('Invalid date')).toBeInTheDocument();
  });

  it('uses the yyyy/mm placeholder for the Y/m format', () => {
    const { input } = setup({ dateFormat: 'Y/m' });
    expect(input).toHaveAttribute('placeholder', 'yyyy/mm');
  });

  it('shows an externally provided ISO value in the field', () => {
    const { input } = setup({ value: '2026-03-15' });
    expect(input.value).toContain('2026');
  });
});
