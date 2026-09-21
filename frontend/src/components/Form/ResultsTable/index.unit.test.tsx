import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { StrictMode } from 'react';
import { describe, it, expect, vi } from 'vitest';

import ResultsTable, { type ResultsTableColumn } from './index';

interface Row {
  id: string;
  name: string;
}

const columns: ResultsTableColumn<Row>[] = [{ key: 'name', header: 'Name' }];

describe('ResultsTable - page clamping', () => {
  it('clamps a page beyond the last valid page', async () => {
    const onPaginationChange = vi.fn();
    render(
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        page={5}
        pageSize={10}
        totalItems={30}
        isLoading={false}
        onPaginationChange={onPaginationChange}
      />,
    );

    await waitFor(() => {
      expect(onPaginationChange).toHaveBeenCalledWith({ page: 3, pageSize: 10 });
    });
  });

  it('does not clamp an in-range page', async () => {
    const onPaginationChange = vi.fn();
    render(
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        page={2}
        pageSize={10}
        totalItems={30}
        isLoading={false}
        onPaginationChange={onPaginationChange}
      />,
    );

    // Give any (incorrect) effect a chance to fire before asserting it didn't.
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(onPaginationChange).not.toHaveBeenCalled();
  });

  it('does not rewrite the page when totalItems is 0', async () => {
    const onPaginationChange = vi.fn();
    render(
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        page={5}
        pageSize={10}
        totalItems={0}
        isLoading={false}
        onPaginationChange={onPaginationChange}
      />,
    );

    // A total of 0 is also what an in-flight or failed fetch looks like here, so the
    // parent's page is left alone; the control is kept coherent by clamping what it
    // displays instead (see the forward-button tests below).
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(onPaginationChange).not.toHaveBeenCalled();
  });

  it('disables the forward button for a page past the end of an empty result set', () => {
    const onPaginationChange = vi.fn();
    const { container } = render(
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        page={3}
        pageSize={20}
        totalItems={0}
        isLoading={false}
        onPaginationChange={onPaginationChange}
      />,
    );

    const next = container.querySelector('.cds--pagination__button--forward') as HTMLButtonElement;
    expect(next.disabled).toBe(true);
    fireEvent.click(next);
    expect(onPaginationChange).not.toHaveBeenCalled();
  });

  it('does not let a page below 1 reach the control', () => {
    const onPaginationChange = vi.fn();
    const { container } = render(
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        page={0}
        pageSize={20}
        totalItems={100}
        isLoading={false}
        onPaginationChange={onPaginationChange}
      />,
    );

    // Carbon disables its back button on `page === 1` alone, so an unclamped 0 would
    // leave it live and send the parent to -1.
    const back = container.querySelector('.cds--pagination__button--backward') as HTMLButtonElement;
    expect(back.disabled).toBe(true);
    fireEvent.click(back);
    expect(onPaginationChange).not.toHaveBeenCalled();
  });

  it('derives the page count from the same pageSize fallback the control uses', () => {
    const onPaginationChange = vi.fn();
    const { container } = render(
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        page={3}
        totalItems={100}
        isLoading={false}
        onPaginationChange={onPaginationChange}
      />,
    );

    // With `pageSize` omitted both sides fall back to 20, so this is 5 pages and page 3
    // is in range — a mismatched fallback would have pinned the display to page 1.
    const pageSelect = container.querySelector('select[id$="-right"]') as HTMLSelectElement;
    expect(pageSelect.options.length).toBe(5);
    expect(pageSelect.value).toBe('3');
  });

  it('disables the forward button while a fetch is in flight with no total yet', () => {
    const onPaginationChange = vi.fn();
    const { container } = render(
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        isLoading
        page={3}
        pageSize={20}
        totalItems={0}
        onPaginationChange={onPaginationChange}
      />,
    );

    // Without the display clamp this read "Page of 1 page" with a live Next, and one
    // click sent the parent to page 4 — a page it had never shown.
    const next = container.querySelector('.cds--pagination__button--forward') as HTMLButtonElement;
    expect(next.disabled).toBe(true);
    fireEvent.click(next);
    expect(onPaginationChange).not.toHaveBeenCalled();
  });

  it('does not clamp when totalItems is undefined', async () => {
    const onPaginationChange = vi.fn();
    render(
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        page={5}
        pageSize={10}
        isLoading={false}
        onPaginationChange={onPaginationChange}
      />,
    );

    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(onPaginationChange).not.toHaveBeenCalled();
  });

  it('does not clamp while loading', async () => {
    const onPaginationChange = vi.fn();
    render(
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        page={5}
        pageSize={10}
        totalItems={30}
        isLoading
        onPaginationChange={onPaginationChange}
      />,
    );

    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(onPaginationChange).not.toHaveBeenCalled();
  });

  it('does not clamp at the exact last page boundary', async () => {
    const onPaginationChange = vi.fn();
    render(
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        page={3}
        pageSize={10}
        totalItems={30}
        isLoading={false}
        onPaginationChange={onPaginationChange}
      />,
    );

    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(onPaginationChange).not.toHaveBeenCalled();
  });

  it('stops clamping once the parent feeds the clamped page back in (no infinite loop)', async () => {
    const onPaginationChange = vi.fn();
    const makeElement = (page: number) => (
      <ResultsTable
        rows={[]}
        columns={columns}
        hasSearched
        page={page}
        pageSize={10}
        totalItems={30}
        isLoading={false}
        onPaginationChange={onPaginationChange}
      />
    );

    const { rerender } = render(makeElement(5));

    await waitFor(() => {
      expect(onPaginationChange).toHaveBeenCalledWith({ page: 3, pageSize: 10 });
    });
    expect(onPaginationChange).toHaveBeenCalledTimes(1);

    // Simulate the parent applying the clamp by feeding the corrected page back in.
    rerender(makeElement(3));

    // Give any (incorrect) re-fire a chance to happen before asserting it didn't.
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(onPaginationChange).toHaveBeenCalledTimes(1);
  });
});

describe('ResultsTable - keyword re-seed', () => {
  // The draft is re-seeded by comparing against the previous applied keyword during
  // render, which is React's documented way to adjust state when a prop changes. An
  // effect would trip the `react-hooks/set-state-in-effect` rule this repo enables and
  // cost a second render pass. Guarding it here so the pattern is not "corrected" back
  // into an effect: it only ever sets this component's own state, it is guarded by a
  // condition, and StrictMode's double render is clean.
  it('tracks the applied keyword in both directions under StrictMode, without React warnings', () => {
    const errors: string[] = [];
    const errSpy = vi.spyOn(console, 'error').mockImplementation((...a) => void errors.push(String(a[0])));
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation((...a) => void errors.push(String(a[0])));

    const table = (keyword: string) => (
      <StrictMode>
        <ResultsTable
          rows={[{ id: '1', name: 'Alpha' }]}
          columns={columns}
          searchKeyword={keyword}
          onSearchKeywordChange={vi.fn()}
        />
      </StrictMode>
    );

    const { rerender } = render(table('oak'));
    expect(screen.getByRole('searchbox')).toHaveValue('oak');

    // The reset a page's "Clear filters" performs.
    rerender(table(''));
    expect(screen.getByRole('searchbox')).toHaveValue('');

    rerender(table('cedar'));
    expect(screen.getByRole('searchbox')).toHaveValue('cedar');

    errSpy.mockRestore();
    warnSpy.mockRestore();
    expect(errors.filter((m) => !m.includes('key')).join(' | ')).toBe('');
  });
});
