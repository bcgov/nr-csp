import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';
import {
  parseSubmission,
  validateSubmissionBusiness,
  submitSubmission,
  type ParsedSubmission,
  type SubmissionParseResponse,
  type SubmissionValidationResponse,
  type SubmissionSubmitResponse,
} from '@/services/cspSubmission.service';
import { type ValidationMessageResponse } from '@/services/invoice.service';
import { downloadBlob } from '@/utils/report';

// ── Mocks ─────────────────────────────────────────────────────────────────────

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (orig) => ({
  ...(await orig<typeof import('react-router-dom')>()),
  useNavigate: () => mockNavigate,
}));

vi.mock('@/services/cspSubmission.service', async (orig) => ({
  ...(await orig<typeof import('@/services/cspSubmission.service')>()),
  parseSubmission: vi.fn(),
  validateSubmissionBusiness: vi.fn(),
  submitSubmission: vi.fn(),
}));

vi.mock('@/utils/report', async (orig) => ({
  ...(await orig<typeof import('@/utils/report')>()),
  downloadBlob: vi.fn(),
}));

import { UploadSubmissionPage } from './index';

const mockParse = vi.mocked(parseSubmission);
const mockValidate = vi.mocked(validateSubmissionBusiness);
const mockSubmit = vi.mocked(submitSubmission);
const mockDownload = vi.mocked(downloadBlob);

// ── Factories ───────────────────────────────────────────────────────────────

const msg = (messageKey: string, message: string, type: 'ERROR' | 'WARNING' = 'ERROR'): ValidationMessageResponse => ({
  messageKey,
  args: [],
  type,
  message,
});

const makeSubmission = (over: Partial<ParsedSubmission> = {}): ParsedSubmission => ({
  email: 'seller@example.com',
  telephone: '5551234567',
  monthComplete: 'Y',
  sellerSubmission: 'Y',
  submissionClientNumber: '12345678',
  submissionClientLocnCode: '01',
  invoices: [
    {
      index: 1,
      invoiceNumber: 'INV-001',
      invoiceDate: '2025-01-01',
      invoiceType: 'S',
      sellerClientNumber: '12345678',
      buyerClientNumber: '87654321',
      maturity: 'M',
      locationFOB: 'FOB',
      totalAmount: 1000,
      totalVolume: 12.345,
      totalPieces: 10,
      // Supplementary detail fields (some blank, to exercise the "(empty)" state).
      replacesInvoiceNumbers: 'INV-000',
      adjustsInvoiceNumbers: null,
      sellerClientLocnCode: '01',
      buyerClientLocnCode: '02',
      otherPartyName: 'Acme Logging',
      otherPartyCity: null,
      otherPartyProvState: null,
      primarySortCode: 'PSC',
      clientPrimarySortCode: null,
      boomNumbers: null,
      timberMarks: null,
      weighSlipNumbers: null,
      submitterNotes: 'Deliver by end of month',
    },
  ],
  lineItems: [
    {
      invoiceIndex: 1,
      lineIndex: 1,
      invoiceNumber: 'INV-001',
      species: 'FIR',
      grade: 'A',
      secondarySortCode: 'SS',
      clientSecondarySortCode: 'CSS',
      numberOfPieces: 5,
      volume: 6.789,
      price: 200,
    },
  ],
  ...over,
});

const makeParse = (over: Partial<SubmissionParseResponse> = {}): SubmissionParseResponse => ({
  valid: true,
  code: 'OK',
  message: 'parsed',
  errors: [],
  submission: makeSubmission(),
  ...over,
});

const makeValidation = (over: Partial<SubmissionValidationResponse> = {}): SubmissionValidationResponse => ({
  valid: true,
  code: 'ACCEPTED',
  message: 'valid',
  acceptedInvoices: ['INV-001'],
  rejectedInvoices: [],
  errors: [],
  ...over,
});

const makeSubmit = (over: Partial<SubmissionSubmitResponse> = {}): SubmissionSubmitResponse => ({
  valid: true,
  code: 'ACCEPTED',
  message: 'saved',
  submissionId: 42,
  acceptedInvoices: ['INV-001'],
  rejectedInvoices: [],
  errors: [],
  ...over,
});

// An axios-style thrown 422 with the envelope on response.data.
const envelopeError = (body: unknown) => ({ response: { data: body } });

// ── Helpers ───────────────────────────────────────────────────────────────────

function renderPage() {
  return render(
    <MemoryRouter>
      <PageTitleProvider>
        <UploadSubmissionPage />
      </PageTitleProvider>
    </MemoryRouter>,
  );
}

