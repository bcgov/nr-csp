import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';

import ResultsTable, { type ResultsTableColumn } from './index';

interface Row {
  id: string;
  name: string;
  qty: number;
}

const columns: ResultsTableColumn<Row>[] = [
  { key: 'name', header: 'Name' },
  { key: 'qty', header: 'Quantity', renderCell: (r) => <span>Q:{r.qty}</span> },
];

const rows: Row[] = [
  { id: '1', name: 'Alpha', qty: 2 },
  { id: '2', name: 'Beta', qty: 5 },
];

describe('ResultsTable', () => {
  it('renders column headers, default cell values, and custom renderCell output', () => {
    render(<ResultsTable rows={rows} columns={columns} />);
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Quantity')).toBeInTheDocument();
    expect(screen.getByText('Alpha')).toBeInTheDocument();
    expect(screen.getByText('Q:2')).toBeInTheDocument();
    expect(screen.getByText('Q:5')).toBeInTheDocument();
  });

  it('renders the default "no search performed" empty state', () => {
    render(<ResultsTable rows={[]} columns={columns} />);
    expect(screen.getByText('No search performed')).toBeInTheDocument();
  });

  it('renders the "no results found" empty state after a search', () => {
    render(<ResultsTable rows={[]} columns={columns} hasSearched />);
    expect(screen.getByText('No results found')).toBeInTheDocument();
  });

  it('honours custom empty-state copy', () => {
    render(<ResultsTable rows={[]} columns={columns} emptyTitle="Nothing here" emptyDescription="Add a row" />);
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
    expect(screen.getByText('Add a row')).toBeInTheDocument();
  });

  it('renders a loading skeleton instead of rows when loading', () => {
    const { container } = render(<ResultsTable rows={[]} columns={columns} isLoading />);
    expect(container.querySelector('.cds--skeleton')).not.toBeNull();
    expect(screen.queryByText('No search performed')).not.toBeInTheDocument();
  });

  it('renders expanded content when a row is expanded', async () => {
    render(
      <ResultsTable
        rows={rows}
        columns={columns}
        expandable
        renderExpandedContent={(r) => <div>Detail for {r.name}</div>}
      />,
    );
    await userEvent.click(screen.getAllByLabelText(/expand current row/i)[0]);
    expect(screen.getByText('Detail for Alpha')).toBeInTheDocument();
  });

  it('shows the keyword search bar and reports the keyword on Enter', async () => {
    const onSearchKeywordChange = vi.fn();
    render(
      <ResultsTable rows={rows} columns={columns} searchKeyword="" onSearchKeywordChange={onSearchKeywordChange} />,
    );
    // The bar updates internal state on type and only reports on Enter / clear.
    await userEvent.type(screen.getByRole('searchbox'), 'alp{enter}');
    expect(onSearchKeywordChange).toHaveBeenCalledWith('alp');
  });

  it('renders a provided footer row', () => {
    render(
      <ResultsTable
        rows={rows}
        columns={columns}
        footerRow={
          <tr>
            <td>Totals</td>
          </tr>
        }
      />,
    );
    expect(screen.getByText('Totals')).toBeInTheDocument();
  });

  it('manages row expansion internally when uncontrolled', async () => {
    render(
      <ResultsTable
        rows={rows}
        columns={columns}
        expandable
        renderExpandedContent={(r) => <div>Detail for {r.name}</div>}
      />,
    );
    // Carbon keeps the toggle's label static ("Expand current row") and flips
    // aria-expanded. With no controlled props, clicking toggles the row's own state.
    const [first] = screen.getAllByLabelText(/expand current row/i);
    expect(first).toHaveAttribute('aria-expanded', 'false');
    await userEvent.click(first);
    expect(first).toHaveAttribute('aria-expanded', 'true');
  });

  it('controlled expansion reports the toggled id set and never falls back to internal state', async () => {
    const onExpandedRowIdsChange = vi.fn();
    const { rerender } = render(
      <ResultsTable
        rows={rows}
        columns={columns}
        expandable
        expandedRowIds={new Set<string>()}
        onExpandedRowIdsChange={onExpandedRowIdsChange}
        renderExpandedContent={(r) => <div>Detail for {r.name}</div>}
      />,
    );

    const [first] = screen.getAllByLabelText(/expand current row/i);
    expect(first).toHaveAttribute('aria-expanded', 'false');

    // Clicking a row's toggle reports the NEXT set to the parent...
    await userEvent.click(first);
    expect(onExpandedRowIdsChange).toHaveBeenCalledWith(new Set(['1']));

    // ...but because the parent owns the set (and the prop hasn't changed), the row
    // stays collapsed — proving controlled mode does NOT fall back to internal state.
    expect(screen.getAllByLabelText(/expand current row/i)[0]).toHaveAttribute('aria-expanded', 'false');

    // Once the parent feeds the new set back in, the controlled state is reflected.
    rerender(
      <ResultsTable
        rows={rows}
        columns={columns}
        expandable
        expandedRowIds={new Set(['1'])}
        onExpandedRowIdsChange={onExpandedRowIdsChange}
        renderExpandedContent={(r) => <div>Detail for {r.name}</div>}
      />,
    );
    expect(screen.getAllByLabelText(/expand current row/i)[0]).toHaveAttribute('aria-expanded', 'true');
  });
});
