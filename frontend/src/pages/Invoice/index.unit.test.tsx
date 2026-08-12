import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import { InvoicePage } from './index';

// ---------------------------------------------------------------------------
// Shared, hoisted mock state (vi.mock factories run before module init).
// ---------------------------------------------------------------------------
const h = vi.hoisted(() => {
  const makeMutation = () => ({
    mutate: vi.fn(),
    mutateAsync: vi.fn(),
    isPending: false,
    variables: undefined as unknown,
  });
  return {
    addNotification: vi.fn(),
    navigate: vi.fn(),
    downloadBlob: vi.fn(),
    getClientsByNumber: vi.fn(),
    getClientsByName: vi.fn(),
    usePermission: vi.fn((_action: string) => true),
    authUser: { username: 'cognito-id', idirUsername: 'TESTUSER' } as {
      username: string;
      idirUsername?: string;
    } | null,
    params: { id: undefined as string | undefined },
    invoiceQuery: { data: undefined as unknown, isLoading: false },
    mutations: {
      create: makeMutation(),
      update: makeMutation(),
      submit: makeMutation(),
      duplicate: makeMutation(),
      del: makeMutation(),
      changeStatus: makeMutation(),
      addLine: makeMutation(),
      delLine: makeMutation(),
      updateLine: makeMutation(),
      exp: makeMutation(),
    },
  };
});

vi.mock('react-router', () => ({
  useParams: () => h.params,
  useNavigate: () => h.navigate,
  useLocation: () => ({ pathname: '/invoice', search: '', state: null }),
}));

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: () => ({ addNotification: h.addNotification }),
}));

vi.mock('@/context/auth/usePermission', () => ({
  usePermission: (p: string) => h.usePermission(p),
}));

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: () => ({ user: h.authUser }),
}));

vi.mock('@/utils/report', () => ({
  downloadBlob: (...args: unknown[]) => h.downloadBlob(...args),
  parseContentDispositionFilename: () => null,
}));

vi.mock('@/services/search.service', () => ({
  getClientsByNumber: (...a: unknown[]) => h.getClientsByNumber(...a),
  getClientsByName: (...a: unknown[]) => h.getClientsByName(...a),
}));

vi.mock('@/services/fob.service', () => ({
  useFobCodesQuery: () => ({ data: [{ code: 'FOB01', description: 'FOB One' }] }),
}));

// PageTitle pulls in the PageTitle context/provider; stub it to a passthrough
// that still renders its children (the status tag lives inside it).
vi.mock('@/components/core/PageTitle', () => ({
  default: ({ children }: { children?: React.ReactNode }) => <div data-testid="page-title">{children}</div>,
}));

vi.mock('@/services/lookup.service', () => ({
  useInvoiceTypesQuery: () => ({ data: [{ code: 'OR', description: 'Original' }] }),
  useMaturityCodesQuery: () => ({ data: [{ code: 'O', description: 'Old growth' }] }),
  useSpeciesLookupQuery: () => ({ data: [{ code: 'FIR', description: 'Fir' }] }),
  useSortCodesLookupQuery: () => ({ data: [{ code: 'SORT01', description: 'Sort One' }] }),
  useGradeLookupQuery: () => ({ data: [{ code: '1', description: 'Grade One' }] }),
  useSpeciesGradeCombosQuery: () => ({ data: [] }),
}));

vi.mock('@/services/invoice.service', () => ({
  useInvoiceQuery: () => h.invoiceQuery,
  useCreateInvoiceMutation: () => h.mutations.create,
  useUpdateInvoiceMutation: () => h.mutations.update,
  useSubmitInvoiceMutation: () => h.mutations.submit,
  useDuplicateInvoiceMutation: () => h.mutations.duplicate,
  useDeleteInvoiceMutation: () => h.mutations.del,
  useChangeInvoiceStatusMutation: () => h.mutations.changeStatus,
  useAddInvoiceLineItemMutation: () => h.mutations.addLine,
  useDeleteInvoiceLineItemMutation: () => h.mutations.delLine,
  useUpdateInvoiceLineItemMutation: () => h.mutations.updateLine,
  useExportInvoiceGroupSummaryMutation: () => h.mutations.exp,
  extractApiErrorMessage: () => 'An error occurred.',
  extractValidationErrors: () => [],
}));

