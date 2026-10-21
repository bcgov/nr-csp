import { renderHook } from '@testing-library/react';
import { createElement } from 'react';
import { describe, it, expect } from 'vitest';

import { AuthContext } from './AuthContext';
import {
  type Role,
  TABLE_MAINTENANCE,
  ESF_SUBMIT,
  INVOICE_DETAILS_APPROVE,
  INBOX,
  REPORTS,
  INVOICE_DETAILS_DELETE,
  INVOICE_DETAILS_SAVE,
  SEARCH,
  SUBMISSION_HISTORY,
  SUBMISSION_UPLOAD,
} from './permissions';
import { usePageAccess } from './usePageAccess';
import { usePermission } from './usePermission';

import type { AuthContextValue } from './types';

function makeWrapper(privileges: Role[]) {
  const value: AuthContextValue = {
    user: {
      username: 'test-user',
      email: 'test@example.com',
      roles: [],
      privileges,
      forestClientNumbers: [],
    },
    isAuthenticated: true,
    isLoading: false,
    isSigningOut: false,
    signIn: async () => {},
    signOut: async () => {},
  };
  return ({ children }: { children: React.ReactNode }) => createElement(AuthContext.Provider, { value }, children);
}

function makeNullUserWrapper() {
  const value: AuthContextValue = {
    user: null,
    isAuthenticated: false,
    isLoading: false,
    isSigningOut: false,
    signIn: async () => {},
    signOut: async () => {},
  };
  return ({ children }: { children: React.ReactNode }) => createElement(AuthContext.Provider, { value }, children);
}

// ── ADMIN role ────────────────────────────────────────────────────────────────

describe('usePermission – ADMIN role', () => {
  it('grants TABLE_MAINTENANCE', () => {
    const { result } = renderHook(() => usePermission(TABLE_MAINTENANCE), {
      wrapper: makeWrapper(['ADMIN']),
    });
    expect(result.current).toBe(true);
  });

  it('grants ESF_SUBMIT', () => {
    const { result } = renderHook(() => usePermission(ESF_SUBMIT), {
      wrapper: makeWrapper(['ADMIN']),
    });
    expect(result.current).toBe(true);
  });

  it('grants INVOICE_DETAILS_APPROVE', () => {
    const { result } = renderHook(() => usePermission(INVOICE_DETAILS_APPROVE), {
      wrapper: makeWrapper(['ADMIN']),
    });
    expect(result.current).toBe(true);
  });

  it('grants INBOX', () => {
    const { result } = renderHook(() => usePermission(INBOX), {
      wrapper: makeWrapper(['ADMIN']),
    });
    expect(result.current).toBe(true);
  });

  it('grants REPORTS', () => {
    const { result } = renderHook(() => usePermission(REPORTS), {
      wrapper: makeWrapper(['ADMIN']),
    });
    expect(result.current).toBe(true);
  });
});

// ── APPROVE role ──────────────────────────────────────────────────────────────

describe('usePermission – APPROVE role', () => {
  it('grants INVOICE_DETAILS_APPROVE', () => {
    const { result } = renderHook(() => usePermission(INVOICE_DETAILS_APPROVE), {
      wrapper: makeWrapper(['APPROVE']),
    });
    expect(result.current).toBe(true);
  });

  it('grants INBOX', () => {
    const { result } = renderHook(() => usePermission(INBOX), {
      wrapper: makeWrapper(['APPROVE']),
    });
    expect(result.current).toBe(true);
  });

  it('denies TABLE_MAINTENANCE', () => {
    const { result } = renderHook(() => usePermission(TABLE_MAINTENANCE), {
      wrapper: makeWrapper(['APPROVE']),
    });
    expect(result.current).toBe(false);
  });

  it('denies ESF_SUBMIT', () => {
    const { result } = renderHook(() => usePermission(ESF_SUBMIT), {
      wrapper: makeWrapper(['APPROVE']),
    });
    expect(result.current).toBe(false);
  });

  it('grants INVOICE_DETAILS_DELETE', () => {
    const { result } = renderHook(() => usePermission(INVOICE_DETAILS_DELETE), {
      wrapper: makeWrapper(['APPROVE']),
    });
    expect(result.current).toBe(true);
  });
});

// ── VIEW role ─────────────────────────────────────────────────────────────────

