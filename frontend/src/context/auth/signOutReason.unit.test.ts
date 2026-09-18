import { beforeEach, describe, expect, it, vi } from 'vitest';

import { setSignOutReason, takeSignOutReason } from './signOutReason';

describe('signOutReason', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('defaults to a deliberate sign-out when nothing was recorded', () => {
    expect(takeSignOutReason()).toBe('user');
  });

  it('round-trips a timeout', () => {
    setSignOutReason('timeout');

    expect(takeSignOutReason()).toBe('timeout');
  });

  it('clears the reason once read, so a later visit is not mislabelled', () => {
    setSignOutReason('timeout');

    expect(takeSignOutReason()).toBe('timeout');
    expect(takeSignOutReason()).toBe('user');
  });

  it('survives the sweeps performSignOut runs on the way out', () => {
    setSignOutReason('timeout');

    // clearPersistedTableState() removes only the `csp.table.` namespace, and
    // clearStoredTokens() only the Cognito prefix — neither may take this key.
    window.sessionStorage.setItem('csp.table.sortCode.v1.page', '3');
    for (const key of Object.keys({ ...window.sessionStorage })) {
      if (key.startsWith('csp.table.')) window.sessionStorage.removeItem(key);
    }

    expect(takeSignOutReason()).toBe('timeout');
  });

  it('falls back to the plain wording when storage is unavailable', () => {
    // Spy the instance, not Storage.prototype — happy-dom's sessionStorage does
    // not route through the prototype, so a prototype spy silently misses.
    vi.spyOn(window.sessionStorage, 'setItem').mockImplementation(() => {
      throw new Error('storage blocked');
    });
    vi.spyOn(window.sessionStorage, 'getItem').mockImplementation(() => {
      throw new Error('storage blocked');
    });

    expect(() => setSignOutReason('timeout')).not.toThrow();
    expect(takeSignOutReason()).toBe('user');
  });
});
