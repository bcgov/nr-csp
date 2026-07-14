import { useState, useCallback } from 'react';
import { usePersistentState } from '@/hooks/usePersistentState';
import {
  Button,
  Column,
  DatePicker,
  DatePickerInput,
  Grid,
  InlineNotification,
  MenuButton,
  MenuItem,
  Stack,
  TableToolbar,
  TableToolbarContent,
  TextInput,
} from '@carbon/react';
import { Add, Edit, TrashCan } from '@carbon/icons-react';

import { useNotification } from '@/context/notification/useNotification';
import { usePermission } from '@/context/auth/usePermission';
import { PROD_SORT_CODE_ADD_NEW_ROW, PROD_SORT_CODE_DELETE } from '@/context/auth/permissions';
import {
  useListSortCodesQuery,
  useExportSortCodesMutation,
  type SortCodeResponse,
  extractApiErrorMessage,
} from '@/services/sortcode.service';
import { downloadBlob } from '@/utils/report';
import { formatDisplayDate } from '@/utils/format';
import ResultsTable, { type ResultsTableColumn } from '../../components/Form/ResultsTable';
import FormModal from '@/components/core/FormModal';
import { useSortCodeForm } from './useSortCodeForm';
import { useSortCodeDelete } from './useSortCodeDelete';
import './index.scss';

type ModalState =
  | { kind: 'closed' }
  | { kind: 'add' }
  | { kind: 'edit'; row: SortCodeResponse }
  | { kind: 'delete'; row: SortCodeResponse };

type SortCodeRow = SortCodeResponse & { id: string };

function SortCodeFormFields({ form, mode }: { form: ReturnType<typeof useSortCodeForm>; mode: 'add' | 'edit' }) {
  const { values, errors, set, setValue, apiErrorMessage } = form;
  return (
    <Stack gap={5}>
      {apiErrorMessage && <InlineNotification kind="error" title="Error" subtitle={apiErrorMessage} hideCloseButton />}
      <TextInput
        id={`${mode}-sort-code-input`}
        labelText="Sort code *"
        value={values.sortCode}
        onChange={set('sortCode')}
        disabled={mode === 'edit'}
        maxLength={1}
        invalid={!!errors.sortCode}
        invalidText={errors.sortCode}
      />
      <TextInput
        id={`${mode}-description-input`}
        labelText="Description *"
        value={values.description}
        onChange={set('description')}
        maxLength={120}
        invalid={!!errors.description}
        invalidText={errors.description}
      />
      <DatePicker
        datePickerType="single"
        dateFormat="Y-m-d"
        value={values.effectiveDate}
        invalid={!!errors.effectiveDate}
        onChange={(_dates, dateStr) => setValue('effectiveDate', dateStr)}
      >
        <DatePickerInput
          id={`${mode}-effective-date-input`}
          labelText="Effective date *"
          placeholder="yyyy-mm-dd"
          invalid={!!errors.effectiveDate}
          invalidText={errors.effectiveDate}
          onChange={set('effectiveDate')}
        />
      </DatePicker>
      <DatePicker
        datePickerType="single"
        dateFormat="Y-m-d"
        value={values.expiryDate}
        invalid={!!errors.expiryDate}
        onChange={(_dates, dateStr) => setValue('expiryDate', dateStr)}
      >
        <DatePickerInput
          id={`${mode}-expiry-date-input`}
          labelText="Expiry date *"
          placeholder="yyyy-mm-dd"
          invalid={!!errors.expiryDate}
          invalidText={errors.expiryDate}
          onChange={set('expiryDate')}
        />
      </DatePicker>
    </Stack>
  );
}

