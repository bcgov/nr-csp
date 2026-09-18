import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import * as r13Service from '@/services/r13.service';
import { parseReportValidationError } from '@/services/reportValidation';
import * as reportUtils from '@/utils/report';
import * as r13Validation from '@/validations/reports/r13';
import { ValidationResult, type ValidationMessage } from '@/validations/validationResult';

import { R13AdHocReportingPage } from './index';

// ── Module mocks ───────────────────────────────────────────────────────────────

// PageTitle uses useNavigate which requires a Router context.
// Stub it with a plain heading so page tests don't need MemoryRouter.
vi.mock('@/components/core/PageTitle', () => ({
  default: ({ title }: { title: string }) => <h1>{title}</h1>,
}));

const mockAddNotification = vi.fn();

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: mockAddNotification }),
}));

vi.mock('@/services/r13.service', () => ({
  useR13ReportMutation: vi.fn(),
}));

// Return empty arrays for all lookup queries so the page renders without network
vi.mock('@/services/lookup.service', () => ({
  useSubmissionStatusesQuery: () => ({ data: [], isLoading: false }),
  useInvoiceStatusesQuery: () => ({ data: [], isLoading: false }),
  useInvoiceTypesQuery: () => ({ data: [], isLoading: false }),
  useSpeciesLookupQuery: () => ({ data: [], isLoading: false }),
  useSortCodesLookupQuery: () => ({ data: [], isLoading: false }),
  useGradeLookupQuery: () => ({ data: [], isLoading: false }),
}));

// Keep real splitMessages so field-error mapping works correctly.
// Mock parseReportValidationError to return no server errors by default.
vi.mock('@/services/reportValidation', async (importActual) => {
  const actual = await importActual();
  return {
    ...(actual as object),
    parseReportValidationError: vi.fn().mockResolvedValue([]),
  };
});

// ── Helpers ────────────────────────────────────────────────────────────────────

/** Build a ValidationResult with the given ERROR messages. */
const makeErrors = (...entries: { key: string; message: string }[]): ValidationResult =>
  new ValidationResult(
    entries.map(({ key, message }): ValidationMessage => ({ messageKey: key, message, type: 'ERROR' })),
  );

/** A passing ValidationResult (no errors). */
const passResult = new ValidationResult([]);

const mockMutate = vi.fn();
const mockValidateR13 = vi.spyOn(r13Validation, 'validateR13');
const mockDownloadBlob = vi.spyOn(reportUtils, 'downloadBlob').mockImplementation(() => {});

const renderPage = () => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={qc}>
      <R13AdHocReportingPage />
    </QueryClientProvider>,
  );
};

beforeEach(() => {
  // Default: mutation idle, validation passes
  (r13Service.useR13ReportMutation as ReturnType<typeof vi.fn>).mockReturnValue({
    mutate: mockMutate,
    isPending: false,
  });
  mockValidateR13.mockReturnValue(passResult);
});

afterEach(() => {
  vi.clearAllMocks();
});

// ── Rendering ──────────────────────────────────────────────────────────────────

describe('R13AdHocReportingPage — rendering', () => {
  it('renders the page heading', () => {
    renderPage();
    expect(screen.getByRole('heading', { level: 1, name: /r13/i })).toBeInTheDocument();
  });

  it('renders all filter section headings', () => {
    renderPage();
    expect(screen.getByRole('heading', { level: 2, name: /report information/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /submission.*approval/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /invoice information/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /client information/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /invoice detail/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /additional columns/i })).toBeInTheDocument();
  });

  it('renders the report name text input', () => {
    renderPage();
    expect(screen.getByLabelText(/report name/i)).toBeInTheDocument();
  });

  it('renders start date and end date inputs', () => {
    renderPage();
    expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
  });

  it('renders the time frame dropdown', () => {
    renderPage();
    expect(screen.getByRole('combobox', { name: /time frame/i })).toBeInTheDocument();
  });

  it('renders Generate PDF, Export CSV, and Clear all buttons', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /generate pdf/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /export csv/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /clear all/i })).toBeInTheDocument();
  });

  it('renders invoice submitter text input', () => {
    renderPage();
    // FilterRow inputs have labelText="" (label is in the table cell, not the input);
    // match by the unique placeholder text instead.
    expect(screen.getByPlaceholderText(/name or id/i)).toBeInTheDocument();
  });

  it('renders seller and buyer filter rows', () => {
    renderPage();
    // titleText="" on ClientAutocomplete (label lives in the table cell), so check
    // the table-cell labels rather than the combobox accessible name.
    expect(screen.getByText('Seller name')).toBeInTheDocument();
    expect(screen.getByText('Seller number')).toBeInTheDocument();
    expect(screen.getByText('Buyer name')).toBeInTheDocument();
    expect(screen.getByText('Buyer number')).toBeInTheDocument();
  });
});

