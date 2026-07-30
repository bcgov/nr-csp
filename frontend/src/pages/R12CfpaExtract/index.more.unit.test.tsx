import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import * as r12Service from '@/services/r12.service';
import * as r12Validation from '@/validations/reports/r12';
import { ValidationResult } from '@/validations/validationResult';
import { downloadBlob } from '@/utils/report';

import { R12CfpaExtractPage } from './index';

const mockAddNotification = vi.fn();

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: mockAddNotification }),
}));

vi.mock('@/services/r12.service', () => ({
  useR12ReportMutation: vi.fn(),
}));

vi.mock('@/services/lookup.service', () => ({
  useMaturityCodesWithCantsQuery: () => ({
    data: [{ code: 'C', description: 'Cants' }],
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

const mockUseR12ReportMutation = r12Service.useR12ReportMutation as ReturnType<typeof vi.fn>;
const mockValidate = vi.spyOn(r12Validation, 'validateR12');
const mockDownloadBlob = vi.mocked(downloadBlob);

const setDate = (label: RegExp, value: string) => {
  const input = screen.getByLabelText(label);
  fireEvent.input(input, { target: { value } });
  fireEvent.blur(input);
};

describe('R12CfpaExtractPage interactions', () => {
  let mutate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mutate = vi.fn();
    mockUseR12ReportMutation.mockReturnValue({ mutate, isPending: false });
    mockValidate.mockReturnValue(new ValidationResult([]));
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('sends the selected year, month, dates, time frame and maturities in the request', () => {
    render(<R12CfpaExtractPage />);
    fireEvent.click(screen.getByRole('combobox', { name: /report year/i }));
    fireEvent.click(screen.getByText('2024'));
    fireEvent.click(screen.getByRole('combobox', { name: /report month/i }));
    fireEvent.click(screen.getByText('02 - February'));
    setDate(/start date/i, '2024-01-10');
    setDate(/end date/i, '2024-02-20');
    fireEvent.click(screen.getByRole('combobox', { name: /time frame/i }));
    fireEvent.click(screen.getByText('03'));
    fireEvent.click(screen.getByText('select-all-maturity'));

    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(mockValidate).toHaveBeenCalledWith('2024', expect.any(Date), expect.any(Date), '03');
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        reportFormat: 'CSV',
        year: 2024,
        month: 2,
        dateFrom: '20240110',
        dateTo: '20240220',
        timeFrame: '03',
        logSaleTypeCode: 'C',
      }),
      expect.anything(),
    );
  });

  it('omits empty criteria from the request', () => {
    render(<R12CfpaExtractPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));

    const request = mutate.mock.calls[0][0];
    expect(request).toMatchObject({ reportFormat: 'PDF' });
    expect(request.year).toBeUndefined();
    expect(request.month).toBeUndefined();
    expect(request.dateFrom).toBeUndefined();
    expect(request.dateTo).toBeUndefined();
    expect(request.timeFrame).toBeUndefined();
    expect(request.logSaleTypeCode).toBeUndefined();
  });

  it('downloads the blob when report generation succeeds', async () => {
    const blob = new Blob(['csv-data']);
    mutate.mockImplementation((_req: unknown, opts: { onSuccess: (r: { blob: Blob; filename: string }) => void }) => {
      opts.onSuccess({ blob, filename: 'r12-extract.csv' });
    });
    render(<R12CfpaExtractPage />);
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));
    await waitFor(() => expect(mockDownloadBlob).toHaveBeenCalledWith(blob, 'r12-extract.csv'));
    expect(mockAddNotification).not.toHaveBeenCalled();
  });

  it('maps server validation errors to field, form and warning displays', async () => {
    mutate.mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
      opts.onError({
        response: {
          data: {
            message: 'Validation failed',
            errors: [
              {
                messageKey: 'report.r12.startdate.required.error',
                message: 'Server: start date missing.',
                type: 'ERROR',
              },
              { messageKey: 'some.unmapped.key', message: 'Server: form level problem.', type: 'ERROR' },
              { messageKey: 'some.warning.key', message: 'Server: heads up.', type: 'WARNING' },
            ],
          },
        },
      });
    });
    render(<R12CfpaExtractPage />);
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
    render(<R12CfpaExtractPage />);
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(screen.getByText('Client: form level error.')).toBeInTheDocument();
    expect(screen.getByText('Client: warning only.')).toBeInTheDocument();
    expect(mutate).not.toHaveBeenCalled();
  });

  it('still generates the report when validation returns only warnings', () => {
    mockValidate.mockReturnValue(
      new ValidationResult([{ messageKey: 'client.warning.key', message: 'Client: proceed anyway.', type: 'WARNING' }]),
    );
    render(<R12CfpaExtractPage />);
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(screen.getByText('Client: proceed anyway.')).toBeInTheDocument();
    expect(mutate).toHaveBeenCalled();
  });

  it('clears field errors when the offending inputs are edited', async () => {
    mockValidate.mockReturnValueOnce(
      new ValidationResult([
        { messageKey: 'report.r12.startdate.required.error', message: 'Start date is required.', type: 'ERROR' },
        { messageKey: 'report.r12.enddate.or.timeframe.required.error', message: 'End date needed.', type: 'ERROR' },
        { messageKey: 'report.timeframe.numeric.error', message: 'Time frame must be numeric.', type: 'ERROR' },
      ]),
    );
    render(<R12CfpaExtractPage />);
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(await screen.findByText('Start date is required.')).toBeInTheDocument();

    setDate(/start date/i, '2024-01-10');
    expect(screen.queryByText('Start date is required.')).not.toBeInTheDocument();
    setDate(/end date/i, '2024-02-20');
    expect(screen.queryByText('End date needed.')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('combobox', { name: /time frame/i }));
    fireEvent.click(screen.getByText('05'));
    expect(screen.queryByText('Time frame must be numeric.')).not.toBeInTheDocument();
  });
});
