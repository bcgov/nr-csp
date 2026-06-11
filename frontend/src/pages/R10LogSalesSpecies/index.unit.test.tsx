import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import * as r10Service from '@/services/r10.service';
import * as r10Validation from '@/validations/reports/r10';
import { ValidationResult } from '@/validations/validationResult';

import { R10LogSalesSpeciesPage } from './index';

const mockAddNotification = vi.fn();

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: mockAddNotification }),
}));

vi.mock('@/services/r10.service', () => ({
  useR10ReportMutation: vi.fn(),
}));

vi.mock('@/services/lookup.service', () => ({
  useMaturityCodesWithCantsQuery: () => ({ data: [], isLoading: false }),
  useInvoiceTypesQuery: () => ({ data: [], isLoading: false }),
}));

const mockUseR10ReportMutation = r10Service.useR10ReportMutation as ReturnType<typeof vi.fn>;
const mockValidate = vi.spyOn(r10Validation, 'validateR10');

const renderPage = () => {
  const qc = new QueryClient();
  render(
    <QueryClientProvider client={qc}>
      <R10LogSalesSpeciesPage />
    </QueryClientProvider>,
  );
};

describe('R10LogSalesSpeciesPage', () => {
  beforeEach(() => {
    mockUseR10ReportMutation.mockReturnValue({ mutate: vi.fn(), isPending: false });
    mockValidate.mockReturnValue(new ValidationResult([]));
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title', () => {
    renderPage();
    expect(screen.getByRole('heading', { level: 1, name: /r10.*log sales species/i })).toBeInTheDocument();
  });

  it('renders all section headings', () => {
    renderPage();
    expect(screen.getByRole('heading', { level: 2, name: 'Report range' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Invoice information' })).toBeInTheDocument();
  });

  it('renders report range inputs', () => {
    renderPage();
    expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /time frame/i })).toBeInTheDocument();
  });

  it('renders client text inputs', () => {
    renderPage();
    expect(screen.getByRole('combobox', { name: /seller name/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /buyer name/i })).toBeInTheDocument();
  });

  it('renders invoice type and maturity selects', () => {
    renderPage();
    expect(screen.getByRole('combobox', { name: /invoice type/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /maturity/i })).toBeInTheDocument();
  });

  it('renders Generate PDF and Export CSV buttons', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /generate pdf/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /export csv/i })).toBeInTheDocument();
  });

  it('shows loading indicator and hides Export CSV when isPending', () => {
    mockUseR10ReportMutation.mockReturnValue({ mutate: vi.fn(), isPending: true });
    renderPage();
    expect(screen.getByText(/generating/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /export csv/i })).not.toBeInTheDocument();
  });

  it('shows inline error when start date is required', async () => {
    mockValidate.mockReturnValue(
      new ValidationResult([
        { messageKey: 'report.startdate.required.error', message: 'Start date is required.', type: 'ERROR' },
      ]),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(await screen.findByText('Start date is required.')).toBeInTheDocument();
  });

  it('shows inline error when end date or time frame is required', async () => {
    mockValidate.mockReturnValue(
      new ValidationResult([
        {
          messageKey: 'report.r10.enddate.or.timeframe.required.error',
          message: 'End date or time frame is required.',
          type: 'ERROR',
        },
      ]),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(await screen.findByText('End date or time frame is required.')).toBeInTheDocument();
  });

  it('calls addNotification with warning when no data is found', async () => {
    mockUseR10ReportMutation.mockReturnValue({
      mutate: vi.fn().mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
        opts.onError({ response: { status: 404 } });
      }),
      isPending: false,
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    await waitFor(() =>
      expect(mockAddNotification).toHaveBeenCalledWith({
        kind: 'warning',
        title: 'No data found. No records matched the selected criteria.',
      }),
    );
  });

  it('calls addNotification with error on general report generation failure', async () => {
    mockUseR10ReportMutation.mockReturnValue({
      mutate: vi.fn().mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
        opts.onError(new Error('Server error'));
      }),
      isPending: false,
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    await waitFor(() =>
      expect(mockAddNotification).toHaveBeenCalledWith({
        kind: 'error',
        title: 'Report generation failed.',
      }),
    );
  });
});
