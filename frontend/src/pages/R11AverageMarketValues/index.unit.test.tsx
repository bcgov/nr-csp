import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import * as r11Service from '@/services/r11.service';
import * as r11Validation from '@/validations/reports/r11';
import { ValidationResult } from '@/validations/validationResult';

import { R11AverageMarketValuesPage } from './index';

const mockAddNotification = vi.fn();

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: mockAddNotification }),
}));

vi.mock('@/services/r11.service', () => ({
  useR11ReportMutation: vi.fn(),
}));

vi.mock('@/services/lookup.service', () => ({
  useMaturityCodesNoCantsQuery: () => ({ data: [], isLoading: false }),
}));

const mockUseR11ReportMutation = r11Service.useR11ReportMutation as ReturnType<typeof vi.fn>;
const mockValidate = vi.spyOn(r11Validation, 'validateR11');

describe('R11AverageMarketValuesPage', () => {
  beforeEach(() => {
    mockUseR11ReportMutation.mockReturnValue({ mutate: vi.fn(), isPending: false });
    mockValidate.mockReturnValue(new ValidationResult([]));
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title', () => {
    render(<R11AverageMarketValuesPage />);
    expect(screen.getByRole('heading', { level: 1, name: /r11.*average market values/i })).toBeInTheDocument();
  });

  it('renders Report range section heading', () => {
    render(<R11AverageMarketValuesPage />);
    expect(screen.getByRole('heading', { level: 2, name: 'Report range' })).toBeInTheDocument();
  });

  it('renders date inputs', () => {
    render(<R11AverageMarketValuesPage />);
    expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
  });

  it('renders time frame, maturity and report type selects', () => {
    render(<R11AverageMarketValuesPage />);
    expect(screen.getByRole('combobox', { name: /time frame/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /maturity/i })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: /report type/i })).toBeInTheDocument();
  });

  it('renders Blended checkbox unchecked by default', () => {
    render(<R11AverageMarketValuesPage />);
    const checkbox = screen.getByRole('checkbox', { name: /blended/i });
    expect(checkbox).toBeInTheDocument();
    expect(checkbox).not.toBeChecked();
  });

  it('toggles Blended checkbox when clicked', () => {
    render(<R11AverageMarketValuesPage />);
    const checkbox = screen.getByRole('checkbox', { name: /blended/i });
    fireEvent.click(checkbox);
    expect(checkbox).toBeChecked();
  });

  it('renders Generate PDF and Export CSV buttons', () => {
    render(<R11AverageMarketValuesPage />);
    expect(screen.getByRole('button', { name: /generate pdf/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /export csv/i })).toBeInTheDocument();
  });

  it('shows loading indicator and hides Export CSV when isPending', () => {
    mockUseR11ReportMutation.mockReturnValue({ mutate: vi.fn(), isPending: true });
    render(<R11AverageMarketValuesPage />);
    expect(screen.getByText(/generating/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /export csv/i })).not.toBeInTheDocument();
  });

  it('shows inline error when start date is required', async () => {
    mockValidate.mockReturnValue(
      new ValidationResult([
        { messageKey: 'report.startdate.required.error', message: 'Start date is required.', type: 'ERROR' },
      ]),
    );
    render(<R11AverageMarketValuesPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(await screen.findByText('Start date is required.')).toBeInTheDocument();
  });

  it('shows inline error when report type is required', async () => {
    mockValidate.mockReturnValue(
      new ValidationResult([
        { messageKey: 'report.r11.reporttype.required.error', message: 'Report type is required.', type: 'ERROR' },
      ]),
    );
    render(<R11AverageMarketValuesPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(await screen.findByText('Report type is required.')).toBeInTheDocument();
  });

  it('calls addNotification with warning when no data is found', async () => {
    mockUseR11ReportMutation.mockReturnValue({
      mutate: vi.fn().mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
        opts.onError({ response: { status: 404 } });
      }),
      isPending: false,
    });
    render(<R11AverageMarketValuesPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    await waitFor(() =>
      expect(mockAddNotification).toHaveBeenCalledWith({
        kind: 'warning',
        title: 'No data found. No records matched the selected criteria.',
      }),
    );
  });

  it('calls addNotification with error on general report generation failure', async () => {
    mockUseR11ReportMutation.mockReturnValue({
      mutate: vi.fn().mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
        opts.onError(new Error('Server error'));
      }),
      isPending: false,
    });
    render(<R11AverageMarketValuesPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    await waitFor(() =>
      expect(mockAddNotification).toHaveBeenCalledWith({
        kind: 'error',
        title: 'Report generation failed.',
      }),
    );
  });
});
