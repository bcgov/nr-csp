import { type ReactNode, useEffect, useRef } from 'react';

import { useAuth } from '@/context/auth/useAuth';
import { LoadingScreen } from '@/components/core/LoadingScreen';

interface Props {
  children: ReactNode;
}

export function ProtectedRoute({ children }: Props) {
  const { isAuthenticated, isLoading, isSigningOut, signIn } = useAuth();
  const loginAttempted = useRef(false);

  useEffect(() => {
    // Don't trigger a login redirect during an OAuth callback — Amplify is
    // still processing the code/state params and will fire a Hub signedIn event.
    const params = new URLSearchParams(window.location.search);
    if (params.has('code') && params.has('state')) return;

    if (!isLoading && !isAuthenticated && !isSigningOut && !loginAttempted.current) {
      loginAttempted.current = true;
      void signIn();
    }
  }, [isLoading, isAuthenticated, isSigningOut, signIn]);

  if (isLoading || isSigningOut || !isAuthenticated) return <LoadingScreen />;

  return <>{children}</>;
}
