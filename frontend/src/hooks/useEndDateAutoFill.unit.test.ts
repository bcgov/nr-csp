import { renderHook } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

import { useEndDateAutoFill } from './useEndDateAutoFill';

describe('useEndDateAutoFill', () => {
  it('does nothing when start date is missing', () => {
    const setEndDate = vi.fn();
    const clearEndDateError = vi.fn();
    renderHook(() => useEndDateAutoFill({ startDate: null, timeFrame: '01', setEndDate, clearEndDateError }));
    expect(setEndDate).not.toHaveBeenCalled();
    expect(clearEndDateError).not.toHaveBeenCalled();
  });

  it('does nothing when time frame is missing', () => {
    const setEndDate = vi.fn();
    const clearEndDateError = vi.fn();
    renderHook(() =>
      useEndDateAutoFill({ startDate: new Date(2026, 2, 15), timeFrame: '', setEndDate, clearEndDateError }),
    );
    expect(setEndDate).not.toHaveBeenCalled();
    expect(clearEndDateError).not.toHaveBeenCalled();
  });

  it('computes and sets end date once both start date and time frame are set', () => {
    const setEndDate = vi.fn();
    const clearEndDateError = vi.fn();
    renderHook(() =>
      useEndDateAutoFill({ startDate: new Date(2026, 2, 15), timeFrame: '01', setEndDate, clearEndDateError }),
    );
    expect(setEndDate).toHaveBeenCalledWith(new Date(2026, 2, 31));
    expect(clearEndDateError).toHaveBeenCalledTimes(1);
  });

  it('recomputes when time frame changes', () => {
    const setEndDate = vi.fn();
    const clearEndDateError = vi.fn();
    const { rerender } = renderHook(
      ({ timeFrame }) =>
        useEndDateAutoFill({ startDate: new Date(2026, 2, 15), timeFrame, setEndDate, clearEndDateError }),
      { initialProps: { timeFrame: '01' } },
    );
    expect(setEndDate).toHaveBeenCalledWith(new Date(2026, 2, 31));

    rerender({ timeFrame: '03' });
    expect(setEndDate).toHaveBeenCalledWith(new Date(2026, 4, 31));
  });

  it('does not recompute when time frame is cleared back to empty', () => {
    const setEndDate = vi.fn();
    const clearEndDateError = vi.fn();
    const { rerender } = renderHook(
      ({ timeFrame }) =>
        useEndDateAutoFill({ startDate: new Date(2026, 2, 15), timeFrame, setEndDate, clearEndDateError }),
      { initialProps: { timeFrame: '01' } },
    );
    setEndDate.mockClear();

    rerender({ timeFrame: '' });
    expect(setEndDate).not.toHaveBeenCalled();
  });
});
