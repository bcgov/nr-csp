import { render, screen, fireEvent } from '@testing-library/react';
import { act } from 'react';
import { describe, it, expect, vi } from 'vitest';

import TaggedMultiSelect from './index';

const items = [
  { code: 'A', description: 'Alpha' },
  { code: 'B', description: 'Beta' },
  { code: 'C', description: 'Gamma' },
];

const itemToString = (item: (typeof items)[number] | null) => (item ? `${item.code} - ${item.description}` : '');

describe('TaggedMultiSelect', () => {
  it('renders a dismissible tag for each selected item', () => {
    render(
      <TaggedMultiSelect
        id="test-tagged"
        placeholder="Select..."
        items={items}
        itemToString={itemToString}
        itemToKey={(item) => item.code}
        selectedItems={[items[0], items[2]]}
        onChange={vi.fn()}
      />,
    );
    expect(screen.getByText('A - Alpha')).toBeInTheDocument();
    expect(screen.getByText('C - Gamma')).toBeInTheDocument();
    expect(screen.queryByText('B - Beta')).not.toBeInTheDocument();
  });

  it('calls onChange without the removed item when its tag is dismissed', async () => {
    const onChange = vi.fn();
    render(
      <TaggedMultiSelect
        id="test-tagged"
        placeholder="Select..."
        items={items}
        itemToString={itemToString}
        itemToKey={(item) => item.code}
        selectedItems={[items[0], items[2]]}
        onChange={onChange}
      />,
    );
    const removeButton = screen.getAllByLabelText('Remove')[0];
    await act(async () => fireEvent.click(removeButton));
    expect(onChange).toHaveBeenCalledWith({ selectedItems: [items[2]] });
  });

  it('renders no tags when nothing is selected', () => {
    render(
      <TaggedMultiSelect
        id="test-tagged"
        placeholder="Select..."
        items={items}
        itemToString={itemToString}
        selectedItems={[]}
        onChange={vi.fn()}
      />,
    );
    expect(screen.queryByLabelText('Remove')).not.toBeInTheDocument();
  });
});