// ---------------------------------------------------------------------------
// Fixtures + helpers
// ---------------------------------------------------------------------------
const CLIENT = {
  clientNumber: '123',
  clientName: 'Acme Logging',
  clientLocnCode: '00',
  clientLocnName: 'HQ',
  city: 'Victoria',
  province: 'BC',
};

const LINE_ITEM = {
  lineItemID: 1,
  invoiceID: 1,
  secondSort: 'SORT01',
  clientSecondarySort: '',
  species: 'FIR',
  speciesDescription: 'Fir',
  grade: '1',
  numOfPieces: 10,
  price: 100,
  volume: 5,
  convertedPrice: null as number | null,
  amount: 500,
};

const makeInvoice = (overrides: Record<string, any> = {}) => ({
  invID: 1,
  invNumber: 'INV-001',
  invType: 'OR',
  invoiceDate: '2026-01-15',
  invStatus: 'DFT',
  submissionId: 10,
  submissionNumber: 67890,
  replaceInvNum: '',
  adjustInvNum: '',
  submittedBy: 'Seller',
  submitterClientNum: '123',
  submitterLocation: '00',
  otherClientNum: '456',
  otherClientLocation: '00',
  otherClientName: '',
  otherClientCity: '',
  otherClientProvState: '',
  maturity: 'O',
  fobCode: 'FOB01',
  primarySortCode: 'P1',
  boomNumbers: [],
  timberMarks: [],
  weightSlips: [],
  reviewComments: '',
  submitComments: '',
  entryUserID: 'user1',
  lineItems: [{ ...LINE_ITEM }],
  warnings: [],
  errors: [],
  ...overrides,
});

const renderPage = () => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <InvoicePage />
    </QueryClientProvider>,
  );
};

// Render an existing invoice and wait until it finishes loading (skeleton gone).
const renderLoaded = async (overrides: Record<string, any> = {}) => {
  h.params.id = '1';
  h.invoiceQuery = { data: makeInvoice(overrides), isLoading: false };
  const utils = renderPage();
  await screen.findByRole('button', { name: 'Save' }, { timeout: 5000 });
  return utils;
};

beforeEach(() => {
  vi.clearAllMocks();
  h.params.id = undefined;
  h.invoiceQuery = { data: undefined, isLoading: false };
  h.usePermission.mockReturnValue(true);
  h.authUser = { username: 'cognito-id', idirUsername: 'TESTUSER' };
  h.getClientsByNumber.mockResolvedValue([CLIENT]);
  h.getClientsByName.mockResolvedValue([CLIENT]);
  Object.values(h.mutations).forEach((m) => {
    m.mutate.mockReset();
    m.isPending = false;
    m.variables = undefined;
  });
});

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
describe('InvoicePage — rendering', () => {
  it('shows a loading skeleton while an existing invoice is loading', () => {
    h.params.id = '1';
    h.invoiceQuery = { data: undefined, isLoading: true };
    const { container } = renderPage();
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
    expect(container.querySelector('.cds--skeleton')).not.toBeNull();
  });

  it('renders a NEW invoice form (no "Add new item", Save disabled until required fields filled)', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Add new item' })).not.toBeInTheDocument();
  });

  it('defaults a NEW invoice maturity to Old growth', () => {
    renderPage();
    // maturityCode is seeded to "O" on a new record → the dropdown shows its label.
    expect(screen.getByText('Old growth')).toBeInTheDocument();
  });

  it('autofills "Entered/Submitted by" with the signed-in IDIR on a NEW invoice', () => {
    renderPage();
    expect(screen.getByText('TESTUSER')).toBeInTheDocument();
  });

  it('falls back to the Cognito username when no IDIR claim is present', () => {
    h.authUser = { username: 'cognito-id' };
    renderPage();
    expect(screen.getByText('cognito-id')).toBeInTheDocument();
  });

  it('shows a dash for "Entered/Submitted by" when there is no signed-in user', () => {
    h.authUser = null;
    const { container } = renderPage();
    const metaValues = Array.from(container.querySelectorAll('.invoice-page__meta-value'));
    expect(metaValues.some((el) => el.textContent === '—')).toBe(true);
  });

  it('shows the stored entryUserID, not the signed-in user, on an existing invoice', async () => {
    await renderLoaded();
    expect(screen.getByText('user1')).toBeInTheDocument();
    expect(screen.queryByText('TESTUSER')).not.toBeInTheDocument();
  });

  it('strips the domain qualifier from a legacy entryUserID', async () => {
    await renderLoaded({ entryUserID: 'IDIR\\JSMITH' });
    expect(screen.getByText('JSMITH')).toBeInTheDocument();
    expect(screen.queryByText('IDIR\\JSMITH')).not.toBeInTheDocument();
  });

  it('hydrates an existing invoice: number, status tag, line items', async () => {
    await renderLoaded();
    expect(screen.getByDisplayValue('INV-001')).toBeInTheDocument();
    expect(screen.getByText('DFT')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add new item' })).toBeInTheDocument();
  });

  it('uppercases characters typed into the invoice number field', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    fireEvent.change(screen.getByLabelText(/Invoice number/), { target: { value: 'inv-abc' } });
    expect(screen.getByLabelText(/Invoice number/)).toHaveValue('INV-ABC');
  });
});

