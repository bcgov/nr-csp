import { Login } from '@carbon/icons-react';
import { Button } from '@carbon/react';
import { useEffect, useState } from 'react';
import { Navigate } from 'react-router';

import bcGovLogo from '@/assets/img/bc-gov-logo.png';
import forestPhoto from '@/assets/img/welcome-forest.webp';
import { LoadingScreen } from '@/components/core/LoadingScreen';
import { NotificationToast } from '@/components/Layout/NotificationToast';
import { useAuth } from '@/context/auth/useAuth';
import { useNotification } from '@/context/notification/useNotification';
import { ROUTES } from '@/routes/routePaths';

import './index.scss';

/**
 * How long to sit on the loading screen waiting for Amplify to finish the code
 * exchange before treating the callback as failed. The happy path clears the
 * params itself in well under a second.
 */
const OAUTH_CALLBACK_TIMEOUT_MS = 15_000;

export function WelcomePage() {
  const { isAuthenticated, isLoading, signIn } = useAuth();
  const { addNotification } = useNotification();
  const [callbackTimedOut, setCallbackTimedOut] = useState(false);

  const params = new URLSearchParams(window.location.search);
  const isOauthCallback = params.has('code') && params.has('state');

  useEffect(() => {
    if (!isOauthCallback) return;
    const timer = setTimeout(() => {
      window.history.replaceState(null, '', window.location.pathname);
      setCallbackTimedOut(true);
    }, OAUTH_CALLBACK_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [isOauthCallback]);

  if (isLoading || (isOauthCallback && !callbackTimedOut)) return <LoadingScreen />;
  if (isAuthenticated) return <Navigate to={ROUTES.SEARCH} replace />;

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
