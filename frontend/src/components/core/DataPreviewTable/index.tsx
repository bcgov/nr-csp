import { ErrorFilled, WarningAltFilled } from '@carbon/icons-react';
import { DataTable, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@carbon/react';
import { type ReactElement, type ReactNode } from 'react';

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
}: DataPreviewTableProps<T>): ReactElement => {
  const headers = columns.map((col) => ({ key: col.key, header: col.header }));

  return (
    <div className="data-preview-table">
      <DataTable rows={rows} headers={headers}>
        {({ rows: tableRows, headers: tableHeaders, getTableProps, getHeaderProps }) => (
          <Table {...getTableProps()} size="lg">
            <TableHead>
              <TableRow>
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
                  <td colSpan={tableHeaders.length}>
                    <p className="data-preview-table__empty-message">{emptyMessage}</p>
                  </td>
                </tr>
              ) : (
                tableRows.map((tableRow) => {
                  const dataRow = rows.find((r) => r.id === tableRow.id);
                  if (!dataRow) return null;
                  const rowIssues = issuesByRowId?.[tableRow.id];
                  return (
                    <TableRow key={tableRow.id}>
                      {tableRow.cells.map((cell, cellIndex) => {
                        const column = columns.find((c) => c.key === cell.info.header);
                        const cellIssues = rowIssues?.fields[String(cell.info.header)] ?? [];
                        // Surface row-level issues (no specific column) on the first cell.
                        const issues = cellIndex === 0 ? [...cellIssues, ...(rowIssues?.row ?? [])] : cellIssues;
                        return (
                          <TableCell
                            key={cell.id}
                            style={column?.align === 'right' ? { textAlign: 'right' } : undefined}
                          >
                            <span
                              className={`data-preview-table__cell${
                                column?.align === 'right' ? ' data-preview-table__cell--right' : ''
                              }`}
                            >
                              <span>{column?.renderCell ? column.renderCell(dataRow) : String(cell.value)}</span>
                              <IssueMarkers issues={issues} />
                            </span>
                          </TableCell>
                        );
                      })}
                    </TableRow>
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
