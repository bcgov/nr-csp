import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';

import EditableLineItemsTable, { type EditableLineItemRow, type EditableLineItemDraft } from './index';

const sortCodeItems = [{ code: 'SORT01', description: 'Sort One' }];
const speciesItems = [{ code: 'FIR', description: 'Fir' }];
const gradeItems = [{ code: '1', description: 'Grade One' }];
const speciesGradeCombos = [{ species: 'FIR', grade: '1' }];

const row: EditableLineItemRow = {
  id: 'r1',
  secondarySort: 'SORT01',
  species: 'FIR',
  clientSecondarySort: 'CS1',
  numberPieces: 10,
  grade: '1',
  volume: 5,
  price: 100,
  amount: 500,
};

const setup = (overrides: Partial<React.ComponentProps<typeof EditableLineItemsTable>> = {}) => {
  const handlers = {
    onStartEdit: vi.fn(),
    onCancelEdit: vi.fn(),
    onSaveEdit: vi.fn(),
    onDraftChange: vi.fn(),
    onDeleteItem: vi.fn(),
  };
  render(
    <EditableLineItemsTable
      rows={[row]}
      canEdit
      sortCodeItems={sortCodeItems}
      speciesItems={speciesItems}
      gradeItems={gradeItems}
      speciesGradeCombos={speciesGradeCombos}
      editDraft={null}
      fieldErrors={{}}
      {...handlers}
      {...overrides}
    />,
  );
  return handlers;
};

describe('EditableLineItemsTable', () => {
  it('renders the Details heading and a row of display values', () => {
    setup();
    expect(screen.getByText('Details')).toBeInTheDocument();
    expect(screen.getByText('SORT01')).toBeInTheDocument();
    expect(screen.getByText('FIR')).toBeInTheDocument();
    expect(screen.getByText('CS1')).toBeInTheDocument();
  });

  it('shows edit and delete actions when canEdit is true', () => {
    setup();
    expect(screen.getByRole('button', { name: 'Edit line item' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Delete line item' })).toBeInTheDocument();
  });

  it('hides edit and delete actions when canEdit is false', () => {
    setup({ canEdit: false });
    expect(screen.queryByRole('button', { name: 'Edit line item' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Delete line item' })).not.toBeInTheDocument();
  });

  it('calls onStartEdit with the row id', async () => {
    const handlers = setup();
    await userEvent.click(screen.getByRole('button', { name: 'Edit line item' }));
    expect(handlers.onStartEdit).toHaveBeenCalledWith('r1');
  });

  it('calls onDeleteItem with the row id', async () => {
    const handlers = setup();
    await userEvent.click(screen.getByRole('button', { name: 'Delete line item' }));
    expect(handlers.onDeleteItem).toHaveBeenCalledWith('r1');
  });

  it('shows save / cancel and fires their handlers while a row is being edited', async () => {
    const draft: EditableLineItemDraft = {
      id: 'r1',
      secondSort: 'SORT01',
      species: 'FIR',
      grade: '1',
      numOfPieces: '10',
      volume: '5',
      price: '100',
      clientSecondarySort: 'CS1',
    };
    const handlers = setup({ editDraft: draft });
    const save = screen.getByRole('button', { name: 'Save line item' });
    const cancel = screen.getByRole('button', { name: 'Cancel' });
    await userEvent.click(save);
    expect(handlers.onSaveEdit).toHaveBeenCalled();
    await userEvent.click(cancel);
    expect(handlers.onCancelEdit).toHaveBeenCalled();
  });
});
