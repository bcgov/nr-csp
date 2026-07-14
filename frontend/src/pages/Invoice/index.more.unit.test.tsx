import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import { InvoicePage } from './index';

// ---------------------------------------------------------------------------
// Shared, hoisted mock state — adapted from index.unit.test.tsx. Adds a
// controllable `extractValidationErrors` so error paths can return
// field-mapped validation errors, and mutateAsync-capable mutation mocks
// for the group edit/delete flows.
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
    extractValidationErrors: vi.fn((_err: unknown): unknown[] => []),
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

vi.mock('@/components/core/PageTitle', () => ({
  default: ({ children }: { children?: React.ReactNode }) => <div data-testid="page-title">{children}</div>,
}));

vi.mock('@/services/lookup.service', () => ({
  useInvoiceTypesQuery: () => ({ data: [{ code: 'OR', description: 'Original' }] }),
  useMaturityCodesQuery: () => ({ data: [{ code: 'O', description: 'Old growth' }] }),
  useSpeciesLookupQuery: () => ({
    data: [
      { code: 'FIR', description: 'Fir' },
      { code: 'SPR', description: 'Spruce' },
    ],
  }),
  useSortCodesLookupQuery: () => ({
    data: [
      { code: 'SORT01', description: 'Sort One' },
      { code: 'SORT02', description: 'Sort Two' },
    ],
  }),
  useGradeLookupQuery: () => ({ data: [{ code: '1', description: 'Grade One' }] }),
  useSpeciesGradeCombosQuery: () => ({
    data: [
      { species: 'FIR', grade: '1' },
      { species: 'SPR', grade: '1' },
    ],
  }),
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
  extractValidationErrors: (err: unknown) => h.extractValidationErrors(err),
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

type MutationOpts = {
  onSuccess?: (data: any) => void;
  onError?: (err: unknown) => void;
};

const renderPage = () => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <InvoicePage />
    </QueryClientProvider>,
  );
};

// Render an existing invoice and wait until it finishes loading.
const renderLoaded = async (overrides: Record<string, any> = {}) => {
  h.params.id = '1';
  h.invoiceQuery = { data: makeInvoice(overrides), isLoading: false };
  const utils = renderPage();
  await screen.findByRole('button', { name: 'Save' }, { timeout: 5000 });
  return utils;
};

// Open a Carbon Dropdown and pick an option by its rendered text.
const pickOption = async (combo: HTMLElement, optionName: string | RegExp) => {
  await userEvent.click(combo);
  await userEvent.click(await screen.findByRole('option', { name: optionName }));
};

// Carbon IconButtons take their accessible name from a Tooltip popover whose
// text is `visibility: hidden` until hovered, so `getByRole('button', { name })`
// computes an empty name in happy-dom. Locate the button through its tooltip
// text instead.
const iconButton = (scope: ParentNode, label: string): HTMLElement => {
  const holders = Array.from(scope.querySelectorAll('.cds--popover-container'));
  const holder = holders.find((s) => s.querySelector('.cds--tooltip-content')?.textContent?.startsWith(label));
  const btn = holder?.querySelector('button');
  if (!btn) throw new Error(`Icon button "${label}" not found`);
  return btn as HTMLElement;
};

beforeEach(() => {
  vi.clearAllMocks();
  h.params.id = undefined;
  h.invoiceQuery = { data: undefined, isLoading: false };
  h.usePermission.mockReturnValue(true);
  h.getClientsByNumber.mockResolvedValue([CLIENT]);
  h.getClientsByName.mockResolvedValue([CLIENT]);
  h.extractValidationErrors.mockReset();
  h.extractValidationErrors.mockReturnValue([]);
  Object.values(h.mutations).forEach((m) => {
    m.mutate.mockReset();
    m.mutateAsync.mockReset();
    m.isPending = false;
    m.variables = undefined;
  });
});

// ---------------------------------------------------------------------------
// Warning / error message relabelling ("Line #<id>" → "Group G Line L")
// ---------------------------------------------------------------------------
describe('InvoicePage — message relabelling', () => {
  it('rewrites "Line #<id>" warnings into "Group G Line L - <message>"', async () => {
    await renderLoaded({
      warnings: [{ messageKey: 'w1', message: 'Volume too low Line #1.', type: 'WARNING', args: null }],
    });
    expect(screen.getByText('Group 1 Line 1 - Volume too low.')).toBeInTheDocument();
  });

  it('drops a "Line #New" reference and keeps only the message body', async () => {
    await renderLoaded({
      warnings: [{ messageKey: 'w2', message: 'Cannot add Line #New.', type: 'WARNING', args: null }],
    });
    expect(screen.getByText('Cannot add.')).toBeInTheDocument();
  });
});

// ---------------------------------------------------------------------------
// FOB blur validation + auto-clearing of server errors on edit
// ---------------------------------------------------------------------------
describe('InvoicePage — FOB validation & server error auto-clear', () => {
  it('flags an unknown FOB code on blur and clears it while retyping', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    const fob = screen.getByLabelText(/FOB/);
    fireEvent.change(fob, { target: { value: 'BAD' } });
    fireEvent.blur(fob);
    expect(screen.getByText('FOB code "BAD" is not a valid FOB location.')).toBeInTheDocument();
    // Typing clears the inline error immediately (re-checked on next blur).
    fireEvent.change(fob, { target: { value: 'FOB01' } });
    expect(screen.queryByText(/is not a valid FOB location/)).not.toBeInTheDocument();
    fireEvent.blur(fob); // valid code → still no error
    expect(screen.queryByText(/is not a valid FOB location/)).not.toBeInTheDocument();
    // Empty value clears the error too.
    fireEvent.change(fob, { target: { value: '' } });
    fireEvent.blur(fob);
    expect(screen.queryByText(/is not a valid FOB location/)).not.toBeInTheDocument();
  });

  it('drops a server field error once the user edits that field', async () => {
    await renderLoaded({
      invStatus: 'DFT',
      errors: [{ messageKey: 'invoice.fob.required.error', message: 'FOB is required', type: 'ERROR', args: null }],
    });
    expect(screen.getByText('FOB is required')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/FOB/), { target: { value: 'FOB99' } });
    await waitFor(() => expect(screen.queryByText('FOB is required')).not.toBeInTheDocument());
  });
});