describe('InvoicePage — invoice not found', () => {
  it('shows an "Invoice not found" banner when the id in the URL is not a valid number', () => {
    h.params.id = 'not-a-number';
    renderPage();
    expect(screen.getByText('Invoice not found')).toBeInTheDocument();
    expect(
      screen.getByText('Invoice not found. It may have been removed, or the link is incorrect.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
  });

  it('shows an "Invoice not found" banner when the invoice query 404s', () => {
    h.params.id = '999';
    h.invoiceQuery = {
      data: undefined,
      isLoading: false,
      isError: true,
      error: { response: { status: 404 } },
    };
    renderPage();
    expect(screen.getByText('Invoice not found')).toBeInTheDocument();
    expect(
      screen.getByText('Invoice not found. It may have been removed, or the link is incorrect.'),
    ).toBeInTheDocument();
  });

  it('shows the server error message when the invoice query fails for a non-404 reason', () => {
    h.params.id = '5';
    h.invoiceQuery = {
      data: undefined,
      isLoading: false,
      isError: true,
      error: { response: { status: 500, data: { message: 'Database is unavailable.' } } },
    };
    renderPage();
    expect(screen.getByText('Invoice not found')).toBeInTheDocument();
    expect(screen.getByText('Database is unavailable.')).toBeInTheDocument();
  });

  it('falls back to a generic message for a non-404 failure with no server message', () => {
    h.params.id = '5';
    h.invoiceQuery = {
      data: undefined,
      isLoading: false,
      isError: true,
      error: { response: {} },
    };
    renderPage();
    expect(screen.getByText('Invoice not found')).toBeInTheDocument();
    expect(screen.getByText('Failed to load the invoice. Please try again.')).toBeInTheDocument();
  });
});

describe('InvoicePage — button enablement by status', () => {
  it('DFT: Save/Submit/Duplicate/Delete enabled; Approve/Cancel/Reject disabled', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Submit' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Duplicate' })).toBeEnabled();
    expect(screen.getByRole('button', { name: /delete/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Reject' })).toBeDisabled();
  });

  it('PRO: Approve/Cancel/Reject enabled; Submit disabled', async () => {
    await renderLoaded({ invStatus: 'PRO' });
    expect(screen.getByRole('button', { name: 'Approve' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Reject' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Submit' })).toBeDisabled();
  });

  it('APP: shows Unapprove instead of Approve, and disables Delete', async () => {
    await renderLoaded({ invStatus: 'APP' });
    expect(screen.getByRole('button', { name: 'Unapprove' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /delete/i })).toBeDisabled();
  });

  it('REJ: header is locked (Save + fields disabled) but line items can still be added', async () => {
    await renderLoaded({ invStatus: 'REJ' });
    expect(screen.getByDisplayValue('INV-001')).toBeDisabled(); // header field locked
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    // Lines remain editable: "Add new item" is offered on a rejected invoice.
    expect(screen.getByRole('button', { name: 'Add new item' })).toBeInTheDocument();
  });

  it('APP: every invoice information field is disabled except the reviewer comment', async () => {
    await renderLoaded({ invStatus: 'APP' });
    expect(screen.getByDisplayValue('INV-001')).toBeDisabled(); // invoice number
    expect(screen.getByLabelText('Submitted comment')).toBeDisabled();
    expect(screen.getByLabelText('Client primary sort code')).toBeDisabled();
    // The reviewer comment stays editable so the invoice can be unapproved.
    expect(screen.getByLabelText('Reviewer comment')).toBeEnabled();
  });
});

describe('InvoicePage — permission gating (viewer)', () => {
  it('disables action buttons and hides "Add new item" without permissions', async () => {
    h.usePermission.mockReturnValue(false);
    await renderLoaded({ invStatus: 'DFT' });
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Submit' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Duplicate' })).toBeDisabled();
    expect(screen.getByRole('button', { name: /delete/i })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Add new item' })).not.toBeInTheDocument();
  });
});

