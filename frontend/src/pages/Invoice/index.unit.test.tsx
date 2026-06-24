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
    usePermission: vi.fn(() => true),
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

vi.mock('react-router-dom', () => ({
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

  it('hydrates an existing invoice: number, status tag, line items', async () => {
    await renderLoaded();
    expect(screen.getByDisplayValue('INV-001')).toBeInTheDocument();
    expect(screen.getByText('DFT')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add new item' })).toBeInTheDocument();
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
});
