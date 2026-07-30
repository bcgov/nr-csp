import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import * as r10Service from '@/services/r10.service';
import * as r10Validation from '@/validations/reports/r10';
import { ValidationResult } from '@/validations/validationResult';
import { downloadBlob } from '@/utils/report';

import { R10LogSalesSpeciesPage } from './index';

const mockAddNotification = vi.fn();

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: mockAddNotification }),
}));

vi.mock('@/services/r10.service', () => ({
  useR10ReportMutation: vi.fn(),
}));

vi.mock('@/services/lookup.service', () => ({
  useMaturityCodesWithCantsQuery: () => ({
    data: [{ code: 'O', description: 'Old Growth' }],
    isLoading: false,
  }),
  useInvoiceTypesQuery: () => ({
    data: [{ code: 'LOG', description: 'Logging' }],
    isLoading: false,
  }),
}));

// Keep every real report util except downloadBlob, which touches DOM anchor
// click/object URLs — mock it so the CSV/PDF success path can be asserted.
vi.mock('@/utils/report', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/utils/report')>();
  return { ...actual, downloadBlob: vi.fn() };
});

// Lightweight stand-in for the client autocomplete so the page's
// onSelect/onTypedChange handlers can be driven directly.
vi.mock('@/components/Form/ClientAutocomplete', () => ({
  default: (props: {
    id: string;
    onSelect: (client: { clientNumber: string; clientName: string; clientLocnCode: string } | null) => void;
    onTypedChange?: (value: string) => void;
  }) => (
    <div>
      <button
        type="button"
        onClick={() => props.onSelect({ clientNumber: '00001234', clientName: 'ACME LOGGING', clientLocnCode: '00' })}
      >
        {`select-${props.id}`}
      </button>
      <button type="button" onClick={() => props.onSelect(null)}>
        {`clear-${props.id}`}
      </button>
      <input aria-label={`typed-${props.id}`} onChange={(e) => props.onTypedChange?.(e.target.value)} />
    </div>
  ),
}));

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

const mockUseR10ReportMutation = r10Service.useR10ReportMutation as ReturnType<typeof vi.fn>;
const mockValidate = vi.spyOn(r10Validation, 'validateR10');
const mockDownloadBlob = vi.mocked(downloadBlob);

const renderPage = () => {
  const qc = new QueryClient();
  render(
    <QueryClientProvider client={qc}>
      <R10LogSalesSpeciesPage />
    </QueryClientProvider>,
  );
};

const setDate = (label: RegExp, value: string) => {
  const input = screen.getByLabelText(label);
  fireEvent.input(input, { target: { value } });
  fireEvent.blur(input);
};

describe('R10LogSalesSpeciesPage interactions', () => {
  let mutate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mutate = vi.fn();
    mockUseR10ReportMutation.mockReturnValue({ mutate, isPending: false });
    mockValidate.mockReturnValue(new ValidationResult([]));
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('sends the filled report range and invoice criteria in the request', () => {
    renderPage();
    setDate(/start date/i, '2024-01-10');
    setDate(/end date/i, '2024-02-20');
    fireEvent.click(screen.getByRole('combobox', { name: /time frame/i }));
    fireEvent.click(screen.getByText('03'));
    fireEvent.click(screen.getByRole('combobox', { name: /invoice type/i }));
    fireEvent.click(screen.getByText('Logging'));

    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        reportFormat: 'CSV',
        dateFrom: '20240110',
        dateTo: '20240220',
        timeFrame: '03',
        invoiceTypeCode: 'LOG',
      }),
      expect.anything(),
    );
  });

  it('passes the selected seller and buyer to validation and the request', () => {
    renderPage();
    fireEvent.click(screen.getByText('select-seller-client'));
    fireEvent.click(screen.getByText('select-buyer-client'));
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));

    expect(mockValidate).toHaveBeenCalledWith(
      expect.objectContaining({
        sellerName: 'ACME LOGGING',
        sellerNumber: '00001234',
        buyerName: 'ACME LOGGING',
        buyerNumber: '00001234',
      }),
    );
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ sellerClientNumber: '00001234', buyerClientNumber: '00001234' }),
      expect.anything(),
    );
  });

  it('keeps the typed name but drops the number when the selection is cleared', () => {
    renderPage();
    fireEvent.change(screen.getByLabelText('typed-seller-client'), { target: { value: 'acme typed' } });
    fireEvent.click(screen.getByText('clear-seller-client'));
    fireEvent.click(screen.getByText('clear-buyer-client'));
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));

    expect(mockValidate).toHaveBeenCalledWith(
      expect.objectContaining({ sellerName: 'acme typed', sellerNumber: '', buyerNumber: '' }),
    );
    const request = mutate.mock.calls[0][0];
    expect(request.sellerClientNumber).toBeUndefined();
    expect(request.buyerClientNumber).toBeUndefined();
  });

  it('downloads the blob when report generation succeeds', async () => {
    const blob = new Blob(['csv-data']);
    mutate.mockImplementation((_req: unknown, opts: { onSuccess: (r: { blob: Blob; filename: string }) => void }) => {
      opts.onSuccess({ blob, filename: 'r10-report.csv' });
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));
    await waitFor(() => expect(mockDownloadBlob).toHaveBeenCalledWith(blob, 'r10-report.csv'));
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
    renderPage();
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
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(screen.getByText('Client: form level error.')).toBeInTheDocument();
    expect(screen.getByText('Client: warning only.')).toBeInTheDocument();
    expect(mutate).not.toHaveBeenCalled();
  });

  it('still generates the report when validation returns only warnings', () => {
    mockValidate.mockReturnValue(
      new ValidationResult([{ messageKey: 'client.warning.key', message: 'Client: proceed anyway.', type: 'WARNING' }]),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(screen.getByText('Client: proceed anyway.')).toBeInTheDocument();
    expect(mutate).toHaveBeenCalled();
  });

  it('clears a date field error when the date is edited', async () => {
    mockValidate.mockReturnValueOnce(
      new ValidationResult([
        { messageKey: 'report.startdate.required.error', message: 'Start date is required.', type: 'ERROR' },
        { messageKey: 'report.r10.enddate.or.timeframe.required.error', message: 'End date needed.', type: 'ERROR' },
      ]),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(await screen.findByText('Start date is required.')).toBeInTheDocument();
    expect(screen.getByText('End date needed.')).toBeInTheDocument();

    setDate(/start date/i, '2024-01-10');
    expect(screen.queryByText('Start date is required.')).not.toBeInTheDocument();
    setDate(/end date/i, '2024-02-20');
    expect(screen.queryByText('End date needed.')).not.toBeInTheDocument();
  });

  it('includes selected maturities in the request', () => {
    renderPage();
    fireEvent.click(screen.getByText('select-all-maturity'));
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(mutate).toHaveBeenCalledWith(expect.objectContaining({ maturityCodes: 'O' }), expect.anything());
  });
});
