import { render, screen, waitFor } from '@testing-library/react';
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

  it('does not clamp to page 0 when totalItems is 0', async () => {
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

    await new Promise((resolve) => setTimeout(resolve, 0));
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
