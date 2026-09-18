import { type ReactNode, useEffect, useRef } from 'react';
import { Navigate } from 'react-router';

import { LoadingScreen } from '@/components/core/LoadingScreen';
import { useAuth } from '@/context/auth/useAuth';
import { useOauthCallbackPending } from '@/context/auth/useOauthCallbackPending';

import { ROUTES } from './routePaths';

interface Props {
  children: ReactNode;
  /** Set on the small set of routes BCeID users are permitted to reach. */
  bceidAllowed?: boolean;
}

export function ProtectedRoute({ children, bceidAllowed }: Props) {
  const { user, isAuthenticated, isLoading, isSigningOut, signIn } = useAuth();
  const isCallbackPending = useOauthCallbackPending();
  const loginAttempted = useRef(false);

  useEffect(() => {
    // Don't trigger a login redirect during an OAuth callback — Amplify is
    // still processing the code/state params and will fire a Hub signedIn
    // event, which a second sign-in started here would abandon. The wait is
    // bounded, so an exchange that never completes falls through to a fresh
    // sign-in rather than leaving this stuck on the loading screen.
    //
    // A bceidAllowed route can't assume the deep-linker is IDIR the way every
    // other route safely can (they're IDIR-only anyway) — the render below
    // sends an anonymous visitor to the welcome screen to choose instead.
    if (isCallbackPending || bceidAllowed) return;

    if (!isLoading && !isAuthenticated && !isSigningOut && !loginAttempted.current) {
      loginAttempted.current = true;
      void signIn('IDIR');
    }
  }, [isCallbackPending, isLoading, isAuthenticated, isSigningOut, signIn, bceidAllowed]);

  if (isLoading || isSigningOut) return <LoadingScreen />;

  if (!isAuthenticated) {
    if (bceidAllowed && !isCallbackPending) return <Navigate to={ROUTES.LANDING} replace />;
    return <LoadingScreen />;
  }

  if (user?.idpProvider === 'BCEIDBUSINESS' && !bceidAllowed) {
    return <Navigate to={ROUTES.UPLOAD_SUBMISSION} replace />;
  }

  return <>{children}</>;
}