describe('InvoicePage — action flows', () => {
  it('Save fires the update mutation for an existing invoice', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(h.mutations.update.mutate).toHaveBeenCalled();
  });

  it('Save keeps the client primary sort code independent of the primary sort code', async () => {
    await renderLoaded({ invStatus: 'DFT', primarySortCode: 'SORT01', clientPrimarySortCode: 'CLIENT-XYZ' });
    expect(screen.getByLabelText('Client primary sort code')).toHaveValue('CLIENT-XYZ');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(h.mutations.update.mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        body: expect.objectContaining({ primarySortCode: 'SORT01', clientPrimarySortCode: 'CLIENT-XYZ' }),
      }),
      expect.anything(),
    );
  });

  it('Submit fires the submit mutation with the invoice id', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Submit' }));
    expect(h.mutations.submit.mutate).toHaveBeenCalledWith(1, expect.anything());
  });

  it('Submit is disabled when there are no line items', async () => {
    await renderLoaded({ invStatus: 'DFT', lineItems: [] });
    expect(screen.getByRole('button', { name: 'Submit' })).toBeDisabled();
  });

  it('Duplicate fires the duplicate mutation with the invoice id', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Duplicate' }));
    expect(h.mutations.duplicate.mutate).toHaveBeenCalledWith(1, expect.anything());
  });

  it('Delete opens a confirmation modal and fires the delete mutation on confirm', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: /delete/i }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/delete invoice/i)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: /delete/i }));
    expect(h.mutations.del.mutate).toHaveBeenCalledWith(1, expect.anything());
  });

  it('Approve (PROCESSING) fires a status change to APP', async () => {
    await renderLoaded({ invStatus: 'PRO' });
    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));
    expect(h.mutations.changeStatus.mutate).toHaveBeenCalledWith(
      expect.objectContaining({ id: 1, body: expect.objectContaining({ status: 'APP' }) }),
      expect.anything(),
    );
  });

  it('Reject without a reviewer comment is blocked and shows an inline error', async () => {
    await renderLoaded({ invStatus: 'PRO' });
    await userEvent.click(screen.getByRole('button', { name: 'Reject' }));
    expect(h.mutations.changeStatus.mutate).not.toHaveBeenCalled();
    expect(screen.getByText(/reviewer comment is required/i)).toBeInTheDocument();
  });

  it('Unapprove (APPROVED) fires a status change to UNA once a comment is entered', async () => {
    await renderLoaded({ invStatus: 'APP' });
    fireEvent.change(screen.getByLabelText('Reviewer comment'), { target: { value: 'Reverting' } });
    await userEvent.click(screen.getByRole('button', { name: 'Unapprove' }));
    expect(h.mutations.changeStatus.mutate).toHaveBeenCalledWith(
      expect.objectContaining({ body: expect.objectContaining({ status: 'UNA', reviewComments: 'Reverting' }) }),
      expect.anything(),
    );
  });
});

