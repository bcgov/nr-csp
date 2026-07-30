import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';

import SingleSelect from './index';

interface Item {
  code: string;
  description: string;
}

const items: Item[] = [
  { code: 'A', description: 'Alpha' },
  { code: 'B', description: 'Beta' },
];
const itemToString = (item: Item | null) => (item ? `${item.code} - ${item.description}` : '');

const setup = (overrides: Partial<React.ComponentProps<typeof SingleSelect<Item>>> = {}) => {
  const onChange = vi.fn();
  render(
    <SingleSelect<Item>
      id="sel"
      titleText="Pick one"
      label="Select..."
      items={items}
      itemToString={itemToString}
      selectedItem={undefined}
      onChange={onChange}
      {...overrides}
    />,
  );
  return { onChange };
};

describe('SingleSelect', () => {
  it('shows the "Select..." placeholder when nothing is selected', () => {
    setup();
    expect(screen.getByRole('combobox')).toHaveTextContent('Select...');
  });

  it('shows the selected item label when one is provided', () => {
    setup({ selectedItem: items[1] });
    expect(screen.getByRole('combobox')).toHaveTextContent('B - Beta');
  });

  it('calls onChange with the chosen item', async () => {
    const { onChange } = setup();
    await userEvent.click(screen.getByRole('combobox'));
    await userEvent.click(screen.getByRole('option', { name: 'A - Alpha' }));
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ selectedItem: items[0] }));
  });

  it('calls onChange with null when the blank "Select..." option is chosen', async () => {
    const { onChange } = setup({ selectedItem: items[0] });
    await userEvent.click(screen.getByRole('combobox'));
    await userEvent.click(screen.getByRole('option', { name: 'Select...' }));
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ selectedItem: null }));
  });
});
