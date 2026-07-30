import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import EditableLineItemsTable, { type EditableLineItemDraft, type EditableLineItemRow } from './index';

const ROWS: EditableLineItemRow[] = [
  {
    id: '1',
    secondarySort: '01',
    species: 'FI',
    clientSecondarySort: 'C01',
    numberPieces: 10,
    grade: 'A',
    volume: 25.5,
    price: 100,
    amount: 2550,
  },
  {
    id: '2',
    secondarySort: '02',
    species: 'CE',
    clientSecondarySort: 'C02',
    numberPieces: 5,
    grade: 'B',
    volume: 10,
    price: 50,
    amount: 500,
  },
];

const LOOKUPS = [
  { code: '01', description: 'Sort one' },
  { code: '02', description: 'Sort two' },
];
const SPECIES = [
  { code: 'FI', description: 'Fir' },
  { code: 'CE', description: 'Cedar' },
];
const GRADES = [
  { code: 'A', description: 'Grade A' },
  { code: 'B', description: 'Grade B' },
];
const COMBOS = [
  { species: 'FI', grade: 'A' },
  { species: 'CE', grade: 'B' },
];

const DRAFT: EditableLineItemDraft = {
  id: '1',
  secondSort: '01',
  species: 'FI',
  grade: 'A',
  numOfPieces: '10',
  volume: '2.5',
  price: '10',
  clientSecondarySort: 'C01',
};

describe('EditableLineItemsTable', () => {
  const handlers = {
    onStartEdit: vi.fn(),
    onCancelEdit: vi.fn(),
    onSaveEdit: vi.fn(),
    onDraftChange: vi.fn(),
    onDeleteItem: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const arrange = (overrides: Partial<Parameters<typeof EditableLineItemsTable>[0]> = {}) =>
    render(
      <EditableLineItemsTable
        rows={ROWS}
        canEdit
        sortCodeItems={LOOKUPS}
        speciesItems={SPECIES}
        gradeItems={GRADES}
        speciesGradeCombos={COMBOS}
        editDraft={null}
        fieldErrors={{}}
        {...handlers}
        {...overrides}
      />,
    );

  it('renders formatted read-only cells for each row', () => {
    arrange();

    expect(screen.getByRole('heading', { name: 'Details' })).toBeInTheDocument();
    expect(screen.getByText('FI')).toBeInTheDocument();
    expect(screen.getByText('25.500')).toBeInTheDocument();
    expect(screen.getByText('$2,550.00')).toBeInTheDocument();
    expect(screen.getByText('$500.00')).toBeInTheDocument();
  });

  it('starts an edit for the clicked row', async () => {
    const user = userEvent.setup();
    arrange();

    await user.click(screen.getAllByRole('button', { name: /edit line item/i })[0]);

    expect(handlers.onStartEdit).toHaveBeenCalledWith('1');
  });

  it('deletes the clicked row', async () => {
    const user = userEvent.setup();
    arrange();

    await user.click(screen.getAllByRole('button', { name: /delete line item/i })[1]);

    expect(handlers.onDeleteItem).toHaveBeenCalledWith('2');
  });

  it('hides the edit and delete actions in read-only mode', () => {
    arrange({ canEdit: false });

    expect(screen.queryByRole('button', { name: /edit line item/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /delete line item/i })).not.toBeInTheDocument();
  });

  it('can allow deletes while edits are disabled', () => {
    arrange({ canEdit: false, canDelete: true });

    expect(screen.queryByRole('button', { name: /edit line item/i })).not.toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /delete line item/i })).toHaveLength(ROWS.length);
  });

  it('disables the other rows actions while a row is being edited', () => {
    arrange({ editDraft: DRAFT });

    // Row 2 is not in edit mode; its actions are present but disabled.
    expect(screen.getByRole('button', { name: /edit line item/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /delete line item/i })).toBeDisabled();
  });

  it('renders editable inputs and a computed amount for the row in edit mode', () => {
    arrange({ editDraft: DRAFT });

    expect(screen.getByRole('button', { name: /save line item/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^cancel$/i })).toBeInTheDocument();
    // price 10 × volume 2.5 = 25.00 preview
    expect(screen.getByText('$25.00')).toBeInTheDocument();
  });

  it('shows an empty amount preview while price or volume is not numeric', () => {
    arrange({ editDraft: { ...DRAFT, price: '' } });

    // With a blank price, no computed preview appears for the edit row.
    expect(screen.queryByText('$25.00')).not.toBeInTheDocument();
  });

  it('propagates typed changes to the draft', async () => {
    const user = userEvent.setup();
    arrange({ editDraft: DRAFT });

    // The volume input is the only field showing "2.5" in the edit row.
    const volume = screen.getByDisplayValue('2.5');
    await user.type(volume, '0');

    expect(handlers.onDraftChange).toHaveBeenCalledWith(expect.objectContaining({ volume: '2.50' }));
  });

  it('saves and cancels through the row action buttons', async () => {
    const user = userEvent.setup();
    arrange({ editDraft: DRAFT });

    // Grab both action buttons up front — clicking toggles the icon tooltip,
    // which changes the computed accessible name for a later query.
    const saveButton = screen.getByRole('button', { name: /save line item/i });
    const cancelButton = screen.getByRole('button', { name: /^cancel$/i });

    await user.click(saveButton);
    expect(handlers.onSaveEdit).toHaveBeenCalledTimes(1);

    await user.click(cancelButton);
    expect(handlers.onCancelEdit).toHaveBeenCalledTimes(1);
  });

  it('flags invalid fields with the supplied error text', () => {
    arrange({ editDraft: DRAFT, fieldErrors: { pieces: 'Pieces must be a whole number' } });

    expect(screen.getByText('Pieces must be a whole number')).toBeInTheDocument();
  });
});