// ---------------------------------------------------------------------------
// Header field handlers
// ---------------------------------------------------------------------------
describe('InvoicePage — header field handlers', () => {
  it('shows "Seller" as the other party when submitted by is Buyer', async () => {
    await renderLoaded({ submittedBy: 'Buyer' });
    expect(
      screen.getByText((_content, el) => el?.tagName === 'H3' && /Other party \(\s*Seller\s*\)/.test(el.textContent ?? '')),
    ).toBeInTheDocument();
  });

  it('clearing then re-entering the invoice date toggles Save availability', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    const date = screen.getByLabelText(/Invoice date/);
    fireEvent.input(date, { target: { value: '' } });
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    fireEvent.input(date, { target: { value: '2026-02-05' } });
    expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled();
  });
});

// ---------------------------------------------------------------------------
// Save / create success + error handling
// ---------------------------------------------------------------------------
describe('InvoicePage — save & create flows', () => {
  it('Save success shows a toast and renders returned warnings', async () => {
    h.mutations.update.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onSuccess?.(
        makeInvoice({ warnings: [{ messageKey: 'w', message: 'Post-save warning', type: 'WARNING', args: null }] }),
      );
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: "Invoice 'INV-001' saved." }));
    expect(screen.getByText('Post-save warning')).toBeInTheDocument();
  });

  it('Save failure with validation errors puts unmapped ones in the page banner', async () => {
    h.extractValidationErrors.mockReturnValue([
      { messageKey: 'some.unmapped.key', message: 'Business rule broken', type: 'ERROR', args: null },
    ]);
    h.mutations.update.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('400'));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(screen.getByText('Business rule broken')).toBeInTheDocument();
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to save invoice.', subtitle: undefined }),
    );
  });

  it('Save failure without validation errors surfaces the API message in the toast', async () => {
    h.mutations.update.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('500'));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to save invoice.', subtitle: 'An error occurred.' }),
    );
  });

  it('Save on a NEW (id-less) invoice runs create and navigates to the new id', async () => {
    // No :id param but the invoice query returns data — hydrates every
    // required field so the create path is reachable.
    h.params.id = undefined;
    h.invoiceQuery = { data: makeInvoice(), isLoading: false };
    h.mutations.create.mutate.mockImplementation((_body: unknown, opts: MutationOpts) => {
      opts.onSuccess?.(makeInvoice({ invID: 5 }));
    });
    renderPage();
    const save = screen.getByRole('button', { name: 'Save' });
    await waitFor(() => expect(save).toBeEnabled(), { timeout: 5000 });
    await userEvent.click(save);
    expect(h.mutations.create.mutate).toHaveBeenCalled();
    expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: "Invoice 'INV-001' created." }));
    expect(h.navigate).toHaveBeenCalledWith('/invoice/5', expect.objectContaining({ replace: true }));
  });

  it('create failure shows the create-specific error toast', async () => {
    h.params.id = undefined;
    h.invoiceQuery = { data: makeInvoice(), isLoading: false };
    h.mutations.create.mutate.mockImplementation((_body: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('boom'));
    });
    renderPage();
    const save = screen.getByRole('button', { name: 'Save' });
    await waitFor(() => expect(save).toBeEnabled(), { timeout: 5000 });
    await userEvent.click(save);
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to create invoice.' }),
    );
  });
});

