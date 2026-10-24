import { Button } from '@carbon/react';
import { type ReactNode, useEffect, useRef } from 'react';
import { Navigate, useLocation } from 'react-router';

import { LoadingScreen } from '@/components/core/LoadingScreen';
import { useAuth } from '@/context/auth/useAuth';
import { useOauthCallbackPending } from '@/context/auth/useOauthCallbackPending';
import { WelcomePage } from '@/pages/Welcome';

import { canAccess, homeRouteFor, isIndustryPath } from './access';

interface Props {
  children: ReactNode;
  /**
   * The page action this route needs (see ROUTE_PAGES). A user without it is
   * sent to their own home page. Omitted only on the shared layout route,
   * whose child routes each name their own.
   */
  page?: string;
}

export function ProtectedRoute({ children, page }: Props) {
  const { user, isAuthenticated, isLoading, isSigningOut, signIn, signOut } = useAuth();
  const isCallbackPending = useOauthCallbackPending();
  const { pathname } = useLocation();
  // Industry users open these pages too, so a signed-out visitor picks IDIR or
  // Business BCeID on the welcome screen rather than being sent to IDIR. It renders
  // at the requested URL, which sign-in saves as its return path.
  const choosesProvider = isIndustryPath(pathname);
  const loginAttempted = useRef(false);

  useEffect(() => {
    // Don't trigger a login redirect while OAuth code/state params are on the
    // URL — a second sign-in started here would abandon an exchange in flight.
    // The wait is bounded, so params that are never exchanged fall through to a
    // fresh sign-in rather than leaving this stuck on the loading screen.
    if (isCallbackPending || choosesProvider) return;

    if (!isLoading && !isAuthenticated && !isSigningOut && !loginAttempted.current) {
      loginAttempted.current = true;
      void signIn();
    }
  }, [isCallbackPending, choosesProvider, isLoading, isAuthenticated, isSigningOut, signIn]);

  if (isLoading || isSigningOut) return <LoadingScreen />;
  if (!isAuthenticated) return choosesProvider && !isCallbackPending ? <WelcomePage /> : <LoadingScreen />;

  if (!user?.privileges.length)
    return (
      <main id="main-content">
        <h1>No CSP access</h1>
        <p>Your account has no CSP role. Contact your application administrator.</p>
        <Button onClick={() => void signOut()}>Sign out</Button>
      </main>
    );
  if (page && !canAccess(user, page)) return <Navigate to={homeRouteFor(user)} replace />;
  return <>{children}</>;
}
