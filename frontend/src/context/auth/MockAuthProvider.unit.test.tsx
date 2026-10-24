import { renderHook } from '@testing-library/react';
import { createElement } from 'react';
import { describe, it, expect, beforeEach } from 'vitest';

import { MockAuthProvider } from './MockAuthProvider';
import { MOCK_FOREST_CLIENTS_KEY, MOCK_ROLE_KEY } from './mockUser';
import { useAuth } from './useAuth';

function wrapper({ children }: { children: React.ReactNode }) {
  return createElement(MockAuthProvider, null, children);
}

beforeEach(() => {
  localStorage.clear();
});

describe('MockAuthProvider – role from localStorage', () => {
  it('defaults to ADMIN privileges when localStorage is empty', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user?.privileges).toEqual(['ADMIN']);
    expect(result.current.user?.roles).toEqual(['CSP_ADMIN']);
  });

  it('uses APPROVE privileges when localStorage has APPROVE', () => {
    localStorage.setItem(MOCK_ROLE_KEY, 'APPROVE');
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user?.privileges).toEqual(['APPROVE']);
    expect(result.current.user?.roles).toEqual(['CSP_APPROVE']);
  });

  it('uses VIEW privileges when localStorage has VIEW', () => {
    localStorage.setItem(MOCK_ROLE_KEY, 'VIEW');
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user?.privileges).toEqual(['VIEW']);
  });

  it('falls back to ADMIN when localStorage has an invalid value', () => {
    localStorage.setItem(MOCK_ROLE_KEY, 'SUPERUSER');
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user?.privileges).toEqual(['ADMIN']);
  });
});

describe('MockAuthProvider – Business BCeID submitter', () => {
  it('maps SUBMITTER to client-scoped FAM roles for the default mock client', () => {
    localStorage.setItem(MOCK_ROLE_KEY, 'SUBMITTER');
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user).toMatchObject({
      identityProvider: 'bceidbusiness',
      roles: ['CSP_SUBMITTER_FOREST_CLIENT-00000001'],
      privileges: ['SUBMITTER'],
      forestClientNumbers: ['00000001'],
    });
  });

  it('uses the forest clients stored for the mock submitter', () => {
    localStorage.setItem(MOCK_ROLE_KEY, 'SUBMITTER');
    localStorage.setItem(MOCK_FOREST_CLIENTS_KEY, '00000002, 00000001');
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user?.forestClientNumbers).toEqual(['00000001', '00000002']);
  });

  it('gives no access when the stored clients are malformed, as the API would', () => {
    localStorage.setItem(MOCK_ROLE_KEY, 'SUBMITTER');
    localStorage.setItem(MOCK_FOREST_CLIENTS_KEY, '123');
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user?.privileges).toEqual([]);
  });

  it('keeps staff mock users on IDIR with no forest clients', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user).toMatchObject({ identityProvider: 'idir', forestClientNumbers: [] });
  });
});
