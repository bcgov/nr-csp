import { render, screen, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import { NotificationProvider } from './NotificationProvider';
import { useNotification } from './useNotification';

function Consumer() {
  const { notifications, addNotification, removeNotification } = useNotification();
  return (
    <div>
      <span data-testid="count">{notifications.length}</span>
      <button onClick={() => addNotification({ kind: 'info', title: 'Hello' })}>add</button>
      <button onClick={() => notifications[0] && removeNotification(notifications[0].id)}>remove</button>
    </div>
  );
}

describe('NotificationProvider', () => {
  it('starts with no notifications', () => {
    render(
      <NotificationProvider>
        <Consumer />
      </NotificationProvider>,
    );
    expect(screen.getByTestId('count').textContent).toBe('0');
  });

  it('adds a notification', () => {
    render(
      <NotificationProvider>
        <Consumer />
      </NotificationProvider>,
    );
    act(() => screen.getByText('add').click());
    expect(screen.getByTestId('count').textContent).toBe('1');
  });

  it('removes a notification', () => {
    render(
      <NotificationProvider>
        <Consumer />
      </NotificationProvider>,
    );
    act(() => screen.getByText('add').click());
    act(() => screen.getByText('remove').click());
    expect(screen.getByTestId('count').textContent).toBe('0');
  });

  it('assigns a unique id to each notification', () => {
    render(
      <NotificationProvider>
        <Consumer />
      </NotificationProvider>,
    );
    act(() => screen.getByText('add').click());
    act(() => screen.getByText('add').click());
    expect(screen.getByTestId('count').textContent).toBe('2');
  });
});
