import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

import { LandingPage } from './index';

vi.mock('@/context/auth/useAuth', () => ({
  useAuth: () => ({ user: null }),
}));

vi.mock('@/services/health.service', () => ({
  useHealthQuery: () => ({
    data: undefined,
    isSuccess: false,
    isError: false,
    isFetching: false,
    refetch: vi.fn(),
  }),
}));

describe('LandingPage', () => {
  it('renders welcome heading', () => {
    render(<LandingPage />);
    expect(screen.getByRole('heading', { name: 'Welcome' })).toBeInTheDocument();
  });
});
