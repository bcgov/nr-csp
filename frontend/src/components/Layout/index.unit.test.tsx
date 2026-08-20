import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi, afterEach } from 'vitest';
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

const mockUseLocation = vi.fn();
vi.mock('react-router', () => ({
  Outlet: () => null,
  useLocation: () => mockUseLocation(),
}));

import Layout from './index';

describe('Layout scroll reset', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderAtPath = (pathname: string) => {
    mockUseLocation.mockReturnValue({ pathname });
    return render(
      <NotificationContext.Provider value={{ notifications: [], addNotification: vi.fn(), removeNotification: vi.fn() }}>
        <Layout />
      </NotificationContext.Provider>,
    );
  };

  it('scrolls to the top on mount', async () => {
    const scrollToSpy = vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined);
    await act(async () => {
      renderAtPath('/r13-ad-hoc');
    });
    expect(scrollToSpy).toHaveBeenCalledWith(0, 0);
  });

  it('scrolls to the top again when the route changes', async () => {
    const scrollToSpy = vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined);
    let rerender!: ReturnType<typeof render>['rerender'];
    await act(async () => {
      ({ rerender } = renderAtPath('/r13-ad-hoc'));
    });
    scrollToSpy.mockClear();

    mockUseLocation.mockReturnValue({ pathname: '/r06-invoice-print-out' });
    await act(async () => {
      rerender(
        <NotificationContext.Provider
          value={{ notifications: [], addNotification: vi.fn(), removeNotification: vi.fn() }}
        >
          <Layout />
        </NotificationContext.Provider>,
      );
    });
    expect(scrollToSpy).toHaveBeenCalledWith(0, 0);
  });
});

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
