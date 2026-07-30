import { render, screen, within } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import DataPreviewTable, { type DataPreviewColumn, type RowIssues } from './index';

type Row = { id: string; name: string; qty: number };

const columns: DataPreviewColumn<Row>[] = [
  { key: 'name', header: 'Name' },
  {
    key: 'qty',
    header: 'Qty',
    align: 'right',
    renderCell: (row) => <em data-testid={`qty-${row.id}`}>{row.qty} units</em>,
  },
];

const rows: Row[] = [
  { id: 'r1', name: 'Alpha', qty: 3 },
  { id: 'r2', name: 'Beta', qty: 7 },
];

describe('DataPreviewTable', () => {
  it('renders headers from columns and one row per rows entry', () => {
    render(<DataPreviewTable rows={rows} columns={columns} emptyMessage="No data" />);

    expect(screen.getByText('Name')).toBeTruthy();
    expect(screen.getByText('Qty')).toBeTruthy();

    // header row + 2 body rows
    const allRows = screen.getAllByRole('row');
    expect(allRows).toHaveLength(3);
  });

  it('uses custom renderCell when provided and falls back to String(cell.value) otherwise', () => {
    render(<DataPreviewTable rows={rows} columns={columns} emptyMessage="No data" />);

    // renderCell output for the qty column
    expect(screen.getByTestId('qty-r1').textContent).toBe('3 units');
    expect(screen.getByTestId('qty-r2').textContent).toBe('7 units');

    // fallback String(cell.value) for the name column (no renderCell)
    expect(screen.getByText('Alpha')).toBeTruthy();
    expect(screen.getByText('Beta')).toBeTruthy();
  });

  it('applies right-align styling to header and cell for align: right columns and not for default columns', () => {
    const { container } = render(<DataPreviewTable rows={rows} columns={columns} emptyMessage="No data" />);

    // Header alignment
    const qtyHeader = screen.getByText('Qty').closest('th') as HTMLElement;
    expect(qtyHeader.style.textAlign).toBe('right');
    const nameHeader = screen.getByText('Name').closest('th') as HTMLElement;
    expect(nameHeader.style.textAlign).toBe('');

    // Cell alignment: right-aligned column applies the modifier class + style
    const rightCells = container.querySelectorAll('.data-preview-table__cell--right');
    expect(rightCells.length).toBe(2);

    const qtyCell = screen.getByTestId('qty-r1').closest('td') as HTMLElement;
    expect(qtyCell.style.textAlign).toBe('right');

    // Left/default column cell has no right modifier or alignment
    const nameCell = screen.getByText('Alpha').closest('td') as HTMLElement;
    expect(nameCell.style.textAlign).toBe('');
    expect(nameCell.querySelector('.data-preview-table__cell--right')).toBeNull();
  });

  it('renders the emptyMessage in a single spanning row when rows is empty', () => {
    const { container } = render(<DataPreviewTable rows={[]} columns={columns} emptyMessage="Nothing here" />);

    expect(screen.getByText('Nothing here')).toBeTruthy();

    const emptyRow = container.querySelector('.data-preview-table__empty-row');
    expect(emptyRow).not.toBeNull();
    const spanningCell = emptyRow?.querySelector('td');
    expect(spanningCell?.getAttribute('colspan')).toBe(String(columns.length));
  });

  it('renders field-level error and warning markers and row-level issues on the first cell', () => {
    const issuesByRowId: Record<string, RowIssues> = {
      r1: {
        fields: {
          name: [{ message: 'Name is invalid', type: 'ERROR' }],
          qty: [{ message: 'Qty is questionable', type: 'WARNING' }],
        },
        row: [{ message: 'Row-level problem', type: 'ERROR' }],
      },
      // r2 has no issues -> no markers
    };

    const { container } = render(
      <DataPreviewTable rows={rows} columns={columns} emptyMessage="No data" issuesByRowId={issuesByRowId} />,
    );

    // Field-level ERROR marker on the name cell with joined-message title.
    const nameCell = screen.getByText('Alpha').closest('td') as HTMLElement;
    const errorMarker = within(nameCell).getByTitle(/Name is invalid/);
    expect(errorMarker.className).toContain('data-preview-table__issue--error');
    // First cell also surfaces the row-level issue -> messages joined with newline.
    expect(errorMarker.getAttribute('title')).toBe('Name is invalid\nRow-level problem');

    // Field-level WARNING marker on the qty cell.
    const qtyCell = screen.getByTestId('qty-r1').closest('td') as HTMLElement;
    const warningMarker = within(qtyCell).getByTitle('Qty is questionable');
    expect(warningMarker.className).toContain('data-preview-table__issue--warning');

    // Row with no issues renders no marker at all.
    const r2Cell = screen.getByText('Beta').closest('td') as HTMLElement;
    expect(r2Cell.querySelector('.data-preview-table__issue')).toBeNull();

    // Sanity: exactly one error and one warning marker overall.
    expect(container.querySelectorAll('.data-preview-table__issue--error').length).toBe(1);
    expect(container.querySelectorAll('.data-preview-table__issue--warning').length).toBe(1);
  });

  it('joins multiple messages on one cell with a newline and gives ERROR icon precedence', () => {
    const issuesByRowId: Record<string, RowIssues> = {
      r1: {
        fields: {
          name: [
            { message: 'Warn first', type: 'WARNING' },
            { message: 'Error second', type: 'ERROR' },
          ],
        },
        row: [],
      },
    };

    render(<DataPreviewTable rows={rows} columns={columns} emptyMessage="No data" issuesByRowId={issuesByRowId} />);

    const nameCell = screen.getByText('Alpha').closest('td') as HTMLElement;
    const marker = nameCell.querySelector('.data-preview-table__issue') as HTMLElement;
    expect(marker.getAttribute('title')).toBe('Warn first\nError second');
    // ERROR takes icon precedence even though a warning is listed first.
    expect(marker.className).toContain('data-preview-table__issue--error');
    expect(marker.className).not.toContain('data-preview-table__issue--warning');
  });
});
