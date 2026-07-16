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
import { Add, Edit, Search as SearchIcon, TrashCan } from '@carbon/icons-react';

import { useNotification } from '@/context/notification/useNotification';
import {
  useSearchFlatPriceConversionsQuery,
  useExportFlatPriceConversionsMutation,
  extractApiErrorMessage,
  type FlatPriceConversionResponse,
  type SearchFlatPriceConversionParams,
} from '@/services/flatPriceConversion.service';
import { downloadBlob } from '@/utils/report';
import { formatDisplayDate } from '@/utils/format';
import {
  useMaturityCodesQuery,
  useSortCodesLookupQuery,
  useSpeciesLookupQuery,
  useGradeLookupQuery,
  useGradesBySpeciesLookupQuery,
  type LookupItemResponse,
} from '@/services/lookup.service';
import ResultsTable, { type ResultsTableColumn } from '@/components/Form/ResultsTable';
import FormModal from '@/components/core/FormModal';
import SearchableSelect from '@/components/Form/SearchableSelect';
import { usePermission } from '@/context/auth/usePermission';
import {
  PROD_FLAT_PRICE_CONV_DELETE,
  PROD_FLAT_PRICE_CONV_EDIT,
  PROD_FLAT_PRICE_CONV_ADD_NEW_ROW,
} from '@/context/auth/permissions';
import { useFlatPriceConversionForm } from './useFlatPriceConversionForm';
import { useFlatPriceConversionDelete } from './useFlatPriceConversionDelete';
import './index.scss';

type ModalState =
  | { kind: 'closed' }
  | { kind: 'add' }
  | { kind: 'edit'; row: FlatPriceConversionResponse }
  | { kind: 'delete'; row: FlatPriceConversionResponse };

type FlatPriceConversionTableRow = Omit<FlatPriceConversionResponse, 'id'> & { id: string; numericId: number };

function FlatPriceConversionFormFields({
  form,
  mode,
  maturityItems,
  speciesItems,
  gradeOptions,
  sortCodeOptions,
}: {
  form: ReturnType<typeof useFlatPriceConversionForm>;
  mode: 'add' | 'edit';
  maturityItems: LookupItemResponse[];
  speciesItems: LookupItemResponse[];
  gradeOptions: string[];
  sortCodeOptions: string[];
}) {
  const { values, errors, set, setValue, apiErrorMessage } = form;

  const maturityDescriptions = maturityItems.map((m) => m.description);
  const maturityByDescription = Object.fromEntries(maturityItems.map((m) => [m.description, m.code]));
  const selectedMaturityDescription = maturityItems.find((m) => m.code === values.maturity)?.description ?? null;

  const speciesDescriptions = speciesItems.map((s) => s.description);
  const speciesByDescription = Object.fromEntries(speciesItems.map((s) => [s.description, s.code]));
  const selectedSpeciesDescription = speciesItems.find((s) => s.code === values.species)?.description ?? null;

  return (
    <Stack gap={5}>
      {apiErrorMessage && <InlineNotification kind="error" title="Error" subtitle={apiErrorMessage} hideCloseButton />}
      <SearchableSelect
        id={`${mode}-species`}
        titleText="Species *"
        label=""
        items={speciesDescriptions}
        selectedItem={selectedSpeciesDescription}
        onChange={({ selectedItem }) => {
          setValue('species', selectedItem ? (speciesByDescription[selectedItem] ?? '') : '');
          setValue('grade', '');
        }}
        invalid={!!errors.species}
        invalidText={errors.species}
      />
      <SearchableSelect
        id={`${mode}-sort-code`}
        titleText="Sort *"
        label=""
        items={sortCodeOptions}
        selectedItem={values.sortCode || null}
        onChange={({ selectedItem }) => setValue('sortCode', selectedItem ?? '')}
        invalid={!!errors.sortCode}
        invalidText={errors.sortCode}
      />
      <SearchableSelect
        id={`${mode}-grade`}
        titleText="Grade *"
        label=""
        items={gradeOptions}
        selectedItem={values.grade || null}
        onChange={({ selectedItem }) => setValue('grade', selectedItem ?? '')}
        disabled={!values.species}
        invalid={!!errors.grade}
        invalidText={errors.grade}
      />
      <SearchableSelect
        id={`${mode}-maturity`}
        titleText="Maturity *"
        label=""
        items={maturityDescriptions}
        selectedItem={selectedMaturityDescription}
        onChange={({ selectedItem }) =>
          setValue('maturity', selectedItem ? (maturityByDescription[selectedItem] ?? '') : '')
        }
        invalid={!!errors.maturity}
        invalidText={errors.maturity}
      />
      <TextInput
        id={`${mode}-flat-price-conversion`}
        labelText="Flat price conversion *"
        type="number"
        min={1}
        max={999}
        value={values.flatPriceConversion}
        onChange={set('flatPriceConversion')}
        invalid={!!errors.flatPriceConversion}
        invalidText={errors.flatPriceConversion}
      />
      <DatePicker
        datePickerType="single"
        dateFormat="Y-m-d"
        value={values.effectiveDate}
        invalid={!!errors.effectiveDate}
        onChange={(_dates, dateStr) => setValue('effectiveDate', dateStr)}
      >
        <DatePickerInput
          id={`${mode}-effective-date`}
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
          id={`${mode}-expiry-date`}
          labelText="Expiry date"
          placeholder="yyyy-mm-dd"
          invalid={!!errors.expiryDate}
          invalidText={errors.expiryDate}
          onChange={set('expiryDate')}
        />
      </DatePicker>
    </Stack>
  );
}

