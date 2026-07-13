import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { Notification } from '@/context/notification/NotificationContext';
import { useNotification } from '@/context/notification/useNotification';

import { NotificationToast } from './index';

vi.mock('@/context/notification/useNotification', () => ({ useNotification: vi.fn() }));

describe('NotificationToast', () => {
  const removeNotification = vi.fn();

  const arrange = (notifications: Notification[]) => {
    vi.mocked(useNotification).mockReturnValue({
      notifications,
      addNotification: vi.fn(),
      removeNotification,
    });
    return render(<NotificationToast />);
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when there are no notifications', () => {
    const { container } = arrange([]);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders a toast with title and subtitle for each notification', () => {
    arrange([
      { id: '1', kind: 'success', title: 'Saved', subtitle: 'Invoice saved successfully' },
      { id: '2', kind: 'error', title: 'Failed' },
    ]);

    expect(screen.getByText('Saved')).toBeInTheDocument();
    expect(screen.getByText('Invoice saved successfully')).toBeInTheDocument();
    expect(screen.getByText('Failed')).toBeInTheDocument();
  });

  it('applies the notification kind to the toast', () => {
    const { container } = arrange([{ id: '1', kind: 'warning', title: 'Careful' }]);
    expect(container.querySelector('.cds--toast-notification--warning')).toBeInTheDocument();
  });

  it('removes the matching notification when a toast is closed', () => {
    arrange([{ id: 'abc', kind: 'info', title: 'Heads up' }]);

    fireEvent.click(screen.getByRole('button'));

    expect(removeNotification).toHaveBeenCalledTimes(1);
    expect(removeNotification).toHaveBeenCalledWith('abc');
  });
});
