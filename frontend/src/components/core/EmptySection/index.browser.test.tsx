import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { EmptySection } from './index';

describe('EmptySection', () => {
  it('renders with default title when none provided', () => {
    render(<EmptySection />);
    expect(screen.getByText('No data')).toBeInTheDocument();
  });

  it('renders custom title', () => {
    render(<EmptySection title="Nothing here" />);
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
  });

  it('renders description when provided', () => {
    render(<EmptySection title="Empty" description="Try again later" />);
    expect(screen.getByText('Try again later')).toBeInTheDocument();
  });

  it('does not render description element when not provided', () => {
    const { container } = render(<EmptySection title="Empty" />);
    const paragraphs = container.querySelectorAll('p');
    expect(paragraphs).toHaveLength(1);
  });
});
