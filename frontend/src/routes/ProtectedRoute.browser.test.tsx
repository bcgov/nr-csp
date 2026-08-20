import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { vi, describe, afterEach, it, expect } from 'vitest';

import * as useAuthModule from '@/context/auth/useAuth';

import { ProtectedRoute } from './ProtectedRoute';
import { ROUTES } from './routePaths';

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

const mockUseAuth = useAuthModule.useAuth as ReturnType<typeof vi.fn>;

function renderRoute() {
  return render(
    <MemoryRouter initialEntries={['/private']}>
      <Routes>
        <Route path={ROUTES.LOGIN} element={<div>Login Page</div>} />
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

describe('ProtectedRoute', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('redirects to /login when not authenticated', () => {
    mockUseAuth.mockReturnValue({ user: null, isAuthenticated: false, isLoading: false, isSigningOut: false });

    const { container } = renderRoute();

    expect(container.textContent).toContain('Login Page');
    expect(container.textContent).not.toContain('Private Content');
  });

  it('renders children when authenticated', () => {
    mockUseAuth.mockReturnValue({
      user: { idpProvider: 'IDIR' },
      isAuthenticated: true,
      isLoading: false,
      isSigningOut: false,
    });

    const { getByText } = renderRoute();
    expect(getByText('Private Content')).toBeTruthy();
  });

  it('shows nothing (loading) while loading', () => {
    mockUseAuth.mockReturnValue({ user: null, isAuthenticated: false, isLoading: true, isSigningOut: false });

    const { container } = renderRoute();
    expect(container.textContent).not.toContain('Private Content');
  });
});
