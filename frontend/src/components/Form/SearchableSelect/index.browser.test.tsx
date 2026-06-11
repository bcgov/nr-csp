import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';

import SearchableSelect from './index';

const items = ['Alpha', 'Beta', 'Gamma'];

const setup = (overrides: Partial<React.ComponentProps<typeof SearchableSelect>> = {}) => {
  const onChange = vi.fn();
  render(
    <SearchableSelect
      id="ss"
      titleText="Pick one"
      items={items}
      selectedItem={null}
      onChange={onChange}
      {...overrides}
    />,
  );
  return { onChange, input: screen.getByRole('combobox', { name: /pick one/i }) };
};

describe('SearchableSelect', () => {
  it('renders with the given title', () => {
    setup();
    expect(screen.getByRole('combobox', { name: /pick one/i })).toBeInTheDocument();
  });

  it('shows the selected item value', () => {
    const { input } = setup({ selectedItem: 'Beta' });
    expect(input).toHaveValue('Beta');
  });

  it('calls onChange with the chosen item', async () => {
    const { onChange, input } = setup();
    await userEvent.click(input);
    await userEvent.click(screen.getByRole('option', { name: 'Beta' }));
    expect(onChange).toHaveBeenCalledWith({ selectedItem: 'Beta' });
  });

  it('highlights the matching substring while typing', async () => {
    const { input } = setup();
    await userEvent.type(input, 'lph');
    const match = document.querySelector('.searchable-select__match');
    expect(match).not.toBeNull();
    expect(match).toHaveTextContent('lph');
  });
});