// ---------------------------------------------------------------------------
// Submit / status change / duplicate / delete result handling
// ---------------------------------------------------------------------------
describe('InvoicePage — submit, status change, duplicate & delete results', () => {
  it('Submit success shows the submitted toast', async () => {
    h.mutations.submit.mutate.mockImplementation((_id: unknown, opts: MutationOpts) => {
      opts.onSuccess?.(makeInvoice());
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Submit' }));
    expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: "Invoice 'INV-001' submitted." }));
  });

  it('Submit failure shows the submit error toast', async () => {
    h.mutations.submit.mutate.mockImplementation((_id: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('boom'));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Submit' }));
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to submit invoice.' }),
    );
  });

  it.each([
    ['Approve', 'PRO', false, "Invoice 'INV-001' approved."],
    ['Reject', 'PRO', true, "Invoice 'INV-001' rejected."],
    ['Cancel', 'PRO', true, "Invoice 'INV-001' cancelled."],
    ['Unapprove', 'APP', true, "Invoice 'INV-001' unapproved."],
  ])('%s success shows the verb-specific toast', async (button, status, needsComment, expectedTitle) => {
    h.mutations.changeStatus.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onSuccess?.(makeInvoice());
    });
    await renderLoaded({ invStatus: status });
    if (needsComment) {
      fireEvent.change(screen.getByLabelText('Reviewer comment'), { target: { value: 'Because.' } });
    }
    await userEvent.click(screen.getByRole('button', { name: button }));
    expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: expectedTitle }));
  });

  it('status change failure shows the status error toast', async () => {
    h.mutations.changeStatus.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('boom'));
    });
    await renderLoaded({ invStatus: 'PRO' });
    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to change invoice status.' }),
    );
  });

  it('Duplicate success navigates to the duplicated invoice', async () => {
    h.mutations.duplicate.mutate.mockImplementation((_id: unknown, opts: MutationOpts) => {
      opts.onSuccess?.(makeInvoice({ invID: 9, invNumber: 'INV-002' }));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Duplicate' }));
    expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: "Invoice 'INV-002' duplicated." }));
    expect(h.navigate).toHaveBeenCalledWith('/invoice/9', { replace: false });
  });

  it('Duplicate failure shows the duplicate error toast', async () => {
    h.mutations.duplicate.mutate.mockImplementation((_id: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('boom'));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: 'Duplicate' }));
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to duplicate invoice.' }),
    );
  });

  it('Delete success navigates back to the new-invoice screen', async () => {
    h.mutations.del.mutate.mockImplementation((_id: unknown, opts: MutationOpts) => {
      opts.onSuccess?.(undefined);
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: /delete/i }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: /delete/i }));
    expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: 'Invoice deleted.' }));
    expect(h.navigate).toHaveBeenCalledWith('/invoice', { replace: true });
  });

  it('Delete failure closes the modal and shows the delete error toast', async () => {
    h.mutations.del.mutate.mockImplementation((_id: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('boom'));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: /delete/i }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: /delete/i }));
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to delete invoice.' }),
    );
  });
});

