import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import App from './App';

// Force the mock auth path so the full provider tree renders without Cognito.
vi.mock('@/env', () => ({ env: { mockUser: true } }));

describe('App', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/logout');
  });

  it('renders the provider tree and serves the public logout route', async () => {
    render(<App />);

    expect(await screen.findByRole('heading', { name: /you have been signed out/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in again/i })).toBeInTheDocument();
  });
});
