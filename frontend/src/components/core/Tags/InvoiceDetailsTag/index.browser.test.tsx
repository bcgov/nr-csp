import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import InvoiceDetailsTag from './index';

describe('InvoiceDetailsTag', () => {
  it('renders the label text', () => {
    render(<InvoiceDetailsTag label="New" />);
    expect(screen.getByText('New')).toBeInTheDocument();
  });

  it('renders as a green Carbon tag with the component class', () => {
    render(<InvoiceDetailsTag label="New" />);
    const tag = screen.getByText('New').closest('.cds--tag');
    expect(tag).not.toBeNull();
    expect(tag).toHaveClass('cds--tag--green');
    expect(tag).toHaveClass('invoice-details-tag');
  });
});
