import { describe, it, expect, vi } from 'vitest';

import { emitSessionExpired, onSessionExpired } from './sessionExpiredSignal';

describe('sessionExpiredSignal', () => {
  it('calls a subscribed listener when the signal is emitted', () => {
    const listener = vi.fn();
    onSessionExpired(listener);

    emitSessionExpired();

    expect(listener).toHaveBeenCalledTimes(1);
  });

  it('notifies every subscribed listener', () => {
    const first = vi.fn();
    const second = vi.fn();
    onSessionExpired(first);
    onSessionExpired(second);

    emitSessionExpired();

    expect(first).toHaveBeenCalledTimes(1);
    expect(second).toHaveBeenCalledTimes(1);
  });

  it('stops notifying a listener after it unsubscribes', () => {
    const listener = vi.fn();
    const unsubscribe = onSessionExpired(listener);

    unsubscribe();
    emitSessionExpired();

    expect(listener).not.toHaveBeenCalled();
  });

  it('does nothing when emitted with no subscribers', () => {
    expect(() => emitSessionExpired()).not.toThrow();
  });
});
