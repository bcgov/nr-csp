import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import InvoiceStatusTag from './index';

const tagFor = (status: string) => screen.getByText(status).closest('.cds--tag');

describe('InvoiceStatusTag', () => {
  it('renders the status text', () => {
    render(<InvoiceStatusTag status="APP" />);
    expect(screen.getByText('APP')).toBeInTheDocument();
  });

  it('maps a status code to its pill modifier (APP → approved, REJ → rejected)', () => {
    const { rerender } = render(<InvoiceStatusTag status="APP" />);
    expect(tagFor('APP')).toHaveClass('invoice-status-tag--approved');

    rerender(<InvoiceStatusTag status="REJ" />);
    expect(tagFor('REJ')).toHaveClass('invoice-status-tag--rejected');
  });

  it('maps a status description to its pill modifier (Processing → processing)', () => {
    render(<InvoiceStatusTag status="Processing" />);
    expect(tagFor('Processing')).toHaveClass('invoice-status-tag--processing');
  });

  it('applies no status modifier for an unrecognised status', () => {
    render(<InvoiceStatusTag status="WHATEVER" />);
    const tag = tagFor('WHATEVER');
    expect(tag).toHaveClass('invoice-status-tag');
    expect(tag?.className).not.toMatch(/invoice-status-tag--/);
  });
});
