import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, afterEach, it, expect } from 'vitest';

import * as useAuthModule from '@/context/auth/useAuth';

import { ProtectedRoute } from './ProtectedRoute';

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

const mockUseAuth = useAuthModule.useAuth as ReturnType<typeof vi.fn>;

describe('ProtectedRoute', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('calls signIn and shows loading screen when not authenticated', () => {
    const signIn = vi.fn();
    mockUseAuth.mockReturnValue({ isAuthenticated: false, isLoading: false, signIn });

    const { container } = render(
      <MemoryRouter initialEntries={['/private']}>
        <ProtectedRoute>
          <div>Private Content</div>
        </ProtectedRoute>
      </MemoryRouter>,
    );

    expect(signIn).toHaveBeenCalledOnce();
    expect(container.textContent).not.toContain('Private Content');
  });

  it('renders children when authenticated', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true, isLoading: false, signIn: vi.fn() });

    const { getByText } = render(
      <MemoryRouter initialEntries={['/private']}>
        <ProtectedRoute>
          <div>Private Content</div>
        </ProtectedRoute>
      </MemoryRouter>,
    );
    expect(getByText('Private Content')).toBeTruthy();
  });

  it('shows loading screen while loading', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false, isLoading: true, signIn: vi.fn() });

    const { container } = render(
      <MemoryRouter>
        <ProtectedRoute>
          <div>Content</div>
        </ProtectedRoute>
      </MemoryRouter>,
    );
    expect(container.textContent).not.toContain('Content');
  });
});
