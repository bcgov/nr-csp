import { Content, HeaderContainer, Loading, ToastNotification } from '@carbon/react';
import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router';

import { useAuth } from '@/context/auth/useAuth';
import { useNotification } from '@/context/notification/useNotification';
import { LayoutProvider } from '@/context/layout/LayoutProvider';

import { LayoutHeader } from './LayoutHeader/LayoutHeader';
import './index.scss';
import type { FC } from 'react';

const Layout: FC = () => {
  const { isLoading } = useAuth();
  const { notifications, removeNotification } = useNotification();
  const { pathname } = useLocation();

  // Reset scroll position on every route change so a long report page's scroll
  // depth doesn't carry over to the next page.
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  if (isLoading) {
    return <Loading withOverlay />;
  }

  return (
    <>
      <LayoutProvider>
        <HeaderContainer render={LayoutHeader} />
        <Content className="app-content">
          <Outlet />
        </Content>
      </LayoutProvider>
      {/* Bottom-right toast stack — driven by the notification context.*/}
      <div className="layout-toast-container">
        {notifications.map((n) => (
          <ToastNotification
            key={n.id}
            kind={n.kind}
            title={n.title}
            subtitle={n.subtitle}
            timeout={5000}
            onClose={() => removeNotification(n.id)}
          />
        ))}
      </div>
    </>
  );
};

export default Layout;
