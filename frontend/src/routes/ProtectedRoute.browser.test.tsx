import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { vi, describe, afterEach, it, expect } from 'vitest';

import * as useAuthModule from '@/context/auth/useAuth';

import { ProtectedRoute } from './ProtectedRoute';

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

function renderRoute() {
  return render(
    <MemoryRouter initialEntries={['/private']}>
      <Routes>
        <Route
          path="/private"
          element={
            <ProtectedRoute>
              <div>Private Content</div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

const mockUseAuth = useAuthModule.useAuth as ReturnType<typeof vi.fn>;

describe('ProtectedRoute', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('starts an IDIR sign-in when not authenticated', () => {
    const signIn = vi.fn();
    mockUseAuth.mockReturnValue({ user: null, isAuthenticated: false, isLoading: false, isSigningOut: false, signIn });

    const { container } = renderRoute();

    expect(container.textContent).not.toContain('Private Content');
    expect(signIn).toHaveBeenCalledWith('IDIR');
  });

  it('renders children when authenticated', () => {
    mockUseAuth.mockReturnValue({
      user: { idpProvider: 'IDIR' },
      isAuthenticated: true,
      isLoading: false,
      isSigningOut: false,
      signIn: vi.fn(),
    });

    const { getByText } = renderRoute();
    expect(getByText('Private Content')).toBeTruthy();
  });

  it('shows nothing (loading) while loading', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      isSigningOut: false,
      signIn: vi.fn(),
    });

    const { container } = renderRoute();
    expect(container.textContent).not.toContain('Private Content');
  });
});
