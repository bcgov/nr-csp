import { Login } from '@carbon/icons-react';
import { Button } from '@carbon/react';
import { Navigate } from 'react-router';

import bcGovLogo from '@/assets/img/bc-gov-logo.png';
import forestPhoto from '@/assets/img/welcome-forest.webp';
import { LoadingScreen } from '@/components/core/LoadingScreen';
import { NotificationToast } from '@/components/Layout/NotificationToast';
import { useAuth } from '@/context/auth/useAuth';
import { useOauthCallbackPending } from '@/context/auth/useOauthCallbackPending';
import { useNotification } from '@/context/notification/useNotification';
import { ROUTES } from '@/routes/routePaths';

import './index.scss';

export function WelcomePage() {
  const { isAuthenticated, isLoading, signIn } = useAuth();
  const { addNotification } = useNotification();
  const isCallbackPending = useOauthCallbackPending();

  // A live session settles it, whatever is on the URL: stale callback params
  // survive a reload, a back-button and a second tab, and waiting on an
  // exchange that has already happened would only stall someone who is
  // signed in.
  if (isAuthenticated) return <Navigate to={ROUTES.SEARCH} replace />;
  if (isLoading || isCallbackPending) return <LoadingScreen />;

  const startIdirLogin = () =>
    void signIn().catch(() =>
      addNotification({
        kind: 'error',
        title: 'Could not start the IDIR login',
        subtitle: 'Please try again, or reload the page if this keeps happening.',
      }),
    );

  return (
    <main id="main-content" className="welcome-page">
      <div className="welcome-page__panel">
        <img className="welcome-page__logo" src={bcGovLogo} alt="Government of British Columbia" />
        <h1 className="welcome-page__heading">Welcome to CSP</h1>
        <p className="welcome-page__subheading">Coast Selling Price System</p>
        <div className="welcome-page__actions">
          <Button kind="primary" size="lg" renderIcon={Login} className="welcome-page__action" onClick={startIdirLogin}>
            Log in with IDIR
          </Button>
          <Button
            kind="tertiary"
            size="lg"
            renderIcon={Login}
            className="welcome-page__action"
            onClick={() =>
              addNotification({
                kind: 'info',
                title: 'Business BCeID login is not available yet',
                subtitle: 'Please log in with IDIR to continue.',
              })
            }
          >
            Log in with Business BCeID
          </Button>
        </div>
      </div>
      <div className="welcome-page__photo">
        <img src={forestPhoto} alt="" fetchPriority="high" />
      </div>
      <NotificationToast />
    </main>
  );
}
