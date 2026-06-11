import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import * as useNotificationModule from '@/context/notification/useNotification';

import { NotificationToast } from './index';

vi.mock('@/context/notification/useNotification', () => ({
  useNotification: vi.fn(),
}));

const mockUseNotification = useNotificationModule.useNotification as ReturnType<typeof vi.fn>;

const withNotifications = (notifications: unknown[], removeNotification = vi.fn()) => {
  mockUseNotification.mockReturnValue({ notifications, removeNotification });
  return removeNotification;
};

describe('NotificationToast', () => {
  beforeEach(() => mockUseNotification.mockReset());

  it('renders nothing when there are no notifications', () => {
    withNotifications([]);
    const { container } = render(<NotificationToast />);
    expect(container.querySelector('.notification-toast-container')).toBeNull();
  });

  it('renders a toast per notification with title and subtitle', () => {
    withNotifications([
      { id: '1', kind: 'success', title: 'Saved', subtitle: 'All good' },
      { id: '2', kind: 'error', title: 'Failed', subtitle: 'Try again' },
    ]);
    render(<NotificationToast />);
    expect(screen.getByText('Saved')).toBeInTheDocument();
    expect(screen.getByText('All good')).toBeInTheDocument();
    expect(screen.getByText('Failed')).toBeInTheDocument();
    expect(screen.getByText('Try again')).toBeInTheDocument();
  });

  it('removes a notification when its close button is clicked', () => {
    const removeNotification = withNotifications([{ id: '42', kind: 'info', title: 'Heads up' }]);
    render(<NotificationToast />);
    fireEvent.click(screen.getByRole('button'));
    expect(removeNotification).toHaveBeenCalledWith('42');
  });
});
