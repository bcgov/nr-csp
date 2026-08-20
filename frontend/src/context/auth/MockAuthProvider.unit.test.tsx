import { renderHook } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { createElement } from 'react';

import { MockAuthProvider, MOCK_IDP_KEY, MOCK_ROLE_KEY } from './MockAuthProvider';
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

describe('MockAuthProvider – idp from localStorage', () => {
  it('defaults to IDIR when localStorage is empty', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user?.idpProvider).toBe('IDIR');
  });

  it('uses BCEID when localStorage has BCEID', () => {
    localStorage.setItem(MOCK_IDP_KEY, 'BCEID');
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user?.idpProvider).toBe('BCEID');
  });

  it('falls back to IDIR when localStorage has an invalid value', () => {
    localStorage.setItem(MOCK_IDP_KEY, 'BCEIDBUSINESS');
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.user?.idpProvider).toBe('IDIR');
  });
});
