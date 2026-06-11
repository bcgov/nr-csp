import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import SelectAllMultiSelect from './index';

// ── Test fixtures ──────────────────────────────────────────────────────────────

type Fruit = { id: string; name: string };

const FRUITS: Fruit[] = [
  { id: 'apple', name: 'Apple' },
  { id: 'banana', name: 'Banana' },
  { id: 'cherry', name: 'Cherry' },
];

const itemToString = (item: Fruit | null) => item?.name ?? '';
const itemToKey = (item: Fruit) => item.id;

interface RenderOpts {
  selectedItems?: Fruit[];
  onChange?: ReturnType<typeof vi.fn>;
  disabled?: boolean;
  hideLabel?: boolean;
}

function renderSelect({ selectedItems = [], onChange = vi.fn(), disabled, hideLabel }: RenderOpts = {}) {
  render(
    <SelectAllMultiSelect
      id="fruit-select"
      titleText="Favourite fruit"
      items={FRUITS}
      selectedItems={selectedItems}
      itemToString={itemToString}
      itemToKey={itemToKey}
      onChange={onChange}
      disabled={disabled}
      hideLabel={hideLabel}
    />,
  );
  return { onChange };
}

// ── Tag chips ──────────────────────────────────────────────────────────────────

describe('SelectAllMultiSelect — tag chips', () => {
  it('renders a tag for each selected item', () => {
    renderSelect({ selectedItems: [FRUITS[0], FRUITS[1]] });
    expect(screen.getByText('Apple')).toBeInTheDocument();
    expect(screen.getByText('Banana')).toBeInTheDocument();
  });

  it('renders no tags when nothing is selected', () => {
    renderSelect({ selectedItems: [] });
    expect(screen.queryByText('Apple')).not.toBeInTheDocument();
  });

  it('calls onChange without the dismissed item when its × button is clicked', () => {
    const onChange = vi.fn();
    renderSelect({ selectedItems: [FRUITS[0], FRUITS[1]], onChange });
    // Carbon DismissibleTag exposes the close button via aria-label (not title)
    const removeButtons = screen.getAllByRole('button', { name: 'Remove' });
    fireEvent.click(removeButtons[0]); // dismiss Apple
    expect(onChange).toHaveBeenCalledWith({ selectedItems: [FRUITS[1]] });
  });
});

// ── Clear all button ───────────────────────────────────────────────────────────

describe('SelectAllMultiSelect — Clear all', () => {
  it('is not rendered when no items are selected', () => {
    renderSelect({ selectedItems: [] });
    expect(screen.queryByRole('button', { name: 'Clear all' })).not.toBeInTheDocument();
  });

  it('is rendered when at least one item is selected', () => {
    renderSelect({ selectedItems: [FRUITS[0]] });
    expect(screen.getByRole('button', { name: 'Clear all' })).toBeInTheDocument();
  });

  it('calls onChange with an empty array when clicked', () => {
    const onChange = vi.fn();
    renderSelect({ selectedItems: [FRUITS[0], FRUITS[2]], onChange });
    fireEvent.click(screen.getByRole('button', { name: 'Clear all' }));
    expect(onChange).toHaveBeenCalledWith({ selectedItems: [] });
  });
});

// ── Label visibility ───────────────────────────────────────────────────────────

describe('SelectAllMultiSelect — label', () => {
  it('shows the titleText as a visible label by default', () => {
    renderSelect();
    // Carbon renders the label; it should be present and NOT have the visually-hidden class
    const label = screen.getByText('Favourite fruit');
    expect(label).not.toHaveClass('cds--visually-hidden');
  });

  it('visually hides the label when hideLabel is true', () => {
    renderSelect({ hideLabel: true });
    const label = screen.getByText('Favourite fruit');
    expect(label).toHaveClass('cds--visually-hidden');
  });
});

// ── Disabled ───────────────────────────────────────────────────────────────────

describe('SelectAllMultiSelect — disabled', () => {
  it('disables the combobox trigger when disabled is true', () => {
    renderSelect({ disabled: true });
    // Carbon's FilterableMultiSelect renders the field as a button/input that
    // gains the disabled attribute.
    const combobox = screen.getByRole('combobox', { name: /favourite fruit/i });
    expect(combobox).toBeDisabled();
  });

  beforeEach(() => {
    vi.clearAllMocks();
  });
});
