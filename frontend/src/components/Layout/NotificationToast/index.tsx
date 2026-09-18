import { ToastNotification } from '@carbon/react';

import { useNotification } from '@/context/notification/useNotification';
import './index.scss';

export const AUTO_CLOSE_MS = 6000;

export function NotificationToast() {
  const { notifications, removeNotification } = useNotification();

  if (notifications.length === 0) return null;

  return (
    <div className="notification-toast-container">
      {notifications.map((n) => (
        <ToastNotification
          key={n.id}
          kind={n.kind}
          title={n.title}
          subtitle={n.subtitle}
          timeout={n.persistent ? 0 : AUTO_CLOSE_MS}
          onClose={() => removeNotification(n.id)}
        />
      ))}
    </div>
  );
}