describe('InvoicePage — field length validation', () => {
  it('shows an inline error when the client primary sort code exceeds 100 characters', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    fireEvent.change(screen.getByLabelText('Client primary sort code'), { target: { value: 'A'.repeat(101) } });
    expect(screen.getByText('Client primary sort code must be at most 100 characters.')).toBeInTheDocument();
  });

  it('shows an inline error when the submitted comment exceeds 4000 characters', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    fireEvent.change(screen.getByLabelText('Submitted comment'), { target: { value: 'A'.repeat(4001) } });
    expect(screen.getByText('Submitted comment must be at most 4000 characters.')).toBeInTheDocument();
  });
});

describe('InvoicePage — line items & export', () => {
  it('opens the Add New Item modal with the submit disabled until valid', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Add new item' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('Add New Item')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Add new item' })).toBeDisabled();
  });

  it('Export to CSV fires the export mutation and downloads the result', async () => {
    h.mutations.exp.mutate.mockImplementation((_vars: unknown, opts: { onSuccess: (r: unknown) => void }) => {
      opts.onSuccess({ blob: new Blob(['x']), filename: 'export.csv' });
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: /export table/i }));
    await userEvent.click(await screen.findByText('Export to CSV'));
    expect(h.mutations.exp.mutate).toHaveBeenCalledWith(expect.objectContaining({ format: 'csv' }), expect.anything());
    expect(h.downloadBlob).toHaveBeenCalled();
  });

  it('shows Y for a group whose lines have a converted price', async () => {
    await renderLoaded({
      invStatus: 'DFT',
      lineItems: [{ ...LINE_ITEM, convertedPrice: 50 }],
    });
    expect(screen.getByText('Y')).toBeInTheDocument();
  });

  it('groups line items by EXACT price (no $0.10 rounding)', async () => {
    await renderLoaded({
      invStatus: 'DFT',
      lineItems: [
        { ...LINE_ITEM, lineItemID: 1, price: 100.02 },
        { ...LINE_ITEM, lineItemID: 2, price: 100.07 },
      ],
    });
    // Same species/sort but two distinct exact prices → two separate group
    // rows (each row contributes its sort code, so >1 group means >2 matches).
    // A round-up-to-$0.10 key would merge both into a single 100.1 group.
    expect(screen.getAllByText('SORT01').length).toBeGreaterThan(2);
  });
});

describe('InvoicePage — warnings & errors', () => {
  it('renders warnings from the loaded invoice in the banner', async () => {
    await renderLoaded({
      invStatus: 'DFT',
      warnings: [{ messageKey: 'w', message: 'Heads up — check this', type: 'WARNING', args: null }],
    });
    expect(screen.getByText('Heads up — check this')).toBeInTheDocument();
  });

  it('renders page-level errors from the loaded invoice in the banner', async () => {
    await renderLoaded({
      invStatus: 'DFT',
      errors: [{ messageKey: 'some.unmapped.key', message: 'Something went wrong', type: 'ERROR', args: null }],
    });
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('shows an invalid submitter client location as an inline field error, not a banner', async () => {
    await renderLoaded({
      invStatus: 'DFT',
      errors: [
        {
          messageKey: 'invoice.submitter.client.location.invalid.error',
          message: 'The combination of the submitter Client Number 123 and Client Location 00 cannot be found in CSP.',
          type: 'ERROR',
          args: null,
        },
      ],
    });
    expect(
      screen.getByText(
        'The combination of the submitter Client Number 123 and Client Location 00 cannot be found in CSP.',
      ),
    ).toBeInTheDocument();
    expect(document.getElementById('submitting-client-location')).toBeInvalid();
  });
});
