import { render, waitFor } from '@testing-library/react';
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
