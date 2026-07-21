import { ErrorFilled, WarningAltFilled } from '@carbon/icons-react';
import {
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableExpandedRow,
  TableExpandHeader,
  TableExpandRow,
  TableHead,
  TableHeader,
  TableRow,
} from '@carbon/react';
import { Fragment, useState, type ReactElement, type ReactNode } from 'react';

import './index.scss';

/** A single validation issue attached to a cell or row. */
export interface CellIssue {
  message: string;
  type: 'ERROR' | 'WARNING';
}

/** Issues attached to a table row: per-column and row-level (no specific column). */
export interface RowIssues {
  /** Column key -> issues on that cell. */
  fields: Record<string, CellIssue[]>;
  /** Issues attached to the row but not a specific visible column. */
  row: CellIssue[];
}

/**
 * A column definition for {@link DataPreviewTable}.
 *
 * @template T The shape of a single row.
 * @property {keyof T & string} key - The row property used as the column key and default cell value.
 * @property {string} header - The column header label.
 * @property {(row: T) => ReactNode} [renderCell] - Optional custom cell renderer; defaults to stringifying `row[key]`.
 * @property {'left' | 'right'} [align] - Header/cell text alignment. Defaults to left.
 */
export interface DataPreviewColumn<T> {
  key: keyof T & string;
  header: string;
  renderCell?: (row: T) => ReactNode;
  align?: 'left' | 'right';
}

/**
 * Props for the DataPreviewTable component.
 *
 * @template T The shape of a single row; must include a string `id`.
 */
interface DataPreviewTableProps<T extends { id: string }> {
  rows: T[];
  columns: DataPreviewColumn<T>[];
  /** Centered italic message shown across all columns when there are no rows. */
  emptyMessage: string;
  /** Validation issues keyed by row `id`. Highlights affected rows and cells. */
  issuesByRowId?: Record<string, RowIssues>;
  /** Table size. Defaults to 'lg'. */
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  /**
   * When true, each row renders with an expand chevron and, if
   * `renderExpandedContent` is supplied, its output is shown in a spanning row
   * beneath the row while it is expanded.
   */
  expandable?: boolean;
  renderExpandedContent?: (row: T) => ReactNode;
  /** Controlled expansion by row `id`; omit for uncontrolled internal state. */
  expandedRowIds?: Set<string>;
  onExpandedRowIdsChange?: (next: Set<string>) => void;
  /** Optional per-row class, e.g. to accent rows by validation severity. */
  rowClassName?: (row: T) => string | undefined;
}

/** Renders the error/warning icons for a set of issues, with the text in a native tooltip. */
const IssueMarkers = ({ issues }: { issues: CellIssue[] }): ReactElement | null => {
  if (issues.length === 0) return null;
  const hasError = issues.some((i) => i.type === 'ERROR');
  const title = issues.map((i) => i.message).join('\n');
  const Icon = hasError ? ErrorFilled : WarningAltFilled;
  return (
    <span
      className={`data-preview-table__issue data-preview-table__issue--${hasError ? 'error' : 'warning'}`}
      title={title}
    >
      <Icon size={16} />
    </span>
  );
};

/** Minimal shape of the cells Carbon's DataTable hands to the render prop. */
interface PreviewCell {
  id: string;
  value: unknown;
  info: { header: string };
}

/** Minimal shape of the rows Carbon's DataTable hands to the render prop. */
interface PreviewRow {
  id: string;
  cells: PreviewCell[];
}

/** Renders a single data cell, with its optional alignment and issue markers. */
const DataPreviewCell = <T,>({
  cell,
  column,
  dataRow,
  issues,
}: {
  cell: PreviewCell;
  column?: DataPreviewColumn<T>;
  dataRow: T;
  issues: CellIssue[];
}): ReactElement => (
  <TableCell style={column?.align === 'right' ? { textAlign: 'right' } : undefined}>
    <span className={`data-preview-table__cell${column?.align === 'right' ? ' data-preview-table__cell--right' : ''}`}>
      <span>{column?.renderCell ? column.renderCell(dataRow) : String(cell.value)}</span>
      <IssueMarkers issues={issues} />
    </span>
  </TableCell>
);

/** Builds the data cells for one row, attaching per-cell and row-level issues. */
const renderRowCells = <T,>(
  tableRow: PreviewRow,
  dataRow: T,
  columns: DataPreviewColumn<T>[],
  rowIssues?: RowIssues,
): ReactNode[] =>
  tableRow.cells.map((cell, cellIndex) => {
    const column = columns.find((c) => c.key === cell.info.header);
    const cellIssues = rowIssues?.fields[String(cell.info.header)] ?? [];
    // Surface row-level issues (no specific column) on the first cell.
    const issues = cellIndex === 0 ? [...cellIssues, ...(rowIssues?.row ?? [])] : cellIssues;
    return <DataPreviewCell key={cell.id} cell={cell} column={column} dataRow={dataRow} issues={issues} />;
  });

