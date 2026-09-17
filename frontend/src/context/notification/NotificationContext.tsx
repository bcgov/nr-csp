import { createContext } from 'react';

export type NotificationKind = 'info' | 'success' | 'warning' | 'error';

export interface Notification {
  id: string;
  kind: NotificationKind;
  title: string;
  subtitle?: string;
  /**
   * Keeps the toast up until the user closes it, instead of auto-closing.
   * For notices the user may not be at the screen to catch — an idle-session
   * timeout being the case this exists for.
   */
  persistent?: boolean;
}

export interface NotificationContextValue {
  notifications: Notification[];
  addNotification: (n: Omit<Notification, 'id'>) => void;
  removeNotification: (id: string) => void;
}

export const NotificationContext = createContext<NotificationContextValue>({
  notifications: [],
  addNotification: () => {},
  removeNotification: () => {},
});
