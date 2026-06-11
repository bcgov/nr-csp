import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { NotificationContext } from '@/context/notification/NotificationContext';

// Minimal stub so Layout doesn't blow up on auth/router deps
vi.mock('@/context/auth/useAuth', () => ({ useAuth: () => ({ isLoading: false }) }));
vi.mock('@/context/layout/LayoutProvider', () => ({
  LayoutProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));
vi.mock('@carbon/react', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@carbon/react')>();
  return {
    ...actual,
    HeaderContainer: ({ render: Render }: { render: React.FC }) => <Render />,
    Content: ({ children }: { children: React.ReactNode }) => <main>{children}</main>,
    Loading: () => null,
  };
});
vi.mock('@/components/Layout/LayoutHeader/LayoutHeader', () => ({
  LayoutHeader: () => null,
}));
vi.mock('react-router-dom', () => ({ Outlet: () => null }));

import Layout from './index';

describe('Layout notifications', () => {
  it('renders a success toast when a notification is in context', async () => {
    const notification = {
      id: '1',
      kind: 'success' as const,
      title: 'Sort code created.',
    };

    await act(async () => {
      render(
        <NotificationContext.Provider
          value={{
            notifications: [notification],
            addNotification: vi.fn(),
            removeNotification: vi.fn(),
          }}
        >
          <Layout />
        </NotificationContext.Provider>,
      );
    });

    expect(screen.getByText('Sort code created.')).toBeInTheDocument();
  });
});