describe('usePermission – VIEW role', () => {
  it('grants INBOX', () => {
    const { result } = renderHook(() => usePermission(INBOX), {
      wrapper: makeWrapper(['VIEW']),
    });
    expect(result.current).toBe(true);
  });

  it('denies INVOICE_DETAILS_APPROVE', () => {
    const { result } = renderHook(() => usePermission(INVOICE_DETAILS_APPROVE), {
      wrapper: makeWrapper(['VIEW']),
    });
    expect(result.current).toBe(false);
  });

  it('denies TABLE_MAINTENANCE', () => {
    const { result } = renderHook(() => usePermission(TABLE_MAINTENANCE), {
      wrapper: makeWrapper(['VIEW']),
    });
    expect(result.current).toBe(false);
  });

  it('denies INVOICE_DETAILS_SAVE', () => {
    const { result } = renderHook(() => usePermission(INVOICE_DETAILS_SAVE), {
      wrapper: makeWrapper(['VIEW']),
    });
    expect(result.current).toBe(false);
  });
});

// ── null / no user ────────────────────────────────────────────────────────────

describe('usePermission – SUBMITTER role (Business BCeID)', () => {
  it('grants upload and history only', () => {
    for (const action of [SUBMISSION_UPLOAD, SUBMISSION_HISTORY]) {
      const { result } = renderHook(() => usePermission(action), { wrapper: makeWrapper(['SUBMITTER']) });
      expect(result.current).toBe(true);
    }
  });

  it('denies every staff action', () => {
    for (const action of [INBOX, INVOICE_DETAILS_APPROVE, INVOICE_DETAILS_SAVE, TABLE_MAINTENANCE, REPORTS, SEARCH]) {
      const { result } = renderHook(() => usePermission(action), { wrapper: makeWrapper(['SUBMITTER']) });
      expect(result.current, action).toBe(false);
    }
  });

  it('staff roles keep upload and history', () => {
    for (const role of ['ADMIN', 'APPROVE', 'VIEW'] as Role[]) {
      const { result } = renderHook(() => usePermission(SUBMISSION_UPLOAD), { wrapper: makeWrapper([role]) });
      expect(result.current, role).toBe(true);
    }
  });
});

describe('usePermission – no user', () => {
  it('returns false for TABLE_MAINTENANCE when user is null', () => {
    const { result } = renderHook(() => usePermission(TABLE_MAINTENANCE), {
      wrapper: makeNullUserWrapper(),
    });
    expect(result.current).toBe(false);
  });

  it('returns false for INBOX when user is null', () => {
    const { result } = renderHook(() => usePermission(INBOX), {
      wrapper: makeNullUserWrapper(),
    });
    expect(result.current).toBe(false);
  });
});

// ── usePageAccess ─────────────────────────────────────────────────────────────

describe('usePageAccess', () => {
  it('ADMIN can access inbox page', () => {
    const { result } = renderHook(() => usePageAccess('inbox'), {
      wrapper: makeWrapper(['ADMIN']),
    });
    expect(result.current).toBe(true);
  });

  it('ADMIN can access reports page', () => {
    const { result } = renderHook(() => usePageAccess('reports'), {
      wrapper: makeWrapper(['ADMIN']),
    });
    expect(result.current).toBe(true);
  });

  it('ADMIN can access tableMaintenance page', () => {
    const { result } = renderHook(() => usePageAccess('tableMaintenance'), {
      wrapper: makeWrapper(['ADMIN']),
    });
    expect(result.current).toBe(true);
  });

  it('VIEW can access inbox page', () => {
    const { result } = renderHook(() => usePageAccess('inbox'), {
      wrapper: makeWrapper(['VIEW']),
    });
    expect(result.current).toBe(true);
  });

  it('VIEW cannot access tableMaintenance page', () => {
    const { result } = renderHook(() => usePageAccess('tableMaintenance'), {
      wrapper: makeWrapper(['VIEW']),
    });
    expect(result.current).toBe(false);
  });

  it('APPROVE can access reports page', () => {
    const { result } = renderHook(() => usePageAccess('reports'), {
      wrapper: makeWrapper(['APPROVE']),
    });
    expect(result.current).toBe(true);
  });

  it('returns false for unknown page when user is null', () => {
    const { result } = renderHook(() => usePageAccess('tableMaintenance'), {
      wrapper: makeNullUserWrapper(),
    });
    expect(result.current).toBe(false);
  });
});
