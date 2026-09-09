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
  const [callbackTimedOut, setCallbackTimedOut] = useState(false);

  // Cognito redirects back to '/' after a successful sign-in (redirectSignIn),
  // so this component also renders mid-callback. Amplify reports
  // `isLoading: false` before the resulting `signedIn` Hub event lands, which
  // would flash the welcome screen at someone who has just authenticated — the
  // same reason ProtectedRoute watches for these two params.
  const params = new URLSearchParams(window.location.search);
  const isOauthCallback = params.has('code') && params.has('state');

  // Only Amplify's success path strips those params from the URL, so a failed
  // exchange — a replayed or expired code, a token request that never
  // answered — would otherwise leave this loading screen up for good, and
  // still up after a reload. Give the exchange a bounded wait, then drop the
  // params and offer the sign-in choice again.
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

  // signInWithRedirect can reject before it ever leaves the page — a session
  // that turned out to be live (loadUser deliberately swallows a failed
  // fetchAuthSession and leaves `user` null), a blocked redirect, a
  // misconfigured idpName. Swallowing that would leave a button that visibly
  // does nothing, however many times it is pressed.
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
      {/* Decorative: an empty alt keeps it out of the accessibility tree. It is
          also the largest thing on the page, so it is worth fetching early.
          The wrapper is load-bearing — see index.scss. */}
      <div className="welcome-page__photo">
        <img src={forestPhoto} alt="" fetchPriority="high" />
      </div>
      <NotificationToast />
    </main>
  );
}