function selectFile(container: HTMLElement) {
  const input = container.querySelector('input[type="file"]') as HTMLInputElement;
  fireEvent.change(input, {
    target: { files: [new File(['<xml/>'], 'sub.xml', { type: 'text/xml' })] },
  });
}

/** Renders, uploads a file, and waits for the pipeline to settle (submission tables rendered). */
async function uploadAndSettle() {
  const utils = renderPage();
  selectFile(utils.container);
  await screen.findByRole('heading', { name: 'Submission Details' });
  return utils;
}

beforeEach(() => {
  vi.clearAllMocks();
});

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('UploadSubmissionPage', () => {
  it('renders idle info notification and upload card; close hides notification', () => {
    const { container } = renderPage();
    expect(screen.getByText('No XML file uploaded.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Upload XML File' })).toBeInTheDocument();

    const closeBtn = container.querySelector('.cds--inline-notification__close-button') as HTMLElement;
    expect(closeBtn).toBeTruthy();
    fireEvent.click(closeBtn);
    expect(screen.queryByText('No XML file uploaded.')).not.toBeInTheDocument();
  });

  it('parse success + business valid renders metadata, tables, and passed notification', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockResolvedValue(makeValidation());

    const { container } = await uploadAndSettle();

    expect(await screen.findByText('Validation passed.')).toBeInTheDocument();
    expect(screen.getByText('Submission is valid. 1 invoice(s) accepted.')).toBeInTheDocument();

    // Cards / headings.
    expect(screen.getByRole('heading', { name: 'Submission Metadata' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Submission Details' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Submitter Information' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Invoice Details' })).toBeInTheDocument();
    // Line items now live inside each expandable invoice row (no separate card).
    expect(screen.queryByRole('heading', { name: 'Invoice Line Items' })).not.toBeInTheDocument();

    // Combined count text (singular invoice + singular line item).
    expect(screen.getByText('1 invoice entry · 1 line item entry')).toBeInTheDocument();

    // Editable inputs seeded from the parsed submission.
    expect((screen.getByLabelText('Email Address') as HTMLInputElement).value).toBe('seller@example.com');
    expect((screen.getByLabelText('Submission Client Number') as HTMLInputElement).value).toBe('12345678');

    // A clean invoice shows the muted "No issues" badge.
    expect(screen.getByText('No issues')).toBeInTheDocument();

    // Expanded panel shows the ViewSubmission-style "Invoice details" card, with
    // populated fields and an italic "(empty)" placeholder for the blank ones.
    expect(screen.getByText('Invoice details for INV-001')).toBeInTheDocument();
    expect(screen.getByText('Acme Logging')).toBeInTheDocument();
    expect(screen.getByText('Deliver by end of month')).toBeInTheDocument();
    expect(screen.getAllByText('(empty)').length).toBeGreaterThan(0);

    // Table content rendered.
    expect(screen.getAllByText('INV-001').length).toBeGreaterThan(0);
    // Selected file name shown.
    expect(screen.getByText(/Selected file: sub\.xml/)).toBeInTheDocument();

    // Services called with the file.
    expect(mockParse).toHaveBeenCalledTimes(1);
    expect(mockValidate).toHaveBeenCalledTimes(1);
    expect(container.querySelector('.upload-submission-page')).toBeTruthy();
  });

  it('parse structural failure via thrown 422 shows issues card and downloads CSV', async () => {
    mockParse.mockRejectedValue(
      envelopeError({
        valid: false,
        errors: [msg('XSD', 'line 5, col 3: cvc-foo: bad', 'ERROR')],
      }),
    );

    const { container } = renderPage();
    selectFile(container);

    expect(await screen.findByText('1 issue found in submission')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Validation issues \(1\)/ })).toBeInTheDocument();

    const downloadBtn = screen.getByRole('button', { name: /Download issues/i });
    fireEvent.click(downloadBtn);
    expect(mockDownload).toHaveBeenCalledTimes(1);
    const [blobArg, nameArg] = mockDownload.mock.calls[0];
    expect(blobArg).toBeInstanceOf(Blob);
    expect(nameArg).toBe('validation-issues.csv');
    expect(mockValidate).not.toHaveBeenCalled();
  });

  it('parse resolves valid:false goes to structural-error (plural count)', async () => {
    mockParse.mockResolvedValue(
      makeParse({
        valid: false,
        submission: null,
        errors: [msg('XML_PARSE', 'bad xml', 'ERROR'), msg('JAXB', 'line 2, col 1: cvc-x: nope', 'ERROR')],
      }),
    );

    const { container } = renderPage();
    selectFile(container);

    expect(await screen.findByText('2 issues found in submission')).toBeInTheDocument();
    expect(mockValidate).not.toHaveBeenCalled();
  });

  it('parse network error (plain Error) shows Upload failed notification', async () => {
    mockParse.mockRejectedValue(new Error('network'));

    const { container } = renderPage();
    selectFile(container);

    expect(await screen.findByText('Upload failed.')).toBeInTheDocument();
    expect(screen.getByText('Could not reach the server. Please try again.')).toBeInTheDocument();
  });

  it('business validation partial via thrown 422 shows warning summary, inline field + banners', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockRejectedValue(
      envelopeError(
        makeValidation({
          valid: false,
          code: 'PARTIALLY_ACCEPTED',
          acceptedInvoices: ['INV-001'],
          rejectedInvoices: ['INV-002'],
          errors: [
            msg('invoice.date.required.error', 'invoice #1 (INV-001): Invoice date is required.', 'ERROR'),
            msg('invoice.grade.invalid.required.error', 'invoice #1 (INV-001), line 1: Grade invalid.', 'ERROR'),
            msg(
              'invoice.submitter.client.location.invalid.error',
              'submission: Submitter client/location invalid.',
              'ERROR',
            ),
            msg('invoice.month.completed.warning', 'submission: Month may be incomplete.', 'WARNING'),
            msg('some.generic.error', 'submission: General submission problem.', 'ERROR'),
          ],
        }),
      ),
    );

    await uploadAndSettle();

    expect(await screen.findByText('Validation issues found.')).toBeInTheDocument();
    expect(
      screen.getByText('1 invoice(s) accepted, 1 rejected. Correct the highlighted issues and resubmit.'),
    ).toBeInTheDocument();

    // Form-level (unmapped) banner still sits at the top of the page.
    expect(screen.getByText('General submission problem.')).toBeInTheDocument();
    // Per-invoice issues are now surfaced locally in the invoice's expanded panel.
    expect(screen.getByText('Issues for INV-001')).toBeInTheDocument();
    expect(screen.getByText('Invoice date is required.')).toBeInTheDocument();
    // The old row-context banner labels are no longer duplicated at the top.
    expect(screen.queryByText('Invoice #1 (INV-001)')).not.toBeInTheDocument();
    expect(screen.queryByText('Invoice #1 (INV-001), line 1')).not.toBeInTheDocument();

    // Submission-level field highlight (invalidText rendered inline on both mapped fields).
    expect(screen.getAllByText('Submitter client/location invalid.').length).toBeGreaterThanOrEqual(1);
    // Warning routed to monthComplete field.
    expect(screen.getByText('Month may be incomplete.')).toBeInTheDocument();

    // Editing a field that carries a server issue clears it (setIssues delete branch).
    fireEvent.change(screen.getByLabelText('Month Complete'), { target: { value: 'N' } });
    await waitFor(() => expect(screen.queryByText('Month may be incomplete.')).not.toBeInTheDocument());
  });

  it('renders plural invoice/line-item counts for multiple rows', async () => {
    mockParse.mockResolvedValue(
      makeParse({
        submission: makeSubmission({
          invoices: [
            makeSubmission().invoices[0],
            { ...makeSubmission().invoices[0], index: 2, invoiceNumber: 'INV-002' },
          ],
          lineItems: [makeSubmission().lineItems[0], { ...makeSubmission().lineItems[0], lineIndex: 2 }],
        }),
      }),
    );
    mockValidate.mockResolvedValue(makeValidation({ acceptedInvoices: ['INV-001', 'INV-002'] }));

    await uploadAndSettle();

    expect(await screen.findByText('2 invoice entries · 2 line item entries')).toBeInTheDocument();
  });

  it('auto-expands every invoice by default and supports expand/collapse all', async () => {
    mockParse.mockResolvedValue(
      makeParse({
        submission: makeSubmission({
          invoices: [
            makeSubmission().invoices[0],
            { ...makeSubmission().invoices[0], index: 2, invoiceNumber: 'INV-002' },
          ],
          lineItems: [
            makeSubmission().lineItems[0],
            { ...makeSubmission().lineItems[0], invoiceIndex: 2, invoiceNumber: 'INV-002' },
          ],
        }),
      }),
    );
    mockValidate.mockResolvedValue(makeValidation({ acceptedInvoices: ['INV-001', 'INV-002'] }));

    const { container } = await uploadAndSettle();
    await screen.findByText('Validation passed.');

    // Carbon keeps the expanded content mounted and toggles visibility via the
    // parent row's expanded state, so assert on aria-expanded (not DOM presence).
    const expandButtons = () =>
      Array.from(container.querySelectorAll('tbody .cds--table-expand__button')) as HTMLElement[];

    // Every invoice starts expanded, with each invoice's own line-items panel.
    expect(expandButtons()).toHaveLength(2);
    expect(expandButtons().every((b) => b.getAttribute('aria-expanded') === 'true')).toBe(true);
    expect(screen.getByText('Line items for INV-001 (1)')).toBeInTheDocument();
    expect(screen.getByText('Line items for INV-002 (1)')).toBeInTheDocument();

    // Collapse all closes them.
    fireEvent.click(screen.getByText('Collapse all'));
    await waitFor(() => expect(expandButtons().every((b) => b.getAttribute('aria-expanded') === 'false')).toBe(true));

    // Expand all opens them again.
    fireEvent.click(screen.getByText('Expand all'));
    await waitFor(() => expect(expandButtons().every((b) => b.getAttribute('aria-expanded') === 'true')).toBe(true));
  });

  it('expands an invoice with no line items to a friendly empty state', async () => {
    mockParse.mockResolvedValue(
      makeParse({
        submission: makeSubmission({
          invoices: [makeSubmission().invoices[0]],
          lineItems: [], // this invoice has no line items
        }),
      }),
    );
    mockValidate.mockResolvedValue(makeValidation());

    await uploadAndSettle();
    await screen.findByText('Validation passed.');

    // The expanded invoice shows a zero count and the empty-state message.
    expect(screen.getByText('Line items for INV-001 (0)')).toBeInTheDocument();
    expect(screen.getByText('This invoice has no line items.')).toBeInTheDocument();
  });

  it('rolls per-invoice flags into a row badge and a local issue list', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockRejectedValue(
      envelopeError(
        makeValidation({
          valid: false,
          code: 'PARTIALLY_ACCEPTED',
          acceptedInvoices: [],
          rejectedInvoices: ['INV-001'],
          errors: [
            // An invoice-level error and a line-item warning on the same invoice.
            msg('invoice.date.required.error', 'invoice #1 (INV-001): Invoice date is required.', 'ERROR'),
            msg('invoice.grade.z.warning', 'invoice #1 (INV-001), line 1: Grade Z used.', 'WARNING'),
          ],
        }),
      ),
    );

    await uploadAndSettle();
    await screen.findByText('Validation issues found.');

    // Row badge rolls up the invoice's own + its line-item issues.
    expect(screen.getByText('1 error, 1 warning')).toBeInTheDocument();
    // Local list under the invoice enumerates them, next to the data.
    expect(screen.getByText('Issues for INV-001')).toBeInTheDocument();
    expect(screen.getByText('Invoice date is required.')).toBeInTheDocument();
    expect(screen.getByText('Grade Z used.')).toBeInTheDocument();
  });

  it('shows inline field markers in the Invoice details card, not on the header row', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockRejectedValue(
      envelopeError(
        makeValidation({
          valid: false,
          code: 'PARTIALLY_ACCEPTED',
          acceptedInvoices: [],
          rejectedInvoices: ['INV-001'],
          errors: [
            // Maps to a summary field (shown only in the header row).
            msg('invoice.date.required.error', 'invoice #1 (INV-001): Invoice date is required.', 'ERROR'),
            // Maps to a details-card field.
            msg('invoice.seller.client.location.invalid.error', 'invoice #1 (INV-001): Seller location invalid.', 'ERROR'),
          ],
        }),
      ),
    );

    await uploadAndSettle();
    await screen.findByText('Validation issues found.');

    // The details-card field carries an inline icon whose tooltip is the message.
    expect(screen.getByTitle('Seller location invalid.')).toBeInTheDocument();
    // The summary-field issue is NOT surfaced as an inline marker on the header row.
    expect(screen.queryByTitle('Invoice date is required.')).not.toBeInTheDocument();
  });

  it('business validation failed (non-partial) via thrown 422 shows error summary', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockRejectedValue(
      envelopeError(
        makeValidation({
          valid: false,
          code: 'REJECTED',
          acceptedInvoices: [],
          rejectedInvoices: ['INV-001'],
          errors: [msg('some.generic.error', 'submission: Everything failed.', 'ERROR')],
        }),
      ),
    );

    await uploadAndSettle();

    expect(
      await screen.findByText(
        'Submission failed business validation. Correct the highlighted issues and upload again.',
      ),
    ).toBeInTheDocument();
    expect(screen.getByText('Everything failed.')).toBeInTheDocument();
  });

  it('business validation network error (plain Error) shows network-error', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockRejectedValue(new Error('network'));

    const { container } = renderPage();
    selectFile(container);

    expect(await screen.findByText('Upload failed.')).toBeInTheDocument();
  });

  it('editing a field updates its value and clears a prior client-side error', async () => {
    // Seed invalid client number so client-side validation fails on submit.
    mockParse.mockResolvedValue(makeParse({ submission: makeSubmission({ submissionClientNumber: '123' }) }));
    mockValidate.mockResolvedValue(makeValidation());

    await uploadAndSettle();
    await screen.findByText('Validation passed.');

    fireEvent.click(screen.getByRole('button', { name: 'Submit' }));

    // submitSubmission not called; inline error surfaces.
    expect(mockSubmit).not.toHaveBeenCalled();
    expect(await screen.findByText('Submission client number must be exactly 8 digits.')).toBeInTheDocument();

    // Editing clears the error and updates the value.
    const clientInput = screen.getByLabelText('Submission Client Number') as HTMLInputElement;
    fireEvent.change(clientInput, { target: { value: '99998888' } });
    expect(clientInput.value).toBe('99998888');
    await waitFor(() =>
      expect(screen.queryByText('Submission client number must be exactly 8 digits.')).not.toBeInTheDocument(),
    );
  });

  it('submit client-side validation failure with blank fields does not call submitSubmission', async () => {
    mockParse.mockResolvedValue(
      makeParse({
        submission: makeSubmission({
          submissionClientNumber: '',
          submissionClientLocnCode: '',
          monthComplete: '',
          sellerSubmission: '',
        }),
      }),
    );
    mockValidate.mockResolvedValue(makeValidation());

    await uploadAndSettle();
    await screen.findByText('Validation passed.');

    fireEvent.click(screen.getByRole('button', { name: 'Submit' }));

    expect(mockSubmit).not.toHaveBeenCalled();
    expect(await screen.findByText('Submission client number is required.')).toBeInTheDocument();
    expect(screen.getByText('Month complete is required.')).toBeInTheDocument();
  });

  it('submit success navigates to submission history', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockResolvedValue(makeValidation());
    mockSubmit.mockResolvedValue(makeSubmit({ valid: true, submissionId: 42 }));

    await uploadAndSettle();
    await screen.findByText('Validation passed.');

    fireEvent.click(screen.getByRole('button', { name: 'Submit' }));

    await waitFor(() => expect(mockSubmit).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/submission-history/42'));
  });

  it('submit rejected (resolved valid:false) surfaces issues without navigating', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockResolvedValue(makeValidation());
    mockSubmit.mockResolvedValue(
      makeSubmit({
        valid: false,
        code: 'PARTIALLY_ACCEPTED',
        submissionId: null,
        acceptedInvoices: ['INV-001'],
        rejectedInvoices: ['INV-002'],
        errors: [msg('some.generic.error', 'submission: Rejected at submit.', 'ERROR')],
      }),
    );

    await uploadAndSettle();
    await screen.findByText('Validation passed.');

    fireEvent.click(screen.getByRole('button', { name: 'Submit' }));

    expect(await screen.findByText('Validation issues found.')).toBeInTheDocument();
    expect(screen.getByText('Rejected at submit.')).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('submit rejected via thrown 422 envelope surfaces issues', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockResolvedValue(makeValidation());
    mockSubmit.mockRejectedValue(
      envelopeError(
        makeSubmit({
          valid: false,
          code: 'REJECTED',
          submissionId: null,
          acceptedInvoices: [],
          rejectedInvoices: ['INV-001'],
          errors: [msg('some.generic.error', 'submission: Thrown submit failure.', 'ERROR')],
        }),
      ),
    );

    await uploadAndSettle();
    await screen.findByText('Validation passed.');

    fireEvent.click(screen.getByRole('button', { name: 'Submit' }));

    expect(await screen.findByText('Thrown submit failure.')).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('submit network error (plain Error) shows network-error', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockResolvedValue(makeValidation());
    mockSubmit.mockRejectedValue(new Error('network'));

    await uploadAndSettle();
    await screen.findByText('Validation passed.');

    fireEvent.click(screen.getByRole('button', { name: 'Submit' }));

    expect(await screen.findByText('Upload failed.')).toBeInTheDocument();
  });

  it('clear button resets back to idle', async () => {
    mockParse.mockResolvedValue(makeParse());
    mockValidate.mockResolvedValue(makeValidation());

    await uploadAndSettle();
    await screen.findByText('Validation passed.');

    fireEvent.click(screen.getByRole('button', { name: 'Clear' }));

    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Submission Details' })).not.toBeInTheDocument());
    expect(screen.getByText('No XML file uploaded.')).toBeInTheDocument();
  });
});
