import { renderHook } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import { useIdleTimeout } from './useIdleTimeout';

const TIMEOUT_MS = 60_000;

describe('useIdleTimeout', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('calls onTimeout after timeoutMs with no activity', () => {
    const onTimeout = vi.fn();
    renderHook(() => useIdleTimeout({ enabled: true, timeoutMs: TIMEOUT_MS, onTimeout }));

    vi.advanceTimersByTime(TIMEOUT_MS - 1);
    expect(onTimeout).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(onTimeout).toHaveBeenCalledTimes(1);
  });

  it('resets the timer on local activity', () => {
    const onTimeout = vi.fn();
    renderHook(() => useIdleTimeout({ enabled: true, timeoutMs: TIMEOUT_MS, onTimeout }));

    vi.advanceTimersByTime(TIMEOUT_MS - 1);
    window.dispatchEvent(new Event('mousemove'));
    vi.advanceTimersByTime(TIMEOUT_MS - 1);

    expect(onTimeout).not.toHaveBeenCalled();
  });

  it('does not track activity or schedule a timeout when disabled', () => {
    const onTimeout = vi.fn();
    renderHook(() => useIdleTimeout({ enabled: false, timeoutMs: TIMEOUT_MS, onTimeout }));

    vi.advanceTimersByTime(TIMEOUT_MS * 2);

    expect(onTimeout).not.toHaveBeenCalled();
  });

  it('resets the timer when another tab reports activity', async () => {
    const onTimeout = vi.fn();
    renderHook(() => useIdleTimeout({ enabled: true, timeoutMs: TIMEOUT_MS, onTimeout }));

    const otherTab = new BroadcastChannel('csp-auth-idle');
    await vi.advanceTimersByTimeAsync(TIMEOUT_MS - 1);
    otherTab.postMessage({ type: 'activity' });
    // BroadcastChannel delivery is real-async even under fake timers; flush microtasks.
    await vi.advanceTimersByTimeAsync(0);
    await vi.advanceTimersByTimeAsync(TIMEOUT_MS - 1);

    expect(onTimeout).not.toHaveBeenCalled();
    otherTab.close();
  });

  it('calls onTimeout when another tab reports going idle', async () => {
    const onTimeout = vi.fn();
    renderHook(() => useIdleTimeout({ enabled: true, timeoutMs: TIMEOUT_MS, onTimeout }));

    const otherTab = new BroadcastChannel('csp-auth-idle');
    otherTab.postMessage({ type: 'logout' });
    await vi.advanceTimersByTimeAsync(0);

    expect(onTimeout).toHaveBeenCalledTimes(1);
    otherTab.close();
  });

  it('cleans up listeners and timer on unmount', () => {
    const onTimeout = vi.fn();
    const { unmount } = renderHook(() => useIdleTimeout({ enabled: true, timeoutMs: TIMEOUT_MS, onTimeout }));

    unmount();
    vi.advanceTimersByTime(TIMEOUT_MS * 2);

    expect(onTimeout).not.toHaveBeenCalled();
  });
});
