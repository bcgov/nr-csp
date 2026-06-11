import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import * as r06Service from '@/services/r06.service';

import { R06InvoicePrintOutPage } from './index';

const mockAddNotification = vi.fn();

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: mockAddNotification }),
}));

vi.mock('@/services/r06.service', () => ({
  useR06ReportMutation: vi.fn(),
}));

vi.mock('@/services/lookup.service', () => ({
  useMaturityCodesWithCantsQuery: () => ({ data: [], isLoading: false }),
  useInvoiceStatusesQuery: () => ({ data: [], isLoading: false }),
  useInvoiceTypesQuery: () => ({ data: [], isLoading: false }),
}));

const mockUseR06ReportMutation = r06Service.useR06ReportMutation as ReturnType<typeof vi.fn>;

const renderPage = () => {
  const qc = new QueryClient();
  render(
    <QueryClientProvider client={qc}>
      <R06InvoicePrintOutPage />
    </QueryClientProvider>,
  );
};

describe('R06InvoicePrintOutPage', () => {
  beforeEach(() => {
    mockUseR06ReportMutation.mockReturnValue({ mutate: vi.fn(), isPending: false });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title', () => {
    renderPage();
    expect(screen.getByRole('heading', { level: 1, name: /r06.*invoice print out/i })).toBeInTheDocument();
  });

  it('renders Report details section heading', () => {
    renderPage();
    expect(screen.getByRole('heading', { level: 2, name: 'Report details' })).toBeInTheDocument();
  });

  it('renders Add invoice number range section heading', () => {
    renderPage();
    expect(screen.getByRole('heading', { level: 2, name: 'Add invoice number range' })).toBeInTheDocument();
  });

  it('renders date inputs', () => {
    renderPage();
    expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
  });

  it('renders Maturity, Invoice status and Invoice type selects', () => {
    renderPage();
    expect(screen.getByRole('combobox', { name: /maturity/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /invoice status/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /invoice type/i })).toBeInTheDocument();
  });

  it('renders Seller and Buyer text inputs', () => {
    renderPage();
    expect(screen.getByRole('combobox', { name: /seller name/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /seller number/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /buyer name/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /buyer number/i })).toBeInTheDocument();
  });

  it('renders the Submission number input', () => {
    renderPage();
    expect(screen.getByLabelText(/submission number/i)).toBeInTheDocument();
  });

  it('renders Generate PDF, Export CSV and Add another range buttons', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /generate pdf/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /export csv/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add another range/i })).toBeInTheDocument();
  });

  it('adds a second From/To range row when Add another range is clicked', () => {
    renderPage();
    expect(screen.getAllByLabelText('From')).toHaveLength(1);
    fireEvent.click(screen.getByRole('button', { name: /add another range/i }));
    expect(screen.getAllByLabelText('From')).toHaveLength(2);
    expect(screen.getAllByLabelText('To')).toHaveLength(2);
  });

  it('shows loading indicator and hides all action buttons when isPending', () => {
    mockUseR06ReportMutation.mockReturnValue({ mutate: vi.fn(), isPending: true });
    renderPage();
    expect(screen.getByText(/generating/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /generate pdf/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /export csv/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /add another range/i })).not.toBeInTheDocument();
  });

  const fillInvoiceRange = () => {
    fireEvent.change(screen.getByLabelText('From'), { target: { value: '100' } });
    fireEvent.change(screen.getByLabelText('To'), { target: { value: '200' } });
  };

  it('calls addNotification with warning when no data is found', async () => {
    mockUseR06ReportMutation.mockReturnValue({
      mutate: vi.fn().mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
        opts.onError({ response: { status: 404 } });
      }),
      isPending: false,
    });
    renderPage();
    fillInvoiceRange();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    // onError parses the error asynchronously before notifying, so await it.
    await waitFor(() =>
      expect(mockAddNotification).toHaveBeenCalledWith({
        kind: 'warning',
        title: 'No data found. No records matched the selected criteria.',
      }),
    );
  });

  it('calls addNotification with error on general report generation failure', async () => {
    mockUseR06ReportMutation.mockReturnValue({
      mutate: vi.fn().mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
        opts.onError(new Error('Server error'));
      }),
      isPending: false,
    });
    renderPage();
    fillInvoiceRange();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    await waitFor(() =>
      expect(mockAddNotification).toHaveBeenCalledWith({
        kind: 'error',
        title: 'Report generation failed.',
      }),
    );
  });

  it('blocks generation and shows required errors when no dates and no invoice numbers', () => {
    const mutate = vi.fn();
    mockUseR06ReportMutation.mockReturnValue({ mutate, isPending: false });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mutate).not.toHaveBeenCalled();
    expect(screen.getAllByText(/required when no invoice numbers are provided/i).length).toBeGreaterThan(0);
  });

  it('blocks generation and shows error when submission number is non-numeric', () => {
    const mutate = vi.fn();
    mockUseR06ReportMutation.mockReturnValue({ mutate, isPending: false });
    renderPage();
    fillInvoiceRange();
    fireEvent.change(screen.getByLabelText(/submission number/i), { target: { value: 'abc' } });
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mutate).not.toHaveBeenCalled();
    expect(screen.getByText(/submission number must be numeric/i)).toBeInTheDocument();
  });

  it('sends submissionId as a number when provided', () => {
    const mutate = vi.fn();
    mockUseR06ReportMutation.mockReturnValue({ mutate, isPending: false });
    renderPage();
    fillInvoiceRange();
    fireEvent.change(screen.getByLabelText(/submission number/i), { target: { value: '12345' } });
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mutate).toHaveBeenCalledWith(expect.objectContaining({ submissionId: 12345 }), expect.anything());
  });

  it('sends invoice numbers as a flat comma list of from,to pairs', () => {
    const mutate = vi.fn();
    mockUseR06ReportMutation.mockReturnValue({ mutate, isPending: false });
    renderPage();
    fireEvent.change(screen.getByLabelText('From'), { target: { value: '100' } });
    fireEvent.change(screen.getByLabelText('To'), { target: { value: '200' } });
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mutate).toHaveBeenCalledWith(expect.objectContaining({ invoiceNumbers: '100,200' }), expect.anything());
  });

  it('duplicates a single-sided invoice value (matching the old app)', () => {
    const mutate = vi.fn();
    mockUseR06ReportMutation.mockReturnValue({ mutate, isPending: false });
    renderPage();
    fireEvent.change(screen.getByLabelText('From'), { target: { value: '300' } });
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mutate).toHaveBeenCalledWith(expect.objectContaining({ invoiceNumbers: '300,300' }), expect.anything());
  });
});
