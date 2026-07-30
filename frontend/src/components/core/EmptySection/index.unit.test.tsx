import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { EmptySection } from './index';

describe('EmptySection', () => {
  it('renders the default title when none is provided', () => {
    render(<EmptySection />);
    expect(screen.getByText('No data')).toBeInTheDocument();
  });

  it('renders a custom title', () => {
    render(<EmptySection title="Nothing here" />);
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
    expect(screen.queryByText('No data')).not.toBeInTheDocument();
  });

  it('renders the description when provided', () => {
    render(<EmptySection title="Nothing here" description="Try adjusting your filters" />);
    expect(screen.getByText('Try adjusting your filters')).toBeInTheDocument();
  });

  it('omits the description paragraph when not provided', () => {
    const { container } = render(<EmptySection />);
    expect(container.querySelectorAll('p')).toHaveLength(1);
  });
});