export function SortCodePage() {
  const NS = 'csp.table.sortCode.v1';
  const [page, setPage] = usePersistentState(NS, 'page', 1);
  const [pageSize, setPageSize] = usePersistentState(NS, 'pageSize', 20);
  const [sortParam, setSortParam] = usePersistentState<string | undefined>(NS, 'sort', undefined);
  const [modal, setModal] = useState<ModalState>({ kind: 'closed' });

  const { addNotification } = useNotification();
  const canAdd = usePermission(PROD_SORT_CODE_ADD_NEW_ROW);
  const canEditOrDelete = usePermission(PROD_SORT_CODE_DELETE);
  const { data, isLoading, isError, error } = useListSortCodesQuery(page - 1, pageSize, sortParam);
  const exportMutation = useExportSortCodesMutation();

  const handleExport = useCallback(
    (format: 'pdf' | 'csv') => {
      exportMutation.mutate(format, {
        onSuccess: ({ blob, filename }) => downloadBlob(blob, filename),
        onError: () =>
          addNotification({ kind: 'error', title: `Failed to export sort codes as ${format.toUpperCase()}.` }),
      });
    },
    [exportMutation, addNotification],
  );

  const rows: SortCodeRow[] = (data?.content ?? []).map((r) => ({ ...r, id: r.sortCode }));

  const handleCreateSuccess = useCallback(
    (code: string) => {
      setModal({ kind: 'closed' });
      setPage(1);
      addNotification({ kind: 'success', title: `Sort code '${code}' created.` });
    },
    [addNotification],
  );

  const handleUpdateSuccess = useCallback(
    (code: string) => {
      setModal({ kind: 'closed' });
      setPage(1);
      addNotification({ kind: 'success', title: `Sort code '${code}' updated.` });
    },
    [addNotification],
  );

  const handleDeleteSuccess = useCallback(
    (code: string) => {
      setModal({ kind: 'closed' });
      setPage(1);
      addNotification({ kind: 'success', title: `Sort code '${code}' deleted.` });
    },
    [addNotification],
  );

  // All three hooks are always mounted; the `open` flag gates their side-effects.
  const addForm = useSortCodeForm(modal.kind === 'add', 'add', undefined, handleCreateSuccess);
  const editForm = useSortCodeForm(
    modal.kind === 'edit',
    'edit',
    modal.kind === 'edit' ? modal.row : undefined,
    handleUpdateSuccess,
  );
  const deleteConfirm = useSortCodeDelete(modal.kind === 'delete', handleDeleteSuccess);
  const { handleConfirm: confirmDelete } = deleteConfirm;

  const handleDeleteSubmit = useCallback(() => {
    if (modal.kind === 'delete') confirmDelete(modal.row.sortCode);
  }, [modal, confirmDelete]);

  const columns: ResultsTableColumn<SortCodeRow>[] = [
    { key: 'sortCode', header: 'Sort code' },
    { key: 'description', header: 'Description' },
    { key: 'effectiveDate', header: 'Effective date', renderCell: (row) => formatDisplayDate(row.effectiveDate) },
    {
      key: 'expiryDate',
      header: 'Expiry date',
      renderCell: (row) => (row.expiryDate ? formatDisplayDate(row.expiryDate) : '-'),
    },
    {
      key: 'updateTimestamp',
      header: 'Actions',
      sortable: false,
      renderCell: (row) => (
        <div className="table-maintenance-page__row-actions">
          {canEditOrDelete && (
            <Button
              kind="ghost"
              hasIconOnly
              renderIcon={Edit}
              iconDescription="Edit"
              size="sm"
              onClick={() => setModal({ kind: 'edit', row })}
            />
          )}
          {canEditOrDelete && (
            <Button
              kind="ghost"
              hasIconOnly
              renderIcon={TrashCan}
              iconDescription="Delete"
              size="sm"
              onClick={() => setModal({ kind: 'delete', row })}
            />
          )}
        </div>
      ),
    },
  ];

  const renderTable = () => {
    if (isError) {
      return (
        <InlineNotification
          kind="error"
          title="Failed to load sort codes."
          subtitle={error ? extractApiErrorMessage(error) : 'Please try again later.'}
          hideCloseButton
        />
      );
    }
    return (
      <>
        <ResultsTable
          rows={rows}
          columns={columns}
          isSortable
          serverSide
          hasSearched
          isLoading={isLoading}
          page={page}
          pageSize={pageSize}
          onSortChange={(sortKey, sortDir) => {
            setSortParam(sortKey && sortDir !== 'NONE' ? `${sortKey},${sortDir.toLowerCase()}` : undefined);
            setPage(1);
          }}
          totalItems={data?.totalElements ?? 0}
          paginationItemsPerPageText="Results per page:"
          paginationItemRangeText={(min, max, total) => `${min} – ${max} of ${total} results`}
          onPaginationChange={
            rows.length > 0
              ? ({ page: newPage, pageSize: newPageSize }) => {
                  setPage(newPage);
                  setPageSize(newPageSize);
                }
              : undefined
          }
        />
        {!isLoading && rows.length === 0 && <p className="table-maintenance-page__empty-state">No sort codes found.</p>}
      </>
    );
  };

  return (
    <div className="table-maintenance-page">
      <Grid>
        <Column sm={4} md={8} lg={16} className="table-maintenance-page__title-col">
          <h1 className="table-maintenance-page__title">Sort code</h1>
        </Column>

        <Column sm={4} md={8} lg={16}>
          <TableToolbar>
            <TableToolbarContent>
              <MenuButton label="Export table" kind="ghost" className="table-maintenance-page__export-btn">
                <MenuItem label="Export as CSV" onClick={() => handleExport('csv')} />
                <MenuItem label="Export as PDF" onClick={() => handleExport('pdf')} />
              </MenuButton>
              {canAdd && (
                <Button kind="primary" renderIcon={Add} onClick={() => setModal({ kind: 'add' })}>
                  Add new row
                </Button>
              )}
            </TableToolbarContent>
          </TableToolbar>

          {renderTable()}
        </Column>
      </Grid>

      <FormModal
        open={modal.kind === 'add'}
        title="Add new sort code"
        submitDisabled={addForm.isPending}
        onSubmit={addForm.handleSubmit}
        onClose={() => setModal({ kind: 'closed' })}
      >
        <SortCodeFormFields form={addForm} mode="add" />
      </FormModal>

      <FormModal
        open={modal.kind === 'edit'}
        title="Edit sort code"
        submitDisabled={editForm.isPending}
        onSubmit={editForm.handleSubmit}
        onClose={() => setModal({ kind: 'closed' })}
      >
        <SortCodeFormFields form={editForm} mode="edit" />
      </FormModal>

      <FormModal
        open={modal.kind === 'delete'}
        title="Delete sort code"
        danger
        submitLabel="Delete"
        submitDisabled={deleteConfirm.isPending}
        onSubmit={handleDeleteSubmit}
        onClose={() => setModal({ kind: 'closed' })}
      >
        <Stack gap={5}>
          {deleteConfirm.apiErrorMessage && (
            <InlineNotification kind="error" title="Error" subtitle={deleteConfirm.apiErrorMessage} hideCloseButton />
          )}
          <p>
            Are you sure you want to delete sort code{' '}
            <strong>'{modal.kind === 'delete' ? modal.row.sortCode : ''}'</strong>? This action cannot be undone.
          </p>
        </Stack>
      </FormModal>
    </div>
  );
}
