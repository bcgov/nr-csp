import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import * as r12Service from '@/services/r12.service';
import * as r12Validation from '@/validations/reports/r12';
import { ValidationResult } from '@/validations/validationResult';

import { R12CfpaExtractPage } from './index';

const mockAddNotification = vi.fn();

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: mockAddNotification }),
}));

vi.mock('@/services/r12.service', () => ({
  useR12ReportMutation: vi.fn(),
}));

vi.mock('@/services/lookup.service', () => ({
  useMaturityCodesWithCantsQuery: () => ({ data: [], isLoading: false }),
}));

const mockUseR12ReportMutation = r12Service.useR12ReportMutation as ReturnType<typeof vi.fn>;
const mockValidate = vi.spyOn(r12Validation, 'validateR12');

describe('R12CfpaExtractPage', () => {
  beforeEach(() => {
    mockUseR12ReportMutation.mockReturnValue({ mutate: vi.fn(), isPending: false });
    mockValidate.mockReturnValue(new ValidationResult([]));
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title', () => {
    render(<R12CfpaExtractPage />);
    expect(screen.getByRole('heading', { level: 1, name: /r12 cfpa detailed data extract/i })).toBeInTheDocument();
  });

  it('renders Report year and Report month selects', () => {
    render(<R12CfpaExtractPage />);
    expect(screen.getByRole('combobox', { name: /report year/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /report month/i })).toBeInTheDocument();
  });

  it('renders date inputs', () => {
    render(<R12CfpaExtractPage />);
    expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
  });

  it('renders Time frame and Maturity selects', () => {
    render(<R12CfpaExtractPage />);
    expect(screen.getByRole('combobox', { name: /time frame/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /maturity/i })).toBeInTheDocument();
  });

  it('renders Generate PDF and Export CSV buttons', () => {
    render(<R12CfpaExtractPage />);
    expect(screen.getByRole('button', { name: /generate pdf/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /export csv/i })).toBeInTheDocument();
  });

  it('shows loading indicator and hides Export CSV when isPending', () => {
    mockUseR12ReportMutation.mockReturnValue({ mutate: vi.fn(), isPending: true });
    render(<R12CfpaExtractPage />);
    expect(screen.getByText(/generating/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /export csv/i })).not.toBeInTheDocument();
  });

  it('shows inline error when start date is required', async () => {
    mockValidate.mockReturnValue(
      new ValidationResult([
        {
          messageKey: 'report.r12.startdate.required.error',
          message: 'Start date is required when no report year is provided.',
          type: 'ERROR',
        },
      ]),
    );
    render(<R12CfpaExtractPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(await screen.findByText('Start date is required when no report year is provided.')).toBeInTheDocument();
  });

  it('shows inline error when end date or time frame is required', async () => {
    mockValidate.mockReturnValue(
      new ValidationResult([
        {
          messageKey: 'report.r12.enddate.or.timeframe.required.error',
          message: 'End date or time frame is required when no report year is provided.',
          type: 'ERROR',
        },
      ]),
    );
    render(<R12CfpaExtractPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(
      await screen.findByText('End date or time frame is required when no report year is provided.'),
    ).toBeInTheDocument();
  });

  it('calls addNotification with warning when no data is found', async () => {
    mockUseR12ReportMutation.mockReturnValue({
      mutate: vi.fn().mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
        opts.onError({ response: { status: 404 } });
      }),
      isPending: false,
    });
    render(<R12CfpaExtractPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    await waitFor(() =>
      expect(mockAddNotification).toHaveBeenCalledWith({
        kind: 'warning',
        title: 'No data found. No records matched the selected criteria.',
      }),
    );
  });

  it('calls addNotification with error on general report generation failure', async () => {
    mockUseR12ReportMutation.mockReturnValue({
      mutate: vi.fn().mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
        opts.onError(new Error('Server error'));
      }),
      isPending: false,
    });
    render(<R12CfpaExtractPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    await waitFor(() =>
      expect(mockAddNotification).toHaveBeenCalledWith({
        kind: 'error',
        title: 'Report generation failed.',
      }),
    );
  });
});
