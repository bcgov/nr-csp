import {
  DataTable,
  DataTableSkeleton,
  Pagination,
  Search,
  Table,
  TableBody,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  TableHead,
  TableHeader,
  TableRow,
} from '@carbon/react';
import { Summit, UserSearch } from '@carbon/pictograms-react';
import React, { useState, useMemo, useEffect, type ReactElement, type ReactNode } from 'react';

import './index.scss';

/**
 * A column definition for ResultsTable.
 *
 * @template T The shape of a single row.
 * @property {keyof T & string} key - The row property used as both the column key and default cell value.
 * @property {string} header - The column header label.
 * @property {(row: T) => ReactNode} [renderCell] - Optional custom renderer that receives the full row; defaults to stringifying `row[key]`.
 */
export interface ResultsTableColumn<T> {
  key: keyof T & string;
  header: string;
  renderCell?: (row: T) => ReactNode;
  /** Optional custom header renderer, e.g. to wrap the label in the same
   * fixed-width box as the cells so the header lines up with the values. Takes
   * precedence over `headerAlign`. */
  renderHeader?: () => ReactNode;
  sortable?: boolean;
  headerAlign?: 'left' | 'center' | 'right';
  cellAlign?: 'left' | 'center' | 'right';
}

/**
 * Props for the ResultsTable component.
 *
 * @template T The shape of a single row; must include a string `id`.
 * @property {T[]} rows - The data rows to display.
 * @property {ResultsTableColumn<T>[]} columns - Column definitions including optional per-column renderers.
 * @property {boolean} [isSortable] - Whether columns are sortable. Defaults to false.
 * @property {boolean} [hasSearched] - When true and `rows` is empty, renders the uniform "No results found" state. When false and `rows` is empty, renders the uniform "No search performed" state.
 * @property {boolean} [isLoading] - When true, renders an animated skeleton table in place of results.
 * @property {string} [searchKeyword] - Current keyword filter value shown in the search bar above the table.
 * @property {(keyword: string) => void} [onSearchKeywordChange] - Called on every keystroke in the keyword search bar. Omit to hide the bar.
 * @property {number} [page] - Current page number. When provided alongside `pageSize` (and `serverSide` is false), sorting spans the
 *   full `rows` dataset — rows are sorted externally then paginated so ascending/descending order is
 *   consistent across all pages.
 * @property {number} [pageSize] - Number of rows per page. Required when `page` is provided.
 * @property {boolean} [serverSide] - When true, skip internal sort and pagination — parent supplies
 *   already-sorted, already-paginated rows. `onSortChange` is invoked with the new sort key + direction
 *   so the parent can refetch.
 * @property {(sortKey: string | null, sortDir: 'ASC' | 'DESC' | 'NONE') => void} [onSortChange] - Called when the sort column or direction changes. Use this to
 *   reset the current page to 1 so the user always sees the first page of the newly sorted results.
 */
interface ResultsTableProps<T extends { id: string }> {
  rows: T[];
  columns: ResultsTableColumn<T>[];
  isSortable?: boolean;
  hasSearched?: boolean;
  isLoading?: boolean;
  searchKeyword?: string;
  onSearchKeywordChange?: (keyword: string) => void;
  page?: number;
  pageSize?: number;
  serverSide?: boolean;
  onSortChange?: (sortKey: string | null, sortDir: 'ASC' | 'DESC' | 'NONE') => void;
  // When `onPaginationChange` is provided, a Carbon Pagination control is rendered at the
  // bottom of the table using `totalItems`, `pageSize`, `page`, and `pageSizes`. Pages that
  // need fully custom pagination can omit this prop and render Pagination themselves.
  totalItems?: number;
  pageSizes?: number[];
  onPaginationChange?: (data: { page: number; pageSize: number }) => void;
  paginationItemsPerPageText?: string;
  paginationItemRangeText?: (min: number, max: number, total: number) => string;
  paginationPageRangeText?: (current: number, total: number) => string;
  // When `expandable` is true, each row renders with a chevron and `renderExpandedContent(row)`
  // is rendered in a TableExpandedRow beneath it when the row is open.
  expandable?: boolean;
  renderExpandedContent?: (row: T) => ReactNode;
  // Optional controlled expansion.
  expandedRowIds?: Set<string>;
  onExpandedRowIdsChange?: (next: Set<string>) => void;
  // Optional table size — defaults to 'lg' to preserve existing usages.
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  // Optional footer row(s) rendered inside the table body after the data rows. The parent
  // owns the `<tr>` markup — useful for totals rows or other summaries that must align
  // with the column widths above.
  footerRow?: ReactNode;
  // Disable the zebra striping (defaults on).
  withZebraStyles?: boolean;
  // Override the built-in empty-state copy. Each falls back to the
  // search-driven defaults ("No results found" / "No search performed").
  emptyTitle?: string;
  emptyDescription?: string;
}