// ---------------------------------------------------------------------------
// Line-item row edit / delete
// ---------------------------------------------------------------------------
describe('InvoicePage — line item row edit & delete', () => {
  it('starting and cancelling a row edit toggles the inline editors', async () => {
    const { container } = await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(iconButton(document.body, 'Edit line item'));
    expect(container.querySelector('#edit-1-price')).not.toBeNull();
    const table = container.querySelector('.editable-line-items-table') as HTMLElement;
    await userEvent.click(iconButton(table, 'Cancel'));
    expect(container.querySelector('#edit-1-price')).toBeNull();
  });

  it('saving a row edit fires the update-line mutation and shows a toast', async () => {
    h.mutations.updateLine.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onSuccess?.(makeInvoice({ lineItems: [{ ...LINE_ITEM, price: 123 }] }));
    });
    const { container } = await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(iconButton(document.body, 'Edit line item'));
    fireEvent.change(container.querySelector('#edit-1-price') as HTMLElement, { target: { value: '123' } });
    await userEvent.click(iconButton(document.body, 'Save line item'));
    expect(h.mutations.updateLine.mutate).toHaveBeenCalledWith(
      expect.objectContaining({ invoiceId: 1, lineId: 1, body: expect.objectContaining({ price: 123 }) }),
      expect.anything(),
    );
    expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: 'Line item updated.' }));
  });

  it('row-edit validation errors light up the inline field', async () => {
    h.extractValidationErrors.mockReturnValue([
      { messageKey: 'invoice.price.negative.value.error', message: 'Price is bad', type: 'ERROR', args: null },
    ]);
    h.mutations.updateLine.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('400'));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(iconButton(document.body, 'Edit line item'));
    await userEvent.click(iconButton(document.body, 'Save line item'));
    expect(screen.getByText('Price is bad')).toBeInTheDocument();
  });

  it('row-edit non-validation errors show the update-line error toast', async () => {
    h.mutations.updateLine.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('500'));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(iconButton(document.body, 'Edit line item'));
    await userEvent.click(iconButton(document.body, 'Save line item'));
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to update line item.', subtitle: 'An error occurred.' }),
    );
  });

  it('deleting a row confirms then fires the delete-line mutation', async () => {
    h.mutations.delLine.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onSuccess?.(makeInvoice({ lineItems: [] }));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(iconButton(document.body, 'Delete line item'));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/delete this line item/i)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: /delete/i }));
    expect(h.mutations.delLine.mutate).toHaveBeenCalledWith(
      expect.objectContaining({ invoiceId: 1, lineId: 1 }),
      expect.anything(),
    );
    expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: 'Line item deleted.' }));
  });

  it('row delete failure shows the delete-line error toast', async () => {
    h.mutations.delLine.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('boom'));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(iconButton(document.body, 'Delete line item'));
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: /delete/i }));
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to delete line item.' }),
    );
  });
});

