import { Login } from '@carbon/icons-react';
import { Button } from '@carbon/react';
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
 * The public front door at '/'. It sits outside the app shell — no header and
 * no side nav, as the design has it — and is where an anonymous visitor picks
 * an identity provider. Apart from /logout, every other page is wrapped in
 * ProtectedRoute, which starts a sign-in of its own.
 *
 * Only IDIR is wired up: Business BCeID is in the design but has no provider
 * behind it yet, so that button stays exactly as designed and says so when
 * pressed rather than being greyed out.
 */
export function WelcomePage() {
  const { isAuthenticated, isLoading, signIn } = useAuth();
  const { addNotification } = useNotification();

  // Cognito redirects back to '/' after a successful sign-in (redirectSignIn),
  // so this component also renders mid-callback. Amplify reports
  // `isLoading: false` before the resulting `signedIn` Hub event lands, which
  // would flash the welcome screen at someone who has just authenticated — the
  // same reason ProtectedRoute watches for these two params.
  const params = new URLSearchParams(window.location.search);
  const isOauthCallback = params.has('code') && params.has('state');

  if (isLoading || isOauthCallback) return <LoadingScreen />;
  if (isAuthenticated) return <Navigate to={ROUTES.SEARCH} replace />;

  return (
    <main id="main-content" className="welcome-page">
      <div className="welcome-page__panel">
        <img className="welcome-page__logo" src={bcGovLogo} alt="Government of British Columbia" />
        <h1 className="welcome-page__heading">Welcome to CSP</h1>
        <p className="welcome-page__subheading">Coast Selling Price System</p>
        <div className="welcome-page__actions">
          <Button
            kind="primary"
            size="lg"
            renderIcon={Login}
            className="welcome-page__action"
            onClick={() => void signIn()}
          >
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
      {/* Decorative: an empty alt keeps it out of the accessibility tree. It is
          also the largest thing on the page, so it is worth fetching early. */}
      <img className="welcome-page__photo" src={forestPhoto} alt="" fetchPriority="high" />
      <NotificationToast />
    </main>
  );
}
