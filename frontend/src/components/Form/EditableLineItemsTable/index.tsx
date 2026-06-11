import { IconButton, TextInput } from '@carbon/react';
import { Checkmark, Close, Edit, TrashCan } from '@carbon/icons-react';

import ResultsTable, { type ResultsTableColumn } from '@/components/Form/ResultsTable';
import SingleSelect from '@/components/Form/SingleSelect';
import { formatCurrency, formatNumber } from '@/utils/format';

import './index.scss';

/** A simple lookup option as used by the sort code / species / grade dropdowns. */
export interface LookupOption {
  code: string;
  description: string;
}

/** A single (species, grade) combination used to filter the dropdowns against each other. */
export interface SpeciesGradeCombo {
  species: string;
  grade: string;
}

/** Display-row shape consumed by the table. Consumers map their own data into this. */
export interface EditableLineItemRow {
  id: string;
  secondarySort: string;
  species: string;
  clientSecondarySort: string;
  numberPieces: number;
  grade: string;
  volume: number;
  price: number;
  amount: number;
}

/** Edit-draft shape for a single in-flight row edit. Held by the consumer. */
export interface EditableLineItemDraft {
  id: string;
  secondSort: string;
  species: string;
  grade: string;
  numOfPieces: string;
  volume: string;
  price: string;
  clientSecondarySort: string;
}

export interface EditableLineItemsTableProps {
  /** Rows to render. Consumer pre-maps from its domain model. */
  rows: EditableLineItemRow[];
  /** When false, the Edit pencil is hidden (read-only view). Delete is still gated by `canDelete`. */
  canEdit: boolean;
  /** When false, the Delete trash icon is hidden. Defaults to `canEdit`. */
  canDelete?: boolean;
  sortCodeItems: LookupOption[];
  speciesItems: LookupOption[];
  gradeItems: LookupOption[];
  /** Full valid (species, grade) pairs — used to filter the dropdowns against each other while editing. */
  speciesGradeCombos: SpeciesGradeCombo[];
  /** The row currently being edited, or `null` if none. */
  editDraft: EditableLineItemDraft | null;
  /** Per-field inline validation errors for the active edit row. Keys: secondarySort, species, grade, pieces, volume, price. */
  fieldErrors: Record<string, string>;
  onStartEdit: (rowId: string) => void;
  onCancelEdit: () => void;
  onSaveEdit: () => void;
  onDraftChange: (next: EditableLineItemDraft) => void;
  onDeleteItem: (rowId: string) => void;
}

const findByCode = (items: LookupOption[], code: string | null | undefined): LookupOption | null =>
  code ? (items.find((i) => i.code === code) ?? null) : null;

/**
 * Inline-editable line items table with secondary-sort / species / client-sort /
 * pieces / grade / volume / price / amount columns. One row is editable at a
 * time, governed by `editDraft`; the consumer owns the draft + edit state.
 *
 * Species and Grade dropdowns mutually filter each other using
 * `speciesGradeCombos`. Amount is a read-only computed preview of
 * `price × volume` while editing.
 * NOTE: This is a WIP (Made specifically for Invoice details) and will be updated to accept general params for future reusability
 */