// ---------------------------------------------------------------------------
// Group edit modal + group delete
// ---------------------------------------------------------------------------
describe('InvoicePage — group edit & delete', () => {
  const openGroupEdit = async () => {
    await userEvent.click(iconButton(document.body, 'Edit group'));
    return screen.findByRole('dialog');
  };

  it('opens the group edit modal with the group identity in the title, and cancels', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    const dialog = await openGroupEdit();
    expect(within(dialog).getByText(/Edit group — SORT01 \/ FIR/)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByText(/Edit group — SORT01 \/ FIR/)).not.toBeInTheDocument());
  });

  it('blocks group save when species is cleared', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    const dialog = await openGroupEdit();
    await pickOption(within(dialog).getByRole('combobox', { name: /species/i }), 'Select...');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(within(dialog).getByText('Species is required.')).toBeInTheDocument();
    expect(h.mutations.updateLine.mutateAsync).not.toHaveBeenCalled();
  });

  it('saving the group updates each line and shows a toast', async () => {
    h.mutations.updateLine.mutateAsync.mockResolvedValue(
      makeInvoice({ lineItems: [{ ...LINE_ITEM, secondSort: 'SORT02', species: 'SPR' }] }),
    );
    await renderLoaded({ invStatus: 'DFT' });
    const dialog = await openGroupEdit();
    await pickOption(within(dialog).getByRole('combobox', { name: /secondary sort code/i }), 'SORT02 - Sort Two');
    await pickOption(within(dialog).getByRole('combobox', { name: /species/i }), 'SPR');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    await waitFor(() =>
      expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: 'Group updated.' })),
    );
    expect(h.mutations.updateLine.mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({
        invoiceId: 1,
        lineId: 1,
        body: expect.objectContaining({ secondSort: 'SORT02', species: 'SPR' }),
      }),
    );
  });

  it('group save validation errors map onto the modal fields', async () => {
    h.mutations.updateLine.mutateAsync.mockRejectedValue(new Error('400'));
    h.extractValidationErrors.mockReturnValue([
      { messageKey: 'invoice.secondry.sortcode.invalid.error', message: 'Bad sort', type: 'ERROR', args: null },
      { messageKey: 'invoice.species.grade.combination.error', message: 'Bad combo', type: 'ERROR', args: null },
    ]);
    await renderLoaded({ invStatus: 'DFT' });
    const dialog = await openGroupEdit();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    await waitFor(() => expect(within(dialog).getByText('Bad sort')).toBeInTheDocument());
    expect(within(dialog).getByText('Bad combo')).toBeInTheDocument();
  });

  it('group save non-validation errors show the group error toast', async () => {
    h.mutations.updateLine.mutateAsync.mockRejectedValue(new Error('500'));
    await renderLoaded({ invStatus: 'DFT' });
    const dialog = await openGroupEdit();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    await waitFor(() =>
      expect(h.addNotification).toHaveBeenCalledWith(
        expect.objectContaining({ kind: 'error', title: 'Failed to update group.' }),
      ),
    );
  });

  it('deleting a group confirms, deletes each line and shows a toast', async () => {
    h.mutations.delLine.mutateAsync.mockResolvedValue(makeInvoice({ lineItems: [] }));
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(iconButton(document.body, 'Delete group'));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/delete all line items in this group/i)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: /delete/i }));
    await waitFor(() =>
      expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: 'Group deleted.' })),
    );
    expect(h.mutations.delLine.mutateAsync).toHaveBeenCalledWith({ invoiceId: 1, lineId: 1 });
  });

  it('group delete failure shows the group delete error toast', async () => {
    h.mutations.delLine.mutateAsync.mockRejectedValue(new Error('boom'));
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(iconButton(document.body, 'Delete group'));
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: /delete/i }));
    await waitFor(() =>
      expect(h.addNotification).toHaveBeenCalledWith(
        expect.objectContaining({ kind: 'error', title: 'Failed to delete group.' }),
      ),
    );
  });
});