export function FlatPriceConversionPage() {
  const modellingCode = 'P';
  const NS = 'csp.table.flatPriceConversion.v1';
  const [filterMaturity, setFilterMaturity] = usePersistentState<string | null>(NS, 'filterMaturity', null);
  const [filterSpecies, setFilterSpecies] = usePersistentState<string | null>(NS, 'filterSpecies', null);
  const [filterGrade, setFilterGrade] = usePersistentState<string | null>(NS, 'filterGrade', null);
  const [filterSortCode, setFilterSortCode] = usePersistentState<string | null>(NS, 'filterSortCode', null);
  const [hasSearched, setHasSearched] = usePersistentState(NS, 'hasSearched', true);
  const [page, setPage] = usePersistentState(NS, 'page', 1);
  const [pageSize, setPageSize] = usePersistentState(NS, 'pageSize', 20);
  const [modal, setModal] = useState<ModalState>({ kind: 'closed' });
  const [searchParams, setSearchParams] = usePersistentState<SearchFlatPriceConversionParams>(NS, 'searchParams', {
    modellingCode: 'P',
  });

  const { addNotification } = useNotification();
  const canEdit = usePermission(PROD_FLAT_PRICE_CONV_EDIT);
  const canDelete = usePermission(PROD_FLAT_PRICE_CONV_DELETE);
  const canAdd = usePermission(PROD_FLAT_PRICE_CONV_ADD_NEW_ROW);

  const { data, isLoading, isError, error } = useSearchFlatPriceConversionsQuery(searchParams);
  const exportMutation = useExportFlatPriceConversionsMutation();

  const handleExport = useCallback(
    (format: 'pdf' | 'csv') => {
      exportMutation.mutate(
        { format, params: searchParams },
        {
          onSuccess: ({ blob, filename }) => downloadBlob(blob, filename),
          onError: () =>
            addNotification({
              kind: 'error',
              title: `Failed to export flat price conversions as ${format.toUpperCase()}.`,
            }),
        },
      );
    },
    [exportMutation, addNotification, searchParams],
  );

  const maturityQuery = useMaturityCodesQuery();
  const speciesQuery = useSpeciesLookupQuery();
  const gradeQuery = useGradeLookupQuery();
  const sortCodeQuery = useSortCodesLookupQuery();

  const gradeFilterOptions = (gradeQuery.data ?? []).map((g) => g.code);
  const sortCodeOptions = (sortCodeQuery.data ?? []).map((s) => s.code);

  const maturityDescriptions = Object.fromEntries((maturityQuery.data ?? []).map((m) => [m.code, m.description]));
  const speciesDescriptions = Object.fromEntries((speciesQuery.data ?? []).map((s) => [s.code, s.description]));
  const sortCodeDescriptions = Object.fromEntries((sortCodeQuery.data ?? []).map((s) => [s.code, s.description]));

  const maturityFilterItems = (maturityQuery.data ?? []).map((m) => m.description);
  const maturityCodeByDescription = Object.fromEntries((maturityQuery.data ?? []).map((m) => [m.description, m.code]));

  const speciesFilterItems = (speciesQuery.data ?? []).map((s) => s.description);
  const speciesCodeByDescription = Object.fromEntries((speciesQuery.data ?? []).map((s) => [s.description, s.code]));

  const editRow = modal.kind === 'edit' ? modal.row : undefined;

  const handleAddSuccess = useCallback(
    (_row: FlatPriceConversionResponse) => {
      setModal({ kind: 'closed' });
      addNotification({ kind: 'success', title: 'Row added successfully.' });
    },
    [addNotification],
  );

  const handleEditSuccess = useCallback(
    (_row: FlatPriceConversionResponse) => {
      setModal({ kind: 'closed' });
      addNotification({ kind: 'success', title: 'Row updated successfully.' });
    },
    [addNotification],
  );

  const handleDeleteSuccess = useCallback(
    (_id: number) => {
      setModal({ kind: 'closed' });
      addNotification({ kind: 'success', title: 'Row deleted.' });
    },
    [addNotification],
  );

  const addForm = useFlatPriceConversionForm(modal.kind === 'add', 'add', modellingCode, undefined, handleAddSuccess);
  const editForm = useFlatPriceConversionForm(modal.kind === 'edit', 'edit', modellingCode, editRow, handleEditSuccess);

  const addGradeQuery = useGradesBySpeciesLookupQuery(modal.kind === 'add' ? addForm.values.species || null : null);
  const editGradeQuery = useGradesBySpeciesLookupQuery(modal.kind === 'edit' ? editForm.values.species || null : null);

  const deleteHook = useFlatPriceConversionDelete(modal.kind === 'delete', handleDeleteSuccess);

  const handleSearch = () => {
    setSearchParams({
      modellingCode,
      maturity: filterMaturity,
      species: filterSpecies,
      grade: filterGrade,
      sortCode: filterSortCode,
    });
    setHasSearched(true);
    setPage(1);
  };

  const handleClearFilters = () => {
    setFilterMaturity(null);
    setFilterSpecies(null);
    setFilterGrade(null);
    setFilterSortCode(null);
    setHasSearched(true);
    setPage(1);
    setSearchParams({ modellingCode });
  };

  const handleDeleteSubmit = useCallback(() => {
    if (modal.kind === 'delete') deleteHook.handleConfirm(modal.row.id);
  }, [modal, deleteHook]);

  const rows: FlatPriceConversionTableRow[] = (data ?? []).map((r) => ({ ...r, id: String(r.id), numericId: r.id }));

  const columns: ResultsTableColumn<FlatPriceConversionTableRow>[] = [
    { key: 'maturity', header: 'Maturity', renderCell: (row) => maturityDescriptions[row.maturity] ?? row.maturity },
    { key: 'species', header: 'Species', renderCell: (row) => speciesDescriptions[row.species] ?? row.species },
    { key: 'sortCode', header: 'Sort code', renderCell: (row) => sortCodeDescriptions[row.sortCode] ?? row.sortCode },
    { key: 'grade', header: 'Grade' },
    { key: 'flatPriceConversion', header: 'Flat price conversion', headerAlign: 'center', cellAlign: 'center' },
    { key: 'effectiveDate', header: 'Effective date', renderCell: (row) => formatDisplayDate(row.effectiveDate) },
    {
      key: 'expiryDate',
      header: 'Expiry date',
      renderCell: (row) => (row.expiryDate ? formatDisplayDate(row.expiryDate) : '-'),
    },
    {
      key: 'id',
      header: 'Actions',
      sortable: false,
      renderCell: (row) => {
        const response: FlatPriceConversionResponse = { ...row, id: row.numericId };
        return (
          <div className="flat-price-conversion-page__row-actions">
            {canEdit && (
              <Button
                kind="ghost"
                hasIconOnly
                renderIcon={Edit}
                iconDescription="Edit"
                size="sm"
                onClick={() => setModal({ kind: 'edit', row: response })}
              />
            )}
            {canDelete && (
              <Button
                kind="ghost"
                hasIconOnly
                renderIcon={TrashCan}
                iconDescription="Delete"
                size="sm"
                onClick={() => setModal({ kind: 'delete', row: response })}
              />
            )}
          </div>
        );
      },
    },
  ];

  const renderTable = () => {
    if (isError) {
      return (
        <InlineNotification
          kind="error"
          title="Failed to load flat price conversions."
          subtitle={error ? extractApiErrorMessage(error) : 'Please try again later.'}
          hideCloseButton
        />
      );
    }
    return (
      <ResultsTable
        rows={rows}
        columns={columns}
        isSortable
        hasSearched={hasSearched}
        isLoading={isLoading}
        page={page}
        pageSize={pageSize}
        totalItems={rows.length}
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
    );
  };

  return (
    <div className="flat-price-conversion-page">
      <Grid>
        <Column sm={4} md={8} lg={16} className="flat-price-conversion-page__title-col">
          <h1 className="flat-price-conversion-page__title">Flat price conversion</h1>
        </Column>

        <Column
          sm={4}
          md={2}
          lg={3}
          className="flat-price-conversion-page__filter-col flat-price-conversion-page__filter-col--first"
        >
          <SearchableSelect
            id="maturity-filter"
            titleText="Maturity"
            label=""
            items={maturityFilterItems}
            selectedItem={filterMaturity ? (maturityDescriptions[filterMaturity] ?? null) : null}
            onChange={({ selectedItem }) =>
              setFilterMaturity(selectedItem ? (maturityCodeByDescription[selectedItem] ?? null) : null)
            }
          />
        </Column>
        <Column sm={4} md={2} lg={3} className="flat-price-conversion-page__filter-col">
          <SearchableSelect
            id="species-filter"
            titleText="Species"
            label=""
            items={speciesFilterItems}
            selectedItem={filterSpecies ? (speciesDescriptions[filterSpecies] ?? null) : null}
            onChange={({ selectedItem }) =>
              setFilterSpecies(selectedItem ? (speciesCodeByDescription[selectedItem] ?? null) : null)
            }
          />
        </Column>
        <Column sm={4} md={2} lg={3} className="flat-price-conversion-page__filter-col">
          <SearchableSelect
            id="sort-code-filter"
            titleText="Sort"
            label=""
            items={sortCodeOptions}
            selectedItem={filterSortCode}
            onChange={({ selectedItem }) => setFilterSortCode(selectedItem ?? null)}
          />
        </Column>
        <Column sm={4} md={2} lg={3} className="flat-price-conversion-page__filter-col">
          <SearchableSelect
            id="grade-filter"
            titleText="Grade"
            label=""
            items={gradeFilterOptions}
            selectedItem={filterGrade}
            onChange={({ selectedItem }) => setFilterGrade(selectedItem ?? null)}
          />
        </Column>
        <Column sm={4} md={8} lg={4} className="flat-price-conversion-page__search-col">
          <label className="flat-price-conversion-page__search-label-spacer" aria-hidden="true">
            &nbsp;
          </label>
          <Button kind="primary" size="md" renderIcon={SearchIcon} iconDescription="Search" onClick={handleSearch}>
            Search
          </Button>
        </Column>

        <Column sm={4} md={8} lg={16} className="flat-price-conversion-page__clear-col">
          <Button kind="ghost" size="sm" onClick={handleClearFilters}>
            Clear filters
          </Button>
        </Column>

        <Column sm={4} md={8} lg={16}>
          <TableToolbar>
            <TableToolbarContent>
              <MenuButton label="Export table" kind="ghost" className="flat-price-conversion-page__export-btn">
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
        title="Add new row"
        size="lg"
        submitDisabled={addForm.isPending}
        onSubmit={addForm.handleSubmit}
        onClose={() => setModal({ kind: 'closed' })}
      >
        <FlatPriceConversionFormFields
          form={addForm}
          mode="add"
          maturityItems={maturityQuery.data ?? []}
          speciesItems={speciesQuery.data ?? []}
          gradeOptions={addGradeQuery.data?.map((g) => g.code) ?? []}
          sortCodeOptions={sortCodeOptions}
        />
      </FormModal>

      <FormModal
        open={modal.kind === 'edit'}
        title="Edit row"
        size="lg"
        submitDisabled={editForm.isPending}
        onSubmit={editForm.handleSubmit}
        onClose={() => setModal({ kind: 'closed' })}
      >
        <FlatPriceConversionFormFields
          form={editForm}
          mode="edit"
          maturityItems={maturityQuery.data ?? []}
          speciesItems={speciesQuery.data ?? []}
          gradeOptions={editGradeQuery.data?.map((g) => g.code) ?? []}
          sortCodeOptions={sortCodeOptions}
        />
      </FormModal>

      <FormModal
        open={modal.kind === 'delete'}
        title="Delete row"
        danger
        submitLabel="Delete"
        submitDisabled={deleteHook.isPending}
        onSubmit={handleDeleteSubmit}
        onClose={() => setModal({ kind: 'closed' })}
      >
        <Stack gap={5}>
          {deleteHook.apiErrorMessage && (
            <InlineNotification kind="error" title="Error" subtitle={deleteHook.apiErrorMessage} hideCloseButton />
          )}
          <p>Are you sure you want to delete this row? This action cannot be undone.</p>
        </Stack>
      </FormModal>
    </div>
  );
}
