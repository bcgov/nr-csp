import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import InvoiceStatusTag from './index';

const tagFor = (status: string) => screen.getByText(status).closest('.cds--tag');

describe('InvoiceStatusTag', () => {
  it('renders the status text', () => {
    render(<InvoiceStatusTag status="APP" />);
    expect(screen.getByText('APP')).toBeInTheDocument();
  });

  it('maps a status code to its colour (APP → green, REJ → red)', () => {
    const { rerender } = render(<InvoiceStatusTag status="APP" />);
    expect(tagFor('APP')).toHaveClass('cds--tag--green');

    rerender(<InvoiceStatusTag status="REJ" />);
    expect(tagFor('REJ')).toHaveClass('cds--tag--red');
  });

  it('maps a status description to its colour (Processing → blue)', () => {
    render(<InvoiceStatusTag status="Processing" />);
    expect(tagFor('Processing')).toHaveClass('cds--tag--blue');
  });

  it('falls back to gray for an unrecognised status', () => {
    render(<InvoiceStatusTag status="WHATEVER" />);
    expect(tagFor('WHATEVER')).toHaveClass('cds--tag--gray');
  });

  it('adds the approved modifier class only for approved statuses', () => {
    const { rerender } = render(<InvoiceStatusTag status="APP" />);
    expect(tagFor('APP')).toHaveClass('invoice-status-tag--approved');

    rerender(<InvoiceStatusTag status="DFT" />);
    expect(tagFor('DFT')).not.toHaveClass('invoice-status-tag--approved');
  });
});