// ── Loading state ──────────────────────────────────────────────────────────────

describe('R13AdHocReportingPage — loading state', () => {
  it('shows a loading indicator and hides the PDF button while pending', () => {
    (r13Service.useR13ReportMutation as ReturnType<typeof vi.fn>).mockReturnValue({
      mutate: mockMutate,
      isPending: true,
    });
    renderPage();
    expect(screen.getByText(/generating/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /generate pdf/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /export csv/i })).not.toBeInTheDocument();
  });
});

// ── Validation ─────────────────────────────────────────────────────────────────

describe('R13AdHocReportingPage — validation', () => {
  it('shows report name error when the field is empty', () => {
    mockValidateR13.mockReturnValue(
      makeErrors({ key: 'report.r13.reportname.required.error', message: 'Report name is required.' }),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(screen.getByText('Report name is required.')).toBeInTheDocument();
  });

  it('shows start date error when start date is missing', () => {
    mockValidateR13.mockReturnValue(
      makeErrors({ key: 'report.startdate.required.error', message: 'Start date is required.' }),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(screen.getByText('Start date is required.')).toBeInTheDocument();
  });

  it('shows end date error when neither endDate nor timeFrame is provided', () => {
    mockValidateR13.mockReturnValue(
      makeErrors({
        key: 'report.r13.enddate.or.timeframe.required.error',
        message: 'End date or time frame is required.',
      }),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(screen.getByText('End date or time frame is required.')).toBeInTheDocument();
  });

  it('shows the show-options error as a notification banner when fewer than 2 columns are selected', () => {
    mockValidateR13.mockReturnValue(
      makeErrors({
        key: 'report.r13.showcolumns.minimum.error',
        message: 'At least 2 columns must be selected to show on report.',
      }),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    // The checkboxes it refers to are spread across the page, so this one belongs
    // in the banner at the top rather than inline beside any single field.
    expect(screen.getByText(/at least 2 columns/i).closest('.cds--inline-notification')).toBeInTheDocument();
  });

  it('pins a server-reported submission number error to the approval ID field', async () => {
    // The field only accepts digits, so this can only reach the page from an
    // API-side rejection — it still belongs beside the input, not in the banner.
    (parseReportValidationError as ReturnType<typeof vi.fn>).mockResolvedValueOnce([
      {
        messageKey: 'report.submissionnumber.numeric.error',
        message: 'Submission number must be numeric.',
        type: 'ERROR',
      },
    ]);
    mockMutate.mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
      opts.onError({ response: { status: 400 } });
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));

    const message = await screen.findByText('Submission number must be numeric.');
    expect(message.closest('.cds--inline-notification')).not.toBeInTheDocument();
  });

  // ── Approval ID number input restriction ─────────────────────────────────

  it.each([
    ['TEST', ''],
    ['%', ''],
    ['12A34', '1234'],
    ['1 2-3', '123'],
  ])('strips non-digits typed into the approval ID number field (%s)', (typed, expected) => {
    renderPage();
    const input = document.querySelector('#approval-id-number') as HTMLInputElement;
    fireEvent.change(input, { target: { value: typed } });
    expect(input).toHaveValue(expected);
  });

  it('caps the approval ID number at 10 digits', () => {
    renderPage();
    const input = document.querySelector('#approval-id-number') as HTMLInputElement;
    fireEvent.change(input, { target: { value: '123456789012345' } });
    expect(input).toHaveValue('1234567890');
  });

  it('does not call the mutation when validation fails', () => {
    mockValidateR13.mockReturnValue(
      makeErrors({ key: 'report.r13.reportname.required.error', message: 'Report name is required.' }),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mockMutate).not.toHaveBeenCalled();
  });

  it('clears the report name error when the user types in the field', () => {
    mockValidateR13.mockReturnValueOnce(
      makeErrors({ key: 'report.r13.reportname.required.error', message: 'Report name is required.' }),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(screen.getByText('Report name is required.')).toBeInTheDocument();

    mockValidateR13.mockReturnValue(passResult);
    fireEvent.change(screen.getByLabelText(/report name/i), { target: { value: 'My Report' } });
    expect(screen.queryByText('Report name is required.')).not.toBeInTheDocument();
  });
});

// ── Export ─────────────────────────────────────────────────────────────────────

describe('R13AdHocReportingPage — export', () => {
  it('calls the mutation with reportFormat: PDF when Generate PDF is clicked', () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mockMutate).toHaveBeenCalledWith(expect.objectContaining({ reportFormat: 'PDF' }), expect.any(Object));
  });

  it('calls the mutation with reportFormat: CSV when Export CSV is clicked', () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /export csv/i }));
    expect(mockMutate).toHaveBeenCalledWith(expect.objectContaining({ reportFormat: 'CSV' }), expect.any(Object));
  });

  it('includes the typed report name in the mutation payload', () => {
    renderPage();
    fireEvent.change(screen.getByLabelText(/report name/i), { target: { value: 'Q1 2026 Report' } });
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mockMutate).toHaveBeenCalledWith(
      expect.objectContaining({ reportName: 'Q1 2026 Report' }),
      expect.any(Object),
    );
  });

  it('calls downloadBlob with the returned blob and filename on success', () => {
    const testBlob = new Blob(['pdf content'], { type: 'application/pdf' });
    mockMutate.mockImplementation((_req: unknown, opts: { onSuccess: (r: unknown) => void }) => {
      opts.onSuccess({ blob: testBlob, filename: 'r13-report.pdf' });
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mockDownloadBlob).toHaveBeenCalledWith(testBlob, 'r13-report.pdf');
  });

  it('calls addNotification with kind:warning when the API returns 404 (no data found)', async () => {
    mockMutate.mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
      opts.onError({ response: { status: 404 } });
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

  it('calls addNotification with kind:error on a general API failure', async () => {
    mockMutate.mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
      opts.onError(new Error('Internal Server Error'));
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

  it('surfaces the API error message when the failure carries no structured validation errors', async () => {
    mockMutate.mockImplementation((_req: unknown, opts: { onError: (e: unknown) => void }) => {
      opts.onError({
        response: { status: 400, data: { code: 'VALIDATION_ERROR', message: 'submissionNumber must be numeric' } },
      });
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    await waitFor(() =>
      expect(mockAddNotification).toHaveBeenCalledWith({
        kind: 'error',
        title: 'Report generation failed.',
        subtitle: 'submissionNumber must be numeric',
      }),
    );
  });

  // ── Range filters ────────────────────────────────────────────────────────
  // The value box of the four aggregated-column filters maps to the "values"
  // request parameter, which the backend matches with a contains — that is what
  // makes % behave as a wildcard. From / To keep their own range parameters.

  it('sends the value box of a range row as its values parameter, wildcard intact', () => {
    renderPage();
    fireEvent.change(document.querySelector('#boom-number-value')!, { target: { value: 'BM%' } });
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mockMutate).toHaveBeenCalledWith(
      expect.objectContaining({ invoiceBoomNumbers: ['BM%'] }),
      expect.any(Object),
    );
    const [request] = mockMutate.mock.calls[0] as [Record<string, unknown>];
    expect(request).not.toHaveProperty('invoiceBoomNumberFrom');
    expect(request).not.toHaveProperty('invoiceBoomNumberTo');
  });

  it('sends the from/to boxes of a range row as range parameters', () => {
    renderPage();
    fireEvent.change(document.querySelector('#timber-mark-from')!, { target: { value: 'T1' } });
    fireEvent.change(document.querySelector('#timber-mark-to')!, { target: { value: 'T9' } });
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mockMutate).toHaveBeenCalledWith(
      expect.objectContaining({ invoiceTimberMarkFrom: 'T1', invoiceTimberMarkTo: 'T9' }),
      expect.any(Object),
    );
    const [request] = mockMutate.mock.calls[0] as [Record<string, unknown>];
    expect(request).not.toHaveProperty('invoiceTimberMarks');
  });

  it('still sends the invoice number value as a closed from/to range', () => {
    renderPage();
    fireEvent.change(document.querySelector('#invoice-number-value')!, { target: { value: 'INV-1' } });
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(mockMutate).toHaveBeenCalledWith(
      expect.objectContaining({ invoiceNumberFrom: 'INV-1', invoiceNumberTo: 'INV-1' }),
      expect.any(Object),
    );
  });
});

// ── Clear all ──────────────────────────────────────────────────────────────────

describe('R13AdHocReportingPage — Clear all', () => {
  it('clears the report name field', () => {
    renderPage();
    const input = screen.getByLabelText(/report name/i);
    fireEvent.change(input, { target: { value: 'My Custom Report' } });
    expect(input).toHaveValue('My Custom Report');

    fireEvent.click(screen.getByRole('button', { name: /clear all/i }));
    expect(input).toHaveValue('');
  });

  it('clears other text filter fields', () => {
    renderPage();
    // Use placeholder since FilterRow inputs have labelText="" (the label lives in the table cell)
    const submitterInput = screen.getByPlaceholderText(/name or id/i);
    fireEvent.change(submitterInput, { target: { value: 'jdoe' } });
    expect(submitterInput).toHaveValue('jdoe');

    fireEvent.click(screen.getByRole('button', { name: /clear all/i }));
    expect(submitterInput).toHaveValue('');
  });

  it('clears all validation errors', () => {
    mockValidateR13.mockReturnValueOnce(
      makeErrors({ key: 'report.r13.reportname.required.error', message: 'Report name is required.' }),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(screen.getByText('Report name is required.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /clear all/i }));
    expect(screen.queryByText('Report name is required.')).not.toBeInTheDocument();
  });
});

// ── Time frame / end date auto-fill ─────────────────────────────────────────────

describe('R13AdHocReportingPage — time frame', () => {
  it('shows a single editable End date input regardless of time frame selection', () => {
    renderPage();
    expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/end date.*time frame/i)).not.toBeInTheDocument();
  });

  it('auto-fills end date once start date and time frame are both set', () => {
    renderPage();
    fireEvent.input(screen.getByLabelText(/start date/i), { target: { value: '2026-03-15' } });
    fireEvent.click(screen.getByRole('combobox', { name: /time frame/i }));
    fireEvent.click(screen.getByText('01'));
    expect(screen.getByLabelText(/end date/i)).toHaveValue('2026-03-31');
  });

  it('keeps a manually-entered end date until start date or time frame change again, and resets time frame to Select...', () => {
    renderPage();
    fireEvent.input(screen.getByLabelText(/start date/i), { target: { value: '2026-03-15' } });
    fireEvent.click(screen.getByRole('combobox', { name: /time frame/i }));
    fireEvent.click(screen.getByText('01'));
    expect(screen.getByLabelText(/end date/i)).toHaveValue('2026-03-31');

    fireEvent.input(screen.getByLabelText(/end date/i), { target: { value: '2026-03-20' } });
    expect(screen.getByLabelText(/end date/i)).toHaveValue('2026-03-20');
    expect(screen.getByRole('combobox', { name: /time frame/i })).toHaveTextContent('Select...');
  });
});

// ── Scroll on validation failure ─────────────────────────────────────────────────

describe('R13AdHocReportingPage — scroll on validation failure', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('scrolls to the top when client-side validation fails', () => {
    const scrollToSpy = vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined);
    mockValidateR13.mockReturnValue(
      makeErrors({ key: 'report.r13.reportname.required.error', message: 'Report name is required.' }),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /generate pdf/i }));
    expect(scrollToSpy).toHaveBeenCalledWith({ top: 0, behavior: 'smooth' });
  });
});
