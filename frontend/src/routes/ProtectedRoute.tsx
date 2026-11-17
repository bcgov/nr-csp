import { Button } from '@carbon/react';
import { type ReactNode } from 'react';
import { createPath, Navigate } from 'react-router';

import { LoadingScreen } from '@/components/core/LoadingScreen';
import { useAuth } from '@/context/auth/useAuth';
import { useOauthCallbackPending } from '@/context/auth/useOauthCallbackPending';

import { canAccess, homeRouteFor } from './access';
import { ROUTES } from './routePaths';

interface Props {
  children: ReactNode;
  /** The page action this route needs (ROUTE_PAGES); omitted only on the layout route. */
  page?: string;
}

export function ProtectedRoute({ children, page }: Props) {
  const { user, isAuthenticated, isLoading, isSigningOut, signOut } = useAuth();
  const isCallbackPending = useOauthCallbackPending();

  if (isLoading || isSigningOut || (!isAuthenticated && isCallbackPending)) return <LoadingScreen />;
  if (!isAuthenticated)
    return <Navigate to={ROUTES.LANDING} replace state={{ returnTo: createPath(window.location) }} />;

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
