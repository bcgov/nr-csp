import { render } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import { NotificationContext } from './NotificationContext';
import { useNotification } from './useNotification';

function HookConsumer() {
  const { notifications } = useNotification();
  return <div data-testid="result">{notifications.length}</div>;
}

describe('useNotification', () => {
  it('returns context value when inside provider', () => {
    const value = {
      notifications: [{ id: '1', kind: 'info' as const, title: 'Test' }],
      addNotification: () => {},
      removeNotification: () => {},
    };
    const { getByTestId } = render(
      <NotificationContext.Provider value={value}>
        <HookConsumer />
      </NotificationContext.Provider>,
    );
    expect(getByTestId('result').textContent).toBe('1');
  });

  it('returns empty notifications from default context', () => {
    const { getByTestId } = render(<HookConsumer />);
    expect(getByTestId('result').textContent).toBe('0');
  });
});
