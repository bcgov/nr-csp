import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { usePersistentState, clearPersistedTableState, setSerializer } from './usePersistentState';

beforeEach(() => {
  window.sessionStorage.clear();
});

describe('usePersistentState', () => {
  it('returns the initial value when nothing is stored', () => {
    const { result } = renderHook(() => usePersistentState('csp.table.test.v1', 'page', 1));
    expect(result.current[0]).toBe(1);
  });

  it('persists updates to sessionStorage', () => {
    const { result } = renderHook(() => usePersistentState('csp.table.test.v1', 'page', 1));
    act(() => result.current[1](3));
    expect(result.current[0]).toBe(3);
    expect(window.sessionStorage.getItem('csp.table.test.v1.page')).toBe('3');
  });

  it('reads an existing stored value on mount', () => {
    window.sessionStorage.setItem('csp.table.test.v1.page', '5');
    const { result } = renderHook(() => usePersistentState('csp.table.test.v1', 'page', 1));
    expect(result.current[0]).toBe(5);
  });

  it('supports updater functions', () => {
    const { result } = renderHook(() => usePersistentState('csp.table.test.v1', 'page', 1));
    act(() => result.current[1]((p) => p + 1));
    expect(result.current[0]).toBe(2);
  });

  it('falls back to the initial value when stored JSON is corrupt', () => {
    window.sessionStorage.setItem('csp.table.test.v1.page', '{not json');
    const { result } = renderHook(() => usePersistentState('csp.table.test.v1', 'page', 1));
    expect(result.current[0]).toBe(1);
  });

  it('round-trips a Set with the set serializer', () => {
    const { result } = renderHook(() =>
      usePersistentState<Set<string>>('csp.table.test.v1', 'expanded', new Set(), setSerializer),
    );
    act(() => result.current[1](new Set(['a', 'b'])));
    expect(window.sessionStorage.getItem('csp.table.test.v1.expanded')).toBe('["a","b"]');

    const { result: restored } = renderHook(() =>
      usePersistentState<Set<string>>('csp.table.test.v1', 'expanded', new Set(), setSerializer),
    );
    expect([...restored.current[0]]).toEqual(['a', 'b']);
  });

  it('does not throw and stays in-memory when sessionStorage.setItem throws', () => {
    const spy = vi
      .spyOn(Storage.prototype, 'setItem')
      .mockImplementation(() => {
        throw new Error('denied');
      });
    const { result } = renderHook(() => usePersistentState('csp.table.test.v1', 'page', 1));
    expect(() => act(() => result.current[1](2))).not.toThrow();
    expect(result.current[0]).toBe(2);
    spy.mockRestore();
  });
});

describe('clearPersistedTableState', () => {
  it('removes only csp.table.* keys and leaves others intact', () => {
    window.sessionStorage.setItem('csp.table.search.v1.page', '2');
    window.sessionStorage.setItem('csp.table.inbox.v1.keyword', '"abc"');
    window.sessionStorage.setItem('amplify.foo', 'keep');

    clearPersistedTableState();

    expect(window.sessionStorage.getItem('csp.table.search.v1.page')).toBeNull();
    expect(window.sessionStorage.getItem('csp.table.inbox.v1.keyword')).toBeNull();
    expect(window.sessionStorage.getItem('amplify.foo')).toBe('keep');
  });
});
