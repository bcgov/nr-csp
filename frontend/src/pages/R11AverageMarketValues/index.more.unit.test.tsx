import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import * as r11Service from '@/services/r11.service';
import * as r11Validation from '@/validations/reports/r11';
import { ValidationResult } from '@/validations/validationResult';
import { downloadBlob } from '@/utils/report';

import { R11AverageMarketValuesPage } from './index';

const mockAddNotification = vi.fn();

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: mockAddNotification }),
}));

vi.mock('@/services/r11.service', () => ({
  useR11ReportMutation: vi.fn(),
}));

vi.mock('@/services/lookup.service', () => ({
  useMaturityCodesNoCantsQuery: () => ({
    data: [{ code: 'S', description: 'Second Growth' }],
    isLoading: false,
  }),
}));

// Keep every real report util except downloadBlob so the success path is assertable.
vi.mock('@/utils/report', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/utils/report')>();
  return { ...actual, downloadBlob: vi.fn() };
});

// Carbon's FilterableMultiSelect does not respond to synthetic clicks in
// happy-dom, so drive the page's maturity onChange handler through a stub.
vi.mock('@/components/Form/TaggedMultiSelect', () => ({
  default: (props: {
    id: string;
    items: { code: string; description: string }[];
    onChange?: (data: { selectedItems: { code: string; description: string }[] }) => void;
  }) => (
    <button type="button" onClick={() => props.onChange?.({ selectedItems: props.items })}>
      {`select-all-${props.id}`}
    </button>
  ),
}));

const mockUseR11ReportMutation = r11Service.useR11ReportMutation as ReturnType<typeof vi.fn>;
const mockValidate = vi.spyOn(r11Validation, 'validateR11');
const mockDownloadBlob = vi.mocked(downloadBlob);

const setDate = (label: RegExp, value: string) => {
  const input = screen.getByLabelText(label);
  fireEvent.input(input, { target: { value } });
  fireEvent.blur(input);
};

describe('R11AverageMarketValuesPage interactions', () => {
  let mutate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mutate = vi.fn();
    mockUseR11ReportMutation.mockReturnValue({ mutate, isPending: false });
    mockValidate.mockReturnValue(new ValidationResult([]));
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('sends the filled report criteria in the request', () => {
    render(<R11AverageMarketValuesPage />);
    setDate(/start date/i, '2024-01-10');
    setDate(/end date/i, '2024-02-20');
    fireEvent.click(screen.getByRole('combobox', { name: /time frame/i }));
    fireEvent.click(screen.getByText('03'));
    fireEvent.click(screen.getByRole('combobox', { name: /report type/i }));
    fireEvent.click(screen.getByText('Production'));
    fireEvent.click(screen.getByText('select-all-maturity'));
    fireEvent.click(screen.getByRole('checkbox', { name: /blended/i }));

    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(mockValidate).toHaveBeenCalledWith(expect.any(Date), expect.any(Date), 'P', '03');
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        reportFormat: 'CSV',
        dateFrom: '20240110',
        dateTo: '20240220',
        timeFrame: '03',
        modelingCode: 'P',
        maturityCodes: 'S',
        blended: true,
      }),
      expect.anything(),
    );
  });

  it('downloads the blob when report generation succeeds', async () => {
    const blob = new Blob(['csv-data']);
    mutate.mockImplementation((_req: unknown, opts: { onSuccess: (r: { blob: Blob; filename: string }) => void }) => {
      opts.onSuccess({ blob, filename: 'r11-report.csv' });
    });
    render(<R11AverageMarketValuesPage />);
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));
    await waitFor(() => expect(mockDownloadBlob).toHaveBeenCalledWith(blob, 'r11-report.csv'));
    expect(mockAddNotification).not.toHaveBeenCalled();
  });

  it('maps server validation errors to field, form and warning displays', async () => {
    mutate.mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
      opts.onError({
        response: {
          data: {
            message: 'Validation failed',
            errors: [
              { messageKey: 'report.startdate.required.error', message: 'Server: start date missing.', type: 'ERROR' },
              { messageKey: 'some.unmapped.key', message: 'Server: form level problem.', type: 'ERROR' },
              { messageKey: 'some.warning.key', message: 'Server: heads up.', type: 'WARNING' },
            ],
          },
        },
      });
    });
    render(<R11AverageMarketValuesPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));

    expect(await screen.findByText('Server: start date missing.')).toBeInTheDocument();
    expect(screen.getByText('Server: form level problem.')).toBeInTheDocument();
    expect(screen.getByText('Server: heads up.')).toBeInTheDocument();
    expect(mockAddNotification).not.toHaveBeenCalled();
  });

  it('shows client-side form errors and warnings without generating the report', () => {
    mockValidate.mockReturnValue(
      new ValidationResult([
        { messageKey: 'unmapped.form.key', message: 'Client: form level error.', type: 'ERROR' },
        { messageKey: 'client.warning.key', message: 'Client: warning only.', type: 'WARNING' },
      ]),
    );
    render(<R11AverageMarketValuesPage />);
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(screen.getByText('Client: form level error.')).toBeInTheDocument();
    expect(screen.getByText('Client: warning only.')).toBeInTheDocument();
    expect(mutate).not.toHaveBeenCalled();
  });

  it('still generates the report when validation returns only warnings', () => {
    mockValidate.mockReturnValue(
      new ValidationResult([{ messageKey: 'client.warning.key', message: 'Client: proceed anyway.', type: 'WARNING' }]),
    );
    render(<R11AverageMarketValuesPage />);
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(screen.getByText('Client: proceed anyway.')).toBeInTheDocument();
    expect(mutate).toHaveBeenCalled();
  });

  it('clears field errors when the offending inputs are edited', async () => {
    mockValidate.mockReturnValueOnce(
      new ValidationResult([
        { messageKey: 'report.startdate.required.error', message: 'Start date is required.', type: 'ERROR' },
        { messageKey: 'report.r11.reporttype.required.error', message: 'Report type is required.', type: 'ERROR' },
      ]),
    );
    render(<R11AverageMarketValuesPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(await screen.findByText('Start date is required.')).toBeInTheDocument();
    expect(screen.getByText('Report type is required.')).toBeInTheDocument();

    setDate(/start date/i, '2024-01-10');
    expect(screen.queryByText('Start date is required.')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('combobox', { name: /report type/i }));
    fireEvent.click(screen.getByText('Scenario 1'));
    expect(screen.queryByText('Report type is required.')).not.toBeInTheDocument();
  });

  it('omits empty criteria and sends blended=false by default', () => {
    render(<R11AverageMarketValuesPage />);
    setDate(/end date/i, '2024-02-20');
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));

    const request = mutate.mock.calls[0][0];
    expect(request).toMatchObject({ reportFormat: 'PDF', dateTo: '20240220', blended: false });
    expect(request.dateFrom).toBeUndefined();
    expect(request.timeFrame).toBeUndefined();
    expect(request.modelingCode).toBeUndefined();
    expect(request.maturityCodes).toBeUndefined();
  });
});
