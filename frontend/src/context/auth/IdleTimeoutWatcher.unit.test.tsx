import { render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { env } from '@/env';
import * as useIdleTimeoutModule from '@/hooks/useIdleTimeout';

import { IdleTimeoutWatcher } from './IdleTimeoutWatcher';
import * as useAuthModule from './useAuth';

vi.mock('./useAuth', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/hooks/useIdleTimeout', () => ({
  useIdleTimeout: vi.fn(),
}));

vi.mock('@/env', () => ({
  env: { mockUser: false, idleTimeoutMs: 30 * 60_000 },
}));

const mockUseAuth = useAuthModule.useAuth as ReturnType<typeof vi.fn>;
const mockUseIdleTimeout = useIdleTimeoutModule.useIdleTimeout as ReturnType<typeof vi.fn>;

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

  it('calls signOut via onTimeout', () => {
    const signOut = vi.fn();
    mockUseAuth.mockReturnValue({ isAuthenticated: true, signOut });

    render(<IdleTimeoutWatcher />);

    const { onTimeout } = mockUseIdleTimeout.mock.calls[0][0];
    onTimeout();

    expect(signOut).toHaveBeenCalledTimes(1);
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
});
