import { render } from '@testing-library/react';
import { ErrorResponse } from 'oidc-client-ts';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { env } from '@/env';
import * as useIdleTimeoutModule from '@/hooks/useIdleTimeout';
import { getOidcUser } from '@/service/oidc-service';

import { IdleTimeoutWatcher } from './IdleTimeoutWatcher';
import { emitSessionExpired } from './sessionExpiredSignal';
import * as useAuthModule from './useAuth';

import type * as OidcService from '@/service/oidc-service';

vi.mock('./useAuth', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/hooks/useIdleTimeout', () => ({
  useIdleTimeout: vi.fn(),
}));

vi.mock('@/env', () => ({
  env: { mockUser: false, idleTimeoutMs: 30 * 60_000 },
}));

vi.mock('@/service/oidc-service', async (importOriginal) => ({
  getOidcUser: vi.fn(),
  isSessionEnded: (await importOriginal<typeof OidcService>()).isSessionEnded,
}));

vi.mock('./sessionExpiredSignal', () => ({
  emitSessionExpired: vi.fn(),
}));

const mockUseAuth = useAuthModule.useAuth as ReturnType<typeof vi.fn>;
const mockUseIdleTimeout = useIdleTimeoutModule.useIdleTimeout as ReturnType<typeof vi.fn>;
const mockGetOidcUser = vi.mocked(getOidcUser);
const mockEmitSessionExpired = vi.mocked(emitSessionExpired);

describe('IdleTimeoutWatcher', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('enables idle tracking when authenticated', () => {
    const signOut = vi.fn();
    mockUseAuth.mockReturnValue({ isAuthenticated: true, signOut });

    render(<IdleTimeoutWatcher />);

    expect(mockUseIdleTimeout).toHaveBeenCalledWith(
      expect.objectContaining({ enabled: true, timeoutMs: env.idleTimeoutMs }),
    );
  });

  it('signs out as a timeout, so the welcome screen explains why', () => {
    const signOut = vi.fn();
    mockUseAuth.mockReturnValue({ isAuthenticated: true, signOut });

    render(<IdleTimeoutWatcher />);

    const { onTimeout } = mockUseIdleTimeout.mock.calls[0][0];
    onTimeout();

    expect(signOut).toHaveBeenCalledTimes(1);
    expect(signOut).toHaveBeenCalledWith('timeout');
  });

  it('disables idle tracking when not authenticated', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false, signOut: vi.fn() });

    render(<IdleTimeoutWatcher />);

    expect(mockUseIdleTimeout).toHaveBeenCalledWith(expect.objectContaining({ enabled: false }));
  });

  it('disables idle tracking in mock-user dev mode even when authenticated', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true, signOut: vi.fn() });
    (env as { mockUser: boolean }).mockUser = true;

    render(<IdleTimeoutWatcher />);

    expect(mockUseIdleTimeout).toHaveBeenCalledWith(expect.objectContaining({ enabled: false }));

    (env as { mockUser: boolean }).mockUser = false;
  });

  it('renders nothing', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true, signOut: vi.fn() });

    const { container } = render(<IdleTimeoutWatcher />);

    expect(container).toBeEmptyDOMElement();
  });

  it('renews the access token on user activity', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true, signOut: vi.fn() });
    mockGetOidcUser.mockResolvedValue(null);

    render(<IdleTimeoutWatcher />);

    const { onActivity } = mockUseIdleTimeout.mock.calls[0][0];
    onActivity();

    expect(mockGetOidcUser).toHaveBeenCalledTimes(1);
    expect(mockGetOidcUser).toHaveBeenCalledWith();
  });

  it('reports an ended session when activity renewal is rejected by SSO', async () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true, signOut: vi.fn() });
    mockGetOidcUser.mockRejectedValue(new ErrorResponse({ error: 'invalid_grant' }));

    render(<IdleTimeoutWatcher />);

    const { onActivity } = mockUseIdleTimeout.mock.calls[0][0];
    onActivity();

    await vi.waitFor(() => expect(mockEmitSessionExpired).toHaveBeenCalledTimes(1));
  });

  it('keeps the session when activity renewal fails temporarily', async () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true, signOut: vi.fn() });
    const failure = new TypeError('Failed to fetch');
    mockGetOidcUser.mockRejectedValue(failure);

    render(<IdleTimeoutWatcher />);

    const { onActivity } = mockUseIdleTimeout.mock.calls[0][0];
    onActivity();
    await expect(mockGetOidcUser.mock.results[0].value).rejects.toBe(failure);
    await Promise.resolve();

    expect(mockEmitSessionExpired).not.toHaveBeenCalled();
  });
});