/** Renders a single (non-expandable) data row. */
const DataPreviewRow = <T,>({
  tableRow,
  dataRow,
  columns,
  rowIssues,
  className,
}: {
  tableRow: PreviewRow;
  dataRow: T;
  columns: DataPreviewColumn<T>[];
  rowIssues?: RowIssues;
  className?: string;
}): ReactElement => <TableRow className={className}>{renderRowCells(tableRow, dataRow, columns, rowIssues)}</TableRow>;

/**
 * DataPreviewTable renders a read-only Carbon table for previewing parsed data.
 * When there are no rows it shows a single centered, muted message spanning the
 * full width. When `issuesByRowId` is supplied, rows with validation issues are
 * highlighted and the affected cells show an inline error/warning marker whose
 * tooltip carries the message.
 *
 * @template T The shape of a single row; must include a string `id`.
 * @param {DataPreviewTableProps<T>} props - The component props.
 * @returns {ReactElement} The rendered table.
 */
const DataPreviewTable = <T extends { id: string }>({
  rows,
  columns,
  emptyMessage,
  issuesByRowId,
  size = 'lg',
  expandable = false,
  renderExpandedContent,
  expandedRowIds,
  onExpandedRowIdsChange,
  rowClassName,
}: DataPreviewTableProps<T>): ReactElement => {
  const headers = columns.map((col) => ({ key: col.key, header: col.header }));

  // Expansion is tracked by row id so a row stays open across `rows` changes.
  // Controlled when `expandedRowIds` is supplied; otherwise internal.
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
  const allExpanded = expandable && rows.length > 0 && rows.every((r) => expandedIds.has(r.id));

  return (
    <div className={`data-preview-table${expandable ? ' data-preview-table--expandable' : ''}`}>
      <DataTable rows={rows} headers={headers}>
        {({
          rows: tableRows,
          headers: tableHeaders,
          getTableProps,
          getHeaderProps,
          getRowProps,
          getExpandHeaderProps,
        }) => (
          <Table {...getTableProps()} size={size}>
            <TableHead>
              <TableRow>
                {expandable ? (
                  <TableExpandHeader
                    {...(getExpandHeaderProps ? getExpandHeaderProps() : {})}
                    isExpanded={allExpanded}
                    onExpand={() => commitExpanded(allExpanded ? new Set() : new Set(rows.map((r) => r.id)))}
                  />
                ) : null}
                {tableHeaders.map((header) => {
                  const colDef = columns.find((c) => c.key === header.key);
                  return (
                    <TableHeader
                      {...getHeaderProps({ header })}
                      key={header.key}
                      style={colDef?.align === 'right' ? { textAlign: 'right' } : undefined}
                    >
                      {header.header}
                    </TableHeader>
                  );
                })}
              </TableRow>
            </TableHead>
            <TableBody>
              {tableRows.length === 0 ? (
                <tr className="data-preview-table__empty-row">
                  <td colSpan={tableHeaders.length + (expandable ? 1 : 0)}>
                    <p className="data-preview-table__empty-message">{emptyMessage}</p>
                  </td>
                </tr>
              ) : (
                tableRows.map((tableRow) => {
                  const dataRow = rows.find((r) => r.id === tableRow.id);
                  if (!dataRow) return null;
                  const rowIssues = issuesByRowId?.[tableRow.id];
                  const customRowClass = rowClassName?.(dataRow);
                  if (expandable) {
                    // React forbids spreading a `key` from a props object; lift the
                    // key Carbon supplies onto the Fragment and merge its className
                    // with the caller's so both survive.
                    const { key: rowKey, className: carbonRowClass, ...rowProps } = getRowProps({ row: tableRow });
                    const mergedClass = [carbonRowClass, customRowClass].filter(Boolean).join(' ') || undefined;
                    return (
                      <Fragment key={rowKey}>
                        <TableExpandRow
                          {...rowProps}
                          className={mergedClass}
                          isExpanded={expandedIds.has(tableRow.id)}
                          onExpand={() => toggleExpanded(tableRow.id)}
                        >
                          {renderRowCells(tableRow, dataRow, columns, rowIssues)}
                        </TableExpandRow>
                        {renderExpandedContent ? (
                          <TableExpandedRow colSpan={tableHeaders.length + 1}>
                            {renderExpandedContent(dataRow)}
                          </TableExpandedRow>
                        ) : null}
                      </Fragment>
                    );
                  }
                  return (
                    <DataPreviewRow
                      key={tableRow.id}
                      tableRow={tableRow}
                      dataRow={dataRow}
                      columns={columns}
                      rowIssues={rowIssues}
                      className={customRowClass}
                    />
                  );
                })
              )}
            </TableBody>
          </Table>
        )}
      </DataTable>
    </div>
  );
};

export default DataPreviewTable;
