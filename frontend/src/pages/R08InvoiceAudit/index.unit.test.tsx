import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import * as r08Service from '@/services/r08.service';

import { R08InvoiceAuditPage } from './index';

const mockAddNotification = vi.fn();

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: mockAddNotification }),
}));

vi.mock('@/services/r08.service', () => ({
  useR08ReportMutation: vi.fn(),
}));

vi.mock('@/services/lookup.service', () => ({
  useInvoiceStatusesQuery: () => ({ data: [], isLoading: false }),
  useInvoiceTypesQuery: () => ({ data: [], isLoading: false }),
  useMaturityCodesNoCantsQuery: () => ({ data: [], isLoading: false }),
}));

const mockUseR08ReportMutation = r08Service.useR08ReportMutation as ReturnType<typeof vi.fn>;

const renderPage = () => {
  const qc = new QueryClient();
  render(
    <QueryClientProvider client={qc}>
      <R08InvoiceAuditPage />
    </QueryClientProvider>,
  );
};

describe('R08InvoiceAuditPage', () => {
  beforeEach(() => {
    mockUseR08ReportMutation.mockReturnValue({ mutate: vi.fn(), isPending: false });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title', () => {
    renderPage();
    expect(screen.getByRole('heading', { level: 1, name: /r08.*invoice audit/i })).toBeInTheDocument();
  });

  it('renders all section headings', () => {
    renderPage();
    expect(screen.getByRole('heading', { level: 2, name: 'Report range' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Invoice information' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Submission information' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Client information' })).toBeInTheDocument();
  });

  it('renders report range inputs', () => {
    renderPage();
    expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /time frame/i })).toBeInTheDocument();
  });

  it('renders invoice selects', () => {
    renderPage();
    expect(screen.getByRole('combobox', { name: /invoice type/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /invoice status/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /maturity/i })).toBeInTheDocument();
  });

  it('renders submission number text input', () => {
    renderPage();
    expect(screen.getByLabelText(/submission number/i)).toBeInTheDocument();
  });

  it('renders client text inputs', () => {
    renderPage();
    expect(screen.getByRole('combobox', { name: /seller name/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /seller number/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /buyer name/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /buyer number/i })).toBeInTheDocument();
  });

  it('renders Generate PDF and Export CSV buttons', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /generate pdf/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /export csv/i })).toBeInTheDocument();
  });

  it('shows loading indicator and hides Export CSV when isPending', () => {
    mockUseR08ReportMutation.mockReturnValue({ mutate: vi.fn(), isPending: true });
    renderPage();
    expect(screen.getByText(/generating/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /export csv/i })).not.toBeInTheDocument();
  });

  it('shows an inline error when no filter criteria are provided', () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    // Client-side validation now surfaces inline (form-error banner), not as a toast.
    expect(screen.getByText(/at least one/i)).toBeInTheDocument();
    expect(mockAddNotification).not.toHaveBeenCalled();
  });

  it('shows inline error when submission number is non-numeric', () => {
    renderPage();
    fireEvent.change(screen.getByLabelText(/submission number/i), { target: { value: 'abc' } });
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(screen.getByText(/submission number must be numeric/i)).toBeInTheDocument();
  });

  it('calls addNotification with warning when no data is found', async () => {
    mockUseR08ReportMutation.mockReturnValue({
      mutate: vi.fn().mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
        opts.onError({ response: { status: 404 } });
      }),
      isPending: false,
    });
    renderPage();
    fireEvent.change(screen.getByLabelText(/submission number/i), { target: { value: '123' } });
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
    mockUseR08ReportMutation.mockReturnValue({
      mutate: vi.fn().mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
        opts.onError(new Error('Server error'));
      }),
      isPending: false,
    });
    renderPage();
    fireEvent.change(screen.getByLabelText(/submission number/i), { target: { value: '123' } });
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    await waitFor(() =>
      expect(mockAddNotification).toHaveBeenCalledWith({
        kind: 'error',
        title: 'Report generation failed.',
      }),
    );
  });
});
