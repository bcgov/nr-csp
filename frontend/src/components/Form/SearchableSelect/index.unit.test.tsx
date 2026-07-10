import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

import SearchableSelect from './index';

const ITEMS = ['Apple', 'Banana', 'Cherry'];

interface RenderOpts {
  selectedItem?: string | null;
  onChange?: ReturnType<typeof vi.fn>;
  disabled?: boolean;
  invalid?: boolean;
  invalidText?: React.ReactNode;
  className?: string;
  label?: string;
}

function renderSelect({ onChange = vi.fn(), ...rest }: RenderOpts = {}) {
  const { container } = render(
    <SearchableSelect
      id="fruit-select"
      titleText="Favourite fruit"
      items={ITEMS}
      onChange={onChange}
      {...rest}
    />,
  );
  const input = screen.getByRole('combobox') as HTMLInputElement;
  return { onChange, input, container };
}

describe('SearchableSelect — rendering', () => {
  it('renders the title text as a label', () => {
    renderSelect();
    expect(screen.getByText('Favourite fruit')).toBeInTheDocument();
  });

  it('uses the label prop as the placeholder', () => {
    const { input } = renderSelect({ label: 'Choose a fruit' });
    expect(input).toHaveAttribute('placeholder', 'Choose a fruit');
  });

  it('defaults the placeholder to an empty string', () => {
    const { input } = renderSelect();
    expect(input).toHaveAttribute('placeholder', '');
  });

  it('applies the searchable-select class plus any custom class', () => {
    const { container } = renderSelect({ className: 'extra' });
    expect(container.querySelector('.searchable-select.extra')).toBeInTheDocument();
  });

  it('disables the input when disabled', () => {
    const { input } = renderSelect({ disabled: true });
    expect(input).toBeDisabled();
  });

  it('shows invalidText when invalid', () => {
    renderSelect({ invalid: true, invalidText: 'Pick a fruit.' });
    expect(screen.getByText('Pick a fruit.')).toBeInTheDocument();
  });
});

describe('SearchableSelect — query highlighting', () => {
  it('renders items without a highlight when no query has been typed', () => {
    const { input, container } = renderSelect();
    fireEvent.click(input);
    expect(screen.getAllByRole('option')).toHaveLength(ITEMS.length);
    expect(container.querySelectorAll('.searchable-select__match')).toHaveLength(0);
  });

  it('highlights the matching substring of each matching item', () => {
    const { input, container } = renderSelect();
    fireEvent.change(input, { target: { value: 'an' } });
    const matches = [...container.querySelectorAll('.searchable-select__match')];
    expect(matches.length).toBeGreaterThan(0);
    expect(matches.every((m) => m.textContent?.toLowerCase() === 'an')).toBe(true);
  });

  it('matches case-insensitively but preserves the item casing', () => {
    const { input, container } = renderSelect();
    fireEvent.change(input, { target: { value: 'BAN' } });
    const match = container.querySelector('.searchable-select__match');
    expect(match).toBeInTheDocument();
    expect(match?.textContent).toBe('Ban');
  });

  it('renders non-matching items as plain text', () => {
    const { input } = renderSelect();
    fireEvent.change(input, { target: { value: 'zzz' } });
    // Nothing matches, so no highlight span is rendered for any option.
    for (const option of screen.getAllByRole('option')) {
      expect(option.querySelector('.searchable-select__match')).toBeNull();
    }
  });
});

describe('SearchableSelect — selection', () => {
  it('calls onChange with the picked item', () => {
    const { input, onChange } = renderSelect();
    fireEvent.click(input);
    fireEvent.click(screen.getByRole('option', { name: 'Banana' }));
    expect(onChange).toHaveBeenCalledWith({ selectedItem: 'Banana' });
  });

  it('coalesces a cleared selection to null', () => {
    const { onChange } = renderSelect({ selectedItem: 'Apple' });
    fireEvent.click(screen.getByRole('button', { name: /clear selected item/i }));
    expect(onChange).toHaveBeenCalledWith({ selectedItem: null });
  });

  it('shows the provided selectedItem in the input', () => {
    const { input } = renderSelect({ selectedItem: 'Cherry' });
    expect(input.value).toBe('Cherry');
  });
});
