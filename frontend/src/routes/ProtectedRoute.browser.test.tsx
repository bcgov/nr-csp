import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
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

  it('redirects an anonymous visitor to the landing route without starting sign-in', () => {
    const signIn = vi.fn();
    mockUseAuth.mockReturnValue({ isAuthenticated: false, isLoading: false, signIn });
    const { getByText } = render(
      <MemoryRouter initialEntries={['/home']}>
        <Routes>
          <Route path="/" element={<div>Choose a provider</div>} />
          <Route
            path="*"
            element={
              <ProtectedRoute>
                <div>Private Content</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(getByText('Choose a provider')).toBeTruthy();
    expect(signIn).not.toHaveBeenCalled();
  });

  it('renders children when authenticated', () => {
    mockUseAuth.mockReturnValue({
      user: { privileges: ['VIEW'] },
      isAuthenticated: true,
      isLoading: false,
      signIn: vi.fn(),
    });

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
