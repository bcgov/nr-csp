import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useNotification } from '@/context/notification/useNotification';

import { AUTO_CLOSE_MS, NotificationToast } from './index';

import type { Notification } from '@/context/notification/NotificationContext';

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

  describe('auto-close', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('closes an ordinary toast once its timeout elapses', () => {
      arrange([{ id: '1', kind: 'success', title: 'Saved' }]);

      act(() => {
        vi.advanceTimersByTime(AUTO_CLOSE_MS + 100);
      });

      expect(removeNotification).toHaveBeenCalledWith('1');
    });

    // A persistent notice is for something the user may not be at the screen
    // to catch — an idle-session timeout is the case it exists for, since the
    // sign-out fires precisely because nobody is there.
    it('leaves a persistent toast up indefinitely', () => {
      arrange([{ id: '1', kind: 'info', title: 'Your session has expired', persistent: true }]);

      act(() => {
        vi.advanceTimersByTime(10 * 60_000);
      });

      expect(removeNotification).not.toHaveBeenCalled();
      expect(screen.getByText('Your session has expired')).toBeInTheDocument();
    });
  });
});
