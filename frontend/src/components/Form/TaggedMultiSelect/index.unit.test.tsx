import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

import TaggedMultiSelect from './index';

type Fruit = { id: string; name: string };

const FRUITS: Fruit[] = [
  { id: 'apple', name: 'Apple' },
  { id: 'banana', name: 'Banana' },
  { id: 'cherry', name: 'Cherry' },
];

const itemToString = (item: Fruit | null) => item?.name ?? '';
const itemToKey = (item: Fruit) => item.id;

describe('TaggedMultiSelect — object items', () => {
  interface Opts {
    selectedItems?: Fruit[];
    onChange?: ReturnType<typeof vi.fn>;
    withItemToKey?: boolean;
  }

  const renderObjects = ({ selectedItems, onChange = vi.fn(), withItemToKey = true }: Opts = {}) => {
    const { container } = render(
      <TaggedMultiSelect
        id="fruit-select"
        titleText="Favourite fruit"
        items={FRUITS}
        selectedItems={selectedItems}
        itemToString={itemToString}
        itemToKey={withItemToKey ? itemToKey : undefined}
        onChange={onChange}
      />,
    );
    return { onChange, container };
  };

  it('renders a dismissible tag for each selected item', () => {
    renderObjects({ selectedItems: [FRUITS[0], FRUITS[2]] });
    expect(screen.getByText('Apple')).toBeInTheDocument();
    expect(screen.getByText('Cherry')).toBeInTheDocument();
    expect(screen.queryByText('Banana')).not.toBeInTheDocument();
  });

  it('renders no tag container when nothing is selected', () => {
    const { container } = renderObjects({ selectedItems: [] });
    expect(container.querySelector('.tagged-multi-select__tags')).toBeNull();
  });

  it('renders no tag container when selectedItems is undefined', () => {
    const { container } = renderObjects();
    expect(container.querySelector('.tagged-multi-select__tags')).toBeNull();
  });

  it('removes only the dismissed item, keyed via itemToKey', () => {
    const onChange = vi.fn();
    renderObjects({ selectedItems: [FRUITS[0], FRUITS[1], FRUITS[2]], onChange });
    const removeButtons = screen.getAllByRole('button', { name: 'Remove' });
    fireEvent.click(removeButtons[1]); // dismiss Banana
    expect(onChange).toHaveBeenCalledWith({ selectedItems: [FRUITS[0], FRUITS[2]] });
  });

  it('falls back to itemToString for keys when itemToKey is not provided', () => {
    const onChange = vi.fn();
    renderObjects({ selectedItems: [FRUITS[0], FRUITS[1]], onChange, withItemToKey: false });
    const removeButtons = screen.getAllByRole('button', { name: 'Remove' });
    fireEvent.click(removeButtons[0]); // dismiss Apple
    expect(onChange).toHaveBeenCalledWith({ selectedItems: [FRUITS[1]] });
  });
});

describe('TaggedMultiSelect — string items without itemToString', () => {
  it('labels tags with String(item) and removes by that label', () => {
    const onChange = vi.fn();
    render(
      <TaggedMultiSelect
        id="letters"
        titleText="Letters"
        items={['A', 'B']}
        selectedItems={['A', 'B']}
        onChange={onChange}
      />,
    );
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText('B')).toBeInTheDocument();
    const removeButtons = screen.getAllByRole('button', { name: 'Remove' });
    fireEvent.click(removeButtons[0]);
    expect(onChange).toHaveBeenCalledWith({ selectedItems: ['B'] });
  });

  it('does not crash when dismissing without an onChange handler', () => {
    render(<TaggedMultiSelect id="letters" titleText="Letters" items={['A']} selectedItems={['A']} />);
    fireEvent.click(screen.getByRole('button', { name: 'Remove' }));
    expect(screen.getByText('A')).toBeInTheDocument();
  });
});

describe('TaggedMultiSelect — field class', () => {
  it('appends the custom className to the field', () => {
    const { container } = render(
      <TaggedMultiSelect id="letters" titleText="Letters" items={['A']} className="custom" onChange={vi.fn()} />,
    );
    expect(container.querySelector('.tagged-multi-select__field.custom')).toBeInTheDocument();
  });
});