export default function EditableLineItemsTable({
  rows,
  canEdit,
  canDelete = canEdit,
  sortCodeItems,
  speciesItems,
  gradeItems,
  speciesGradeCombos,
  editDraft,
  fieldErrors,
  onStartEdit,
  onCancelEdit,
  onSaveEdit,
  onDraftChange,
  onDeleteItem,
}: EditableLineItemsTableProps) {
  const isEditingId = (id: string) => editDraft?.id === id;

  const editSpeciesAllowed = editDraft?.grade
    ? new Set(speciesGradeCombos.filter((c) => c.grade === editDraft.grade).map((c) => c.species))
    : null;
  const editGradeAllowed = editDraft?.species
    ? new Set(speciesGradeCombos.filter((c) => c.species === editDraft.species).map((c) => c.grade))
    : null;
  const editFilteredSpecies = editSpeciesAllowed
    ? speciesItems.filter((s) => editSpeciesAllowed.has(s.code))
    : speciesItems;
  const editFilteredGrades = editGradeAllowed ? gradeItems.filter((g) => editGradeAllowed.has(g.code)) : gradeItems;

  const editComputedAmount = (() => {
    if (!editDraft) return '';
    const p = parseFloat(editDraft.price);
    const v = parseFloat(editDraft.volume);
    if (Number.isNaN(p) || Number.isNaN(v)) return '';
    return (Math.round(p * v * 100) / 100).toFixed(2);
  })();

  const columns: ResultsTableColumn<EditableLineItemRow>[] = [
    {
      key: 'secondarySort',
      header: 'Secondary sort code',
      headerAlign: 'center',
      renderCell: (r) => {
        if (!isEditingId(r.id) || !editDraft) return r.secondarySort;
        return (
          <SingleSelect
            id={`edit-${r.id}-secondary-sort`}
            titleText=""
            hideLabel
            items={sortCodeItems}
            itemToString={(item) => (item ? `${item.code} - ${item.description}` : '')}
            selectedItem={findByCode(sortCodeItems, editDraft.secondSort)}
            onChange={({ selectedItem }) => onDraftChange({ ...editDraft, secondSort: selectedItem?.code ?? '' })}
            size="sm"
            invalid={!!fieldErrors.secondarySort}
            invalidText={fieldErrors.secondarySort}
          />
        );
      },
    },
    {
      key: 'species',
      header: 'Species',
      headerAlign: 'center',
      renderCell: (r) => {
        if (!isEditingId(r.id) || !editDraft) return r.species;
        return (
          <SingleSelect
            id={`edit-${r.id}-species`}
            titleText=""
            hideLabel
            items={editFilteredSpecies}
            itemToString={(item) => item?.code ?? ''}
            selectedItem={findByCode(speciesItems, editDraft.species)}
            onChange={({ selectedItem }) => onDraftChange({ ...editDraft, species: selectedItem?.code ?? '' })}
            size="sm"
            invalid={!!fieldErrors.species}
            invalidText={fieldErrors.species}
          />
        );
      },
    },
    {
      key: 'clientSecondarySort',
      header: 'Client secondary sort code',
      headerAlign: 'center',
      renderCell: (r) => {
        if (!isEditingId(r.id) || !editDraft) return r.clientSecondarySort;
        return (
          <TextInput
            id={`edit-${r.id}-client-sort`}
            labelText=""
            hideLabel
            value={editDraft.clientSecondarySort}
            onChange={(e) => onDraftChange({ ...editDraft, clientSecondarySort: e.target.value })}
            size="sm"
          />
        );
      },
    },
    {
      key: 'numberPieces',
      header: 'Number pieces',
      headerAlign: 'center',
      renderCell: (r) => {
        if (!isEditingId(r.id) || !editDraft) return formatNumber(r.numberPieces);
        return (
          <TextInput
            id={`edit-${r.id}-pieces`}
            labelText=""
            hideLabel
            value={editDraft.numOfPieces}
            onChange={(e) => onDraftChange({ ...editDraft, numOfPieces: e.target.value })}
            size="sm"
            invalid={!!fieldErrors.pieces}
            invalidText={fieldErrors.pieces}
          />
        );
      },
    },
    {
      key: 'grade',
      header: 'Grade',
      headerAlign: 'center',
      renderCell: (r) => {
        if (!isEditingId(r.id) || !editDraft) return r.grade;
        return (
          <SingleSelect
            id={`edit-${r.id}-grade`}
            titleText=""
            hideLabel
            items={editFilteredGrades}
            itemToString={(item) => item?.code ?? ''}
            selectedItem={findByCode(gradeItems, editDraft.grade)}
            onChange={({ selectedItem }) => onDraftChange({ ...editDraft, grade: selectedItem?.code ?? '' })}
            size="sm"
            invalid={!!fieldErrors.grade}
            invalidText={fieldErrors.grade}
          />
        );
      },
    },
    {
      key: 'volume',
      header: 'Volume',
      headerAlign: 'center',
      renderCell: (r) => {
        if (!isEditingId(r.id) || !editDraft) return formatNumber(r.volume, 3);
        return (
          <TextInput
            id={`edit-${r.id}-volume`}
            labelText=""
            hideLabel
            value={editDraft.volume}
            onChange={(e) => onDraftChange({ ...editDraft, volume: e.target.value })}
            size="sm"
            invalid={!!fieldErrors.volume}
            invalidText={fieldErrors.volume}
          />
        );
      },
    },
    {
      key: 'price',
      header: '$ Price',
      headerAlign: 'center',
      renderCell: (r) => {
        if (!isEditingId(r.id) || !editDraft) return formatCurrency(r.price);
        return (
          <TextInput
            id={`edit-${r.id}-price`}
            labelText=""
            hideLabel
            value={editDraft.price}
            onChange={(e) => onDraftChange({ ...editDraft, price: e.target.value })}
            size="sm"
            invalid={!!fieldErrors.price}
            invalidText={fieldErrors.price}
          />
        );
      },
    },
    {
      key: 'amount',
      header: '$ Amount',
      headerAlign: 'center',
      renderCell: (r) => {
        if (isEditingId(r.id)) {
          return editComputedAmount ? `$${editComputedAmount}` : '';
        }
        return formatCurrency(r.amount);
      },
    },
    {
      key: 'id',
      header: '',
      renderCell: (r) => {
        if (isEditingId(r.id)) {
          return (
            <div className="editable-line-items-table__actions">
              <IconButton kind="ghost" size="sm" label="Save line item" align="top" autoAlign onClick={onSaveEdit}>
                <Checkmark />
              </IconButton>
              <IconButton kind="ghost" size="sm" label="Cancel" align="top" autoAlign onClick={onCancelEdit}>
                <Close />
              </IconButton>
            </div>
          );
        }
        return (
          <div className="editable-line-items-table__actions">
            {canEdit ? (
              <IconButton
                kind="ghost"
                size="sm"
                label="Edit line item"
                align="top"
                autoAlign
                disabled={editDraft !== null}
                onClick={() => onStartEdit(r.id)}
              >
                <Edit />
              </IconButton>
            ) : null}
            {canDelete ? (
              <IconButton
                kind="ghost"
                size="sm"
                label="Delete line item"
                align="top"
                autoAlign
                disabled={editDraft !== null}
                onClick={() => onDeleteItem(r.id)}
              >
                <TrashCan />
              </IconButton>
            ) : null}
          </div>
        );
      },
    },
  ];

  return (
    <div className="editable-line-items-table">
      <h4 className="editable-line-items-table__heading">Details</h4>
      <ResultsTable rows={rows} columns={columns} size="sm" withZebraStyles={false} />
    </div>
  );
}