// ---------------------------------------------------------------------------
// Add New Line Item modal
// ---------------------------------------------------------------------------
describe('InvoicePage — add new line item', () => {
  const openAddModal = async () => {
    await userEvent.click(screen.getByRole('button', { name: 'Add new item' }));
    return screen.findByRole('dialog');
  };

  const fillValidNewLine = async (dialog: HTMLElement) => {
    // Grade first so the species list is filtered by grade (and vice versa).
    await pickOption(within(dialog).getByRole('combobox', { name: /grade/i }), '1');
    await pickOption(within(dialog).getByRole('combobox', { name: /secondary sort code/i }), 'SORT01 - Sort One');
    await pickOption(within(dialog).getByRole('combobox', { name: /species/i }), 'FIR');
    fireEvent.change(within(dialog).getByLabelText(/#Pieces/), { target: { value: '5' } });
    fireEvent.change(within(dialog).getByLabelText(/Volume/), { target: { value: '2' } });
    fireEvent.change(within(dialog).getByLabelText(/\$Price/), { target: { value: '10' } });
  };

  it('adds a valid line item and closes the modal on success', async () => {
    h.mutations.addLine.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onSuccess?.(
        makeInvoice({ lineItems: [{ ...LINE_ITEM }, { ...LINE_ITEM, lineItemID: 2, secondSort: 'SORT02' }] }),
      );
    });
    await renderLoaded({ invStatus: 'DFT' });
    const dialog = await openAddModal();
    await fillValidNewLine(dialog);
    // Computed amount preview = price × volume.
    expect(within(dialog).getByLabelText(/\$Amount/)).toHaveValue('20.00');
    const submit = within(dialog).getByRole('button', { name: 'Add new item' });
    expect(submit).toBeEnabled();
    await userEvent.click(submit);
    expect(h.mutations.addLine.mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        invoiceId: 1,
        body: expect.objectContaining({ secondSort: 'SORT01', species: 'FIR', grade: '1', numOfPieces: 5 }),
      }),
      expect.anything(),
    );
    expect(h.addNotification).toHaveBeenCalledWith(expect.objectContaining({ title: 'Line item added.' }));
  });

  it('maps add-line validation errors onto the modal fields', async () => {
    h.mutations.addLine.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('400'));
    });
    h.extractValidationErrors.mockReturnValue([
      {
        messageKey: 'invoice.numberof.pieces.negative.or.zero.error',
        message: 'Pieces must be positive',
        type: 'ERROR',
        args: null,
      },
    ]);
    await renderLoaded({ invStatus: 'DFT' });
    const dialog = await openAddModal();
    await fillValidNewLine(dialog);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add new item' }));
    expect(within(dialog).getByText('Pieces must be positive')).toBeInTheDocument();
  });

  it('shows the add-line error toast for non-validation failures', async () => {
    h.mutations.addLine.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('500'));
    });
    await renderLoaded({ invStatus: 'DFT' });
    const dialog = await openAddModal();
    await fillValidNewLine(dialog);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add new item' }));
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to add line item.', subtitle: 'An error occurred.' }),
    );
  });

  it('cancelling the modal clears the form', async () => {
    await renderLoaded({ invStatus: 'DFT' });
    let dialog = await openAddModal();
    await fillValidNewLine(dialog);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    // Re-open: the form is back to its pristine (invalid) state.
    dialog = await openAddModal();
    expect(within(dialog).getByLabelText(/#Pieces/)).toHaveValue('');
    expect(within(dialog).getByRole('button', { name: 'Add new item' })).toBeDisabled();
  });
});

// ---------------------------------------------------------------------------
// Export variants
// ---------------------------------------------------------------------------
describe('InvoicePage — export', () => {
  it('Export to PDF failure shows a format-specific error toast', async () => {
    h.mutations.exp.mutate.mockImplementation((_vars: unknown, opts: MutationOpts) => {
      opts.onError?.(new Error('boom'));
    });
    await renderLoaded({ invStatus: 'DFT' });
    await userEvent.click(screen.getByRole('button', { name: /export table/i }));
    await userEvent.click(await screen.findByText('Export to PDF'));
    expect(h.mutations.exp.mutate).toHaveBeenCalledWith(expect.objectContaining({ format: 'pdf' }), expect.anything());
    expect(h.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: 'error', title: 'Failed to export the table as PDF.' }),
    );
  });
});
