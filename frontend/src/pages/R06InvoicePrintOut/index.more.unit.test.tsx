import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import * as r06Service from '@/services/r06.service';
import * as r06Validation from '@/validations/reports/r06';
import { ValidationResult } from '@/validations/validationResult';
import { downloadBlob } from '@/utils/report';

import { R06InvoicePrintOutPage } from './index';

const mockAddNotification = vi.fn();

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: mockAddNotification }),
}));

vi.mock('@/services/r06.service', () => ({
  useR06ReportMutation: vi.fn(),
}));

vi.mock('@/services/lookup.service', () => ({
  useMaturityCodesWithCantsQuery: () => ({
    data: [{ code: 'O', description: 'Old Growth' }],
    isLoading: false,
  }),
  useInvoiceStatusesQuery: () => ({
    data: [{ code: 'APP', description: 'Approved' }],
    isLoading: false,
  }),
  useInvoiceTypesQuery: () => ({
    data: [{ code: 'SAL', description: 'Sales' }],
    isLoading: false,
  }),
}));

// Keep every real report util except downloadBlob so the success path is assertable.
vi.mock('@/utils/report', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/utils/report')>();
  return { ...actual, downloadBlob: vi.fn() };
});

type StubClient = { clientNumber: string; clientName: string; clientLocnCode: string };
const stubClient: StubClient = { clientNumber: '00001234', clientName: 'ACME LOGGING', clientLocnCode: '02' };

// Lightweight stand-ins for the autocomplete fields so the page's
// onSelect/onTypedChange handlers can be driven directly.
vi.mock('@/components/Form/ClientAutocomplete', () => ({
  default: (props: {
    id: string;
    onSelect: (client: StubClient | null) => void;
    onTypedChange?: (value: string) => void;
  }) => (
    <div>
      <button
        type="button"
        onClick={() => props.onSelect({ clientNumber: '00001234', clientName: 'ACME LOGGING', clientLocnCode: '02' })}
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

vi.mock('@/components/Form/ClientNumberAutocomplete', () => ({
  default: (props: { id: string; onSelect: (client: StubClient | null) => void }) => (
    <button
      type="button"
      onClick={() => props.onSelect({ clientNumber: '00009876', clientName: 'BIRCH BUYERS', clientLocnCode: '05' })}
    >
      {`select-${props.id}`}
    </button>
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

const mockUseR06ReportMutation = r06Service.useR06ReportMutation as ReturnType<typeof vi.fn>;
const mockValidate = vi.spyOn(r06Validation, 'validateR06');
const mockDownloadBlob = vi.mocked(downloadBlob);

const renderPage = () => {
  const qc = new QueryClient();
  render(
    <QueryClientProvider client={qc}>
      <R06InvoicePrintOutPage />
    </QueryClientProvider>,
  );
};

const setDate = (label: RegExp, value: string) => {
  const input = screen.getByLabelText(label);
  fireEvent.input(input, { target: { value } });
  fireEvent.blur(input);
};

describe('R06InvoicePrintOutPage interactions', () => {
  let mutate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mutate = vi.fn();
    mockUseR06ReportMutation.mockReturnValue({ mutate, isPending: false });
    mockValidate.mockReturnValue(new ValidationResult([]));
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('sends dates, maturities, statuses and types in the request', () => {
    renderPage();
    setDate(/start date/i, '2024-01-10');
    setDate(/end date/i, '2024-02-20');
    fireEvent.click(screen.getByText('select-all-maturity'));
    fireEvent.click(screen.getByRole('combobox', { name: /invoice status/i }));
    fireEvent.click(screen.getByText('Approved'));
    fireEvent.click(screen.getByRole('combobox', { name: /invoice type/i }));
    fireEvent.click(screen.getByText('Sales'));

    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        reportFormat: 'CSV',
        dateFrom: '20240110',
        dateTo: '20240220',
        maturityCodes: 'O',
        logSaleEntryStatusCode: 'APP',
        cspInvoiceTypeCode: 'SAL',
      }),
      expect.anything(),
    );
  });

  it('applies the seller/buyer selections from the name and number autocompletes', () => {
    renderPage();
    fireEvent.click(screen.getByText('select-seller-client'));
    fireEvent.click(screen.getByText('select-buyer-number'));
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));

    expect(mockValidate).toHaveBeenCalledWith(
      expect.objectContaining({
        sellerName: stubClient.clientName,
        sellerNumber: stubClient.clientNumber,
        buyerName: 'BIRCH BUYERS',
        buyerNumber: '00009876',
      }),
    );
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        sellerClientNumber: '00001234',
        sellerLocCode: '02',
        buyerClientNumber: '00009876',
        buyerLocCode: '05',
      }),
      expect.anything(),
    );
  });

  it('keeps the typed name but drops the number when a selection is cleared', () => {
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

  it('uppercases invoice range values as they are typed', () => {
    renderPage();
    fireEvent.change(screen.getByLabelText('From'), { target: { value: 'inv100' } });
    fireEvent.change(screen.getByLabelText('To'), { target: { value: 'inv200' } });
    expect(screen.getByLabelText('From')).toHaveValue('INV100');
    expect(screen.getByLabelText('To')).toHaveValue('INV200');

    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ invoiceNumbers: 'INV100,INV200' }),
      expect.anything(),
    );
  });

  it('downloads the blob when report generation succeeds', async () => {
    const blob = new Blob(['csv-data']);
    mutate.mockImplementation((_req: unknown, opts: { onSuccess: (r: { blob: Blob; filename: string }) => void }) => {
      opts.onSuccess({ blob, filename: 'r06-invoices.csv' });
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));
    await waitFor(() => expect(mockDownloadBlob).toHaveBeenCalledWith(blob, 'r06-invoices.csv'));
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
                messageKey: 'report.r06.startdate.required.error',
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

  it('clears field errors when the offending inputs are edited', async () => {
    mockValidate.mockReturnValueOnce(
      new ValidationResult([
        { messageKey: 'report.r06.startdate.required.error', message: 'Start date is required.', type: 'ERROR' },
        { messageKey: 'report.r06.enddate.required.error', message: 'End date is required.', type: 'ERROR' },
        {
          messageKey: 'report.submissionnumber.numeric.error',
          message: 'Submission number must be numeric.',
          type: 'ERROR',
        },
      ]),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(await screen.findByText('Start date is required.')).toBeInTheDocument();

    setDate(/start date/i, '2024-01-10');
    expect(screen.queryByText('Start date is required.')).not.toBeInTheDocument();
    setDate(/end date/i, '2024-02-20');
    expect(screen.queryByText('End date is required.')).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/submission number/i), { target: { value: '789' } });
    expect(screen.queryByText('Submission number must be numeric.')).not.toBeInTheDocument();
  });
});