/**
 * ResultsTable wraps Carbon's DataTable with a built-in empty state and loading skeleton. Each
 * column may declare a custom `renderCell` for tags, links, badges, etc.
 *
 * When `page` and `pageSize` are provided, the component sorts the full `rows` dataset internally
 * then paginates — so clicking a column header always sorts ALL rows across ALL pages, not just the
 * visible page. Carbon's sort icons are driven by internal state; `onSortChange` lets the parent
 * reset to page 1 whenever the sort changes.
 *
 * @template T The shape of a single row; must include a string `id`.
 * @param {ResultsTableProps<T>} props - The component props.
 * @returns {ReactElement} The rendered skeleton, table, or empty state.
 */
const ResultsTable = <T extends { id: string }>({
  rows,
  columns,
  isSortable = false,
  hasSearched = false,
  isLoading = false,
  searchKeyword,
  onSearchKeywordChange,
  page,
  pageSize,
  serverSide = false,
  onSortChange,
  totalItems,
  pageSizes,
  onPaginationChange,
  paginationItemsPerPageText = 'Items per page:',
  paginationItemRangeText = (min, max, total) => `${min} – ${max} of ${total} items`,
  paginationPageRangeText = (_current, total) => `of ${Math.max(total, 1)} pages`,
  expandable = false,
  renderExpandedContent,
  expandedRowIds,
  onExpandedRowIdsChange,
  size,
  footerRow,
  withZebraStyles = true,
  emptyTitle,
  emptyDescription,
}: ResultsTableProps<T>): ReactElement => {
  const headers = columns.map((col) => ({ key: col.key, header: col.header }));
  const sortableKeys = new Set(columns.filter((col) => col.sortable !== false).map((col) => col.key));

  const [inputValue, setInputValue] = useState(searchKeyword ?? '');

  // Sort state drives both the column-header icons and (when client-side) row ordering.
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDir, setSortDir] = useState<'ASC' | 'DESC' | 'NONE'>('NONE');

  // Expansion is tracked by row id (rather than left to Carbon's internal
  // DataTable state) so a row stays open across `rows` prop changes.
  const [internalExpandedIds, setInternalExpandedIds] = useState<Set<string>>(new Set());
  const expandedIds = expandedRowIds ?? internalExpandedIds;
  const commitExpanded = (next: Set<string>) => {
    onExpandedRowIdsChange?.(next);
    if (expandedRowIds === undefined) setInternalExpandedIds(next);
  };
  const toggleExpanded = (id: string) => {
    const next = new Set(expandedIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    commitExpanded(next);
  };

  // In server-side mode the parent has already sorted; skip internal sort.
  const sortedRows = useMemo(() => {
    if (serverSide || !isSortable || !sortKey || sortDir === 'NONE') return rows;
    return [...rows].sort((a, b) => {
      const aVal = String((a as Record<string, unknown>)[sortKey] ?? '');
      const bVal = String((b as Record<string, unknown>)[sortKey] ?? '');
      return sortDir === 'ASC'
        ? aVal.localeCompare(bVal, undefined, { numeric: true, sensitivity: 'base' })
        : bVal.localeCompare(aVal, undefined, { numeric: true, sensitivity: 'base' });
    });
  }, [rows, sortKey, sortDir, isSortable, serverSide]);

  // In server-side mode the parent has already sliced to the current page; render rows as-is.
  const pageRows =
    !serverSide && page !== undefined && pageSize
      ? sortedRows.slice((page - 1) * pageSize, page * pageSize)
      : sortedRows;

  // Guard against a controlled `page` that exceeds the available pages — e.g. a
  // persisted/restored page whose underlying data has since shrunk. Without this
  // the table would render an empty page even though valid rows exist on earlier
  // pages. Only acts when pagination is controlled and data has finished loading;
  // it notifies the parent (which owns `page`) to snap back to the last real page.
  useEffect(() => {
    if (!onPaginationChange || isLoading) return;
    if (page === undefined || !pageSize) return;
    const total = totalItems ?? 0;
    if (total <= 0) return; // no data (or unknown) — leave the empty-state alone
    const lastPage = Math.max(1, Math.ceil(total / pageSize));
    if (page > lastPage) {
      onPaginationChange({ page: lastPage, pageSize });
    }
  }, [page, pageSize, totalItems, isLoading, onPaginationChange]);

  // Drives the header "expand all" chevron and toggle.
  const allExpanded = expandable && pageRows.length > 0 && pageRows.every((r) => expandedIds.has(r.id));

  const handleHeaderClick = (headerKey: string) => {
    const currentDir = sortKey === headerKey ? sortDir : 'NONE';
    const newDir: 'ASC' | 'DESC' | 'NONE' = currentDir === 'NONE' ? 'ASC' : currentDir === 'ASC' ? 'DESC' : 'NONE';
    const newKey = newDir === 'NONE' ? null : headerKey;
    setSortKey(newKey);
    setSortDir(newDir);
    onSortChange?.(newKey, newDir);
  };

  const paginationBar = onPaginationChange ? (
    <Pagination
      totalItems={totalItems ?? 0}
      pageSize={pageSize ?? 20}
      pageSizes={pageSizes ?? [20, 40, 60, 80, 100]}
      page={page ?? 1}
      onChange={onPaginationChange}
      itemsPerPageText={paginationItemsPerPageText}
      itemRangeText={paginationItemRangeText}
      pageRangeText={paginationPageRangeText}
    />
  ) : null;

  const keywordBar = onSearchKeywordChange ? (
    <Search
      id="results-table-keyword-search"
      labelText="Search by keyword"
      placeholder="Search by keyword"
      value={inputValue}
      onChange={(e) => setInputValue(e.target.value)}
      onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') onSearchKeywordChange(inputValue);
      }}
      onClear={() => {
        setInputValue('');
        onSearchKeywordChange('');
      }}
      size="md"
    />
  ) : null;

  if (isLoading) {
    return (
      <div className={`results-table${expandable ? ' results-table--expandable' : ''}`}>
        {keywordBar}
        <DataTableSkeleton
          headers={headers}
          rowCount={10}
          columnCount={columns.length}
          showHeader={false}
          showToolbar={false}
          zebra
        />
        {paginationBar}
      </div>
    );
  }

  // Empty-state copy + pictogram. Consumers can override the copy via the
  // `emptyTitle` / `emptyDescription` props; otherwise it falls back to the
  // search-driven defaults.
  const resolvedEmptyTitle = emptyTitle ?? (hasSearched ? 'No results found' : 'No search performed');
  const resolvedEmptyDescription =
    emptyDescription ??
    (hasSearched ? 'Try adjusting your search criteria.' : 'Use the filters above and click Search to see results.');
  const Pictogram = hasSearched ? UserSearch : Summit;

  return (
    <div className={`results-table${expandable ? ' results-table--expandable' : ''}`}>
      {keywordBar}
      <DataTable rows={pageRows} headers={headers} isSortable={isSortable}>
        {({
          rows: tableRows,
          headers: tableHeaders,
          getTableProps,
          getHeaderProps,
          getRowProps,
          getExpandHeaderProps,
        }) => (
          <Table {...getTableProps()} size={size} useZebraStyles={withZebraStyles}>
            <TableHead>
              <TableRow>
                {expandable ? (
                  <TableExpandHeader
                    {...(getExpandHeaderProps ? getExpandHeaderProps() : {})}
                    isExpanded={allExpanded}
                    onExpand={() => commitExpanded(allExpanded ? new Set() : new Set(pageRows.map((r) => r.id)))}
                  />
                ) : null}
                {tableHeaders.map((header) => {
                  const colDef = columns.find((c) => c.key === header.key);
                  let headerContent: ReactNode;
                  if (colDef?.renderHeader) {
                    headerContent = colDef.renderHeader();
                  } else if (colDef?.headerAlign) {
                    headerContent = (
                      <span style={{ display: 'block', textAlign: colDef.headerAlign }}>{header.header}</span>
                    );
                  } else {
                    headerContent = header.header;
                  }
                  return (
                    <TableHeader
                      key={header.key}
                      {...getHeaderProps({ header, isSortable: isSortable && sortableKeys.has(header.key) })}
                      isSortHeader={isSortable && sortableKeys.has(header.key) && header.key === sortKey}
                      sortDirection={
                        isSortable && sortableKeys.has(header.key)
                          ? header.key === sortKey
                            ? sortDir
                            : 'NONE'
                          : undefined
                      }
                      onClick={
                        isSortable && sortableKeys.has(header.key) ? () => handleHeaderClick(header.key) : undefined
                      }
                    >
                      {headerContent}
                    </TableHeader>
                  );
                })}
              </TableRow>
            </TableHead>
            <TableBody>
              {tableRows.length === 0 ? (
                <tr className="results-table__empty-row">
                  <td colSpan={tableHeaders.length + (expandable ? 1 : 0)}>
                    <div className="results-table__empty-state">
                      <Pictogram className="results-table__empty-pictogram" />
                      <p className="results-table__empty-title">{resolvedEmptyTitle}</p>
                      <p className="results-table__empty-description">{resolvedEmptyDescription}</p>
                    </div>
                  </td>
                </tr>
              ) : (
                tableRows.map((tableRow) => {
                  const dataRow = rows.find((r) => r.id === tableRow.id);
                  if (!dataRow) return null;
                  const cells = tableRow.cells.map((cell) => {
                    const column = columns.find((c) => c.key === cell.info.header);
                    return (
                      <TableCell key={cell.id} style={column?.cellAlign ? { textAlign: column.cellAlign } : undefined}>
                        {column?.renderCell ? column.renderCell(dataRow) : String(cell.value)}
                      </TableCell>
                    );
                  });
                  if (expandable) {
                    return (
                      <React.Fragment key={tableRow.id}>
                        <TableExpandRow
                          {...getRowProps({ row: tableRow })}
                          isExpanded={expandedIds.has(tableRow.id)}
                          onExpand={() => toggleExpanded(tableRow.id)}
                        >
                          {cells}
                        </TableExpandRow>
                        {renderExpandedContent ? (
                          <TableExpandedRow colSpan={tableHeaders.length + 1}>
                            {renderExpandedContent(dataRow)}
                          </TableExpandedRow>
                        ) : null}
                      </React.Fragment>
                    );
                  }
                  return (
                    <TableRow {...getRowProps({ row: tableRow })} key={tableRow.id}>
                      {cells}
                    </TableRow>
                  );
                })
              )}
              {footerRow}
            </TableBody>
          </Table>
        )}
      </DataTable>
      {paginationBar}
    </div>
  );
};

export default ResultsTable;
