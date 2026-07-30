import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import SearchInput from './index';

describe('SearchInput', () => {
  const onChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderInput = (value = '') =>
    render(
      <SearchInput
        id="kw"
        titleText="Keyword search"
        placeholder="Search invoices"
        value={value}
        onChange={onChange}
      />,
    );

  it('renders an accessible search box with the initial value', () => {
    renderInput('initial');

    const input = screen.getByRole('searchbox', { name: /keyword search/i });
    expect(input).toBeInTheDocument();
    expect(input).toHaveValue('initial');
    expect(input).toHaveAttribute('placeholder', 'Search invoices');
  });

  it('commits the typed value on blur', async () => {
    const user = userEvent.setup();
    renderInput();

    const input = screen.getByRole('searchbox', { name: /keyword search/i });
    await user.type(input, 'cedar');
    await user.tab();

    expect(onChange).toHaveBeenCalledWith('cedar');
  });

  it('commits on Enter by blurring the field', async () => {
    const user = userEvent.setup();
    renderInput();

    const input = screen.getByRole('searchbox', { name: /keyword search/i });
    await user.type(input, 'fir{Enter}');

    expect(onChange).toHaveBeenCalledWith('fir');
    expect(input).not.toHaveFocus();
  });
});
