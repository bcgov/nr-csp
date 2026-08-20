import { type ReactNode } from 'react';
import { Navigate } from 'react-router';

import { LoadingScreen } from '@/components/core/LoadingScreen';
import { useAuth } from '@/context/auth/useAuth';

import { ROUTES } from './routePaths';

interface Props {
  children: ReactNode;
  /** Set on the small set of routes BCeID users are permitted to reach. */
  bceidAllowed?: boolean;
}

export function ProtectedRoute({ children, bceidAllowed }: Props) {
  const { user, isAuthenticated, isLoading, isSigningOut } = useAuth();

  if (isLoading || isSigningOut) return <LoadingScreen />;

  if (!isAuthenticated) {
    // Don't redirect to /login during an OAuth callback — Amplify is still
    // processing the code/state params and will fire a Hub signedIn event.
    // Redirecting here would strip code/state from the URL before it can.
    const params = new URLSearchParams(window.location.search);
    if (params.has('code') && params.has('state')) return <LoadingScreen />;

    return <Navigate to={ROUTES.LOGIN} replace />;
  }

  if (user?.idpProvider === 'BCEID' && !bceidAllowed) {
    return <Navigate to={ROUTES.UPLOAD_SUBMISSION} replace />;
  }

  return <>{children}</>;
}
