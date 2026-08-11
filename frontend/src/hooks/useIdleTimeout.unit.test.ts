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

  it('stops reacting to cross-tab messages after unmount', async () => {
    const onTimeout = vi.fn();
    const { unmount } = renderHook(() => useIdleTimeout({ enabled: true, timeoutMs: TIMEOUT_MS, onTimeout }));

    unmount();
    const otherTab = new BroadcastChannel('csp-auth-idle');
    otherTab.postMessage({ type: 'logout' });
    await vi.advanceTimersByTimeAsync(0);

    expect(onTimeout).not.toHaveBeenCalled();
    otherTab.close();
  });

  it('tears down and stops scheduling a timeout when enabled flips to false', () => {
    const onTimeout = vi.fn();
    const { rerender } = renderHook(({ enabled }) => useIdleTimeout({ enabled, timeoutMs: TIMEOUT_MS, onTimeout }), {
      initialProps: { enabled: true },
    });

    rerender({ enabled: false });
    vi.advanceTimersByTime(TIMEOUT_MS * 2);

    expect(onTimeout).not.toHaveBeenCalled();
  });

  it('throttles cross-tab activity broadcasts to at most one per throttle window', async () => {
    const onTimeout = vi.fn();
    renderHook(() => useIdleTimeout({ enabled: true, timeoutMs: TIMEOUT_MS, onTimeout }));

    const received: unknown[] = [];
    const listenerTab = new BroadcastChannel('csp-auth-idle');
    listenerTab.onmessage = ({ data }) => received.push(data);

    // Several activity events in quick succession should collapse to a single broadcast.
    window.dispatchEvent(new Event('mousemove'));
    window.dispatchEvent(new Event('mousemove'));
    window.dispatchEvent(new Event('keydown'));
    await vi.advanceTimersByTimeAsync(0);

    expect(received).toEqual([{ type: 'activity' }]);
    listenerTab.close();
  });
});
