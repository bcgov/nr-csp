import { useCallback, useState, type Dispatch, type SetStateAction } from 'react';

import { usePersistentState } from '@/hooks/usePersistentState';

/**
 * The table-side state shared by the filter-and-search pages (Inbox, Invoice search):
 * the applied filter snapshot plus the pagination, sort and keyword that drive the query.
 *
 * @template TFilters The page's search-criteria params object.
 */
export interface SearchTableState<TFilters extends object> {
  hasSearched: boolean;
  setHasSearched: Dispatch<SetStateAction<boolean>>;
  /** Snapshot of the filter criteria taken at the moment Search is clicked. */
  appliedFilters: TFilters;
  setAppliedFilters: Dispatch<SetStateAction<TFilters>>;
  page: number;
  setPage: Dispatch<SetStateAction<number>>;
  pageSize: number;
  setPageSize: Dispatch<SetStateAction<number>>;
  /** Spring-style "field,direction" sort string, or undefined when unsorted. */
  sortParam: string | undefined;
  setSortParam: Dispatch<SetStateAction<string | undefined>>;
  keyword: string;
  setKeyword: Dispatch<SetStateAction<string>>;
  /** Pass as ResultsTable's `key`; `reset()` bumps it to force a remount. */
  tableKey: number;
  /** Returns every value above to its default and remounts the table. */
  reset: () => void;
}

/**
 * Owns the persisted table state for a filter-and-search page.
 *
 * Page, size, sort and keyword are tracked separately from `appliedFilters` so that
 * changing them re-queries without re-snapshotting the filter inputs. Everything is
 * persisted under `namespace` (which MUST begin with `csp.table.` so
 * `clearPersistedTableState()` sweeps it) and so survives a trip to a detail page.
 *
 * @param namespace sessionStorage namespace, e.g. `csp.table.inbox.v1`.
 * @param defaultPageSize Rows per page before the user chooses, and after `reset()`.
 */
export function useSearchTableState<TFilters extends object>(
  namespace: string,
  defaultPageSize: number,
): SearchTableState<TFilters> {
  const [hasSearched, setHasSearched] = usePersistentState(namespace, 'hasSearched', false);
  const [appliedFilters, setAppliedFilters] = usePersistentState<TFilters>(namespace, 'appliedFilters', {} as TFilters);
  const [page, setPage] = usePersistentState(namespace, 'page', 1);
  const [pageSize, setPageSize] = usePersistentState(namespace, 'pageSize', defaultPageSize);
  const [sortParam, setSortParam] = usePersistentState<string | undefined>(namespace, 'sort', undefined);
  const [keyword, setKeyword] = usePersistentState(namespace, 'keyword', '');

  // ResultsTable owns its sort direction internally and it cannot be cleared through
  // props — bumping this key remounts it, which resets it. Its keyword input re-seeds
  // itself from `searchKeyword`, so that one is already covered by `setKeyword('')`.
  const [tableKey, setTableKey] = useState(0);

  // Returns the results table to its default, pre-search state. Without this, clearing the
  // filters leaves the previous search on screen, no longer matching the cleared criteria.
  const reset = useCallback(() => {
    setHasSearched(false);
    setAppliedFilters({} as TFilters);
    setPage(1);
    setPageSize(defaultPageSize);
    setSortParam(undefined);
    setKeyword('');
    setTableKey((k) => k + 1);
  }, [defaultPageSize, setHasSearched, setAppliedFilters, setPage, setPageSize, setSortParam, setKeyword]);

  return {
    hasSearched,
    setHasSearched,
    appliedFilters,
    setAppliedFilters,
    page,
    setPage,
    pageSize,
    setPageSize,
    sortParam,
    setSortParam,
    keyword,
    setKeyword,
    tableKey,
    reset,
  };
}
