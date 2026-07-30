import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

import ActiveMultiSelect from './index';

const ITEMS = ['Apple', 'Banana'];

describe('ActiveMultiSelect', () => {
  it('renders a text-input skeleton when showSkeleton is true', () => {
    const { container } = render(
      <ActiveMultiSelect id="fruits" titleText="Fruits" items={ITEMS} showSkeleton onChange={vi.fn()} />,
    );
    expect(container.querySelector('.cds--skeleton')).toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  it('renders the filterable multi-select when showSkeleton is false', () => {
    render(<ActiveMultiSelect id="fruits" titleText="Fruits" items={ITEMS} onChange={vi.fn()} />);
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    expect(screen.getByText('Fruits')).toBeInTheDocument();
  });

  it('applies the active-multi-select class plus any custom class', () => {
    const { container } = render(
      <ActiveMultiSelect id="fruits" titleText="Fruits" items={ITEMS} className="custom" onChange={vi.fn()} />,
    );
    expect(container.querySelector('.active-multi-select.custom')).toBeInTheDocument();
  });
});
