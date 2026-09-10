import { type ReactNode, useEffect, useRef } from 'react';

import { LoadingScreen } from '@/components/core/LoadingScreen';
import { useAuth } from '@/context/auth/useAuth';
import { useOauthCallbackPending } from '@/context/auth/useOauthCallbackPending';

interface Props {
  children: ReactNode;
}

export function ProtectedRoute({ children }: Props) {
  const { isAuthenticated, isLoading, isSigningOut, signIn } = useAuth();
  const isCallbackPending = useOauthCallbackPending();
  const loginAttempted = useRef(false);

  useEffect(() => {
    // Don't trigger a login redirect during an OAuth callback — Amplify is
    // still processing the code/state params and will fire a Hub signedIn
    // event, which a second sign-in started here would abandon. The wait is
    // bounded, so an exchange that never completes falls through to a fresh
    // sign-in rather than leaving this stuck on the loading screen.
    if (isCallbackPending) return;

    if (!isLoading && !isAuthenticated && !isSigningOut && !loginAttempted.current) {
      loginAttempted.current = true;
      void signIn();
    }
  }, [isCallbackPending, isLoading, isAuthenticated, isSigningOut, signIn]);

  if (isLoading || isSigningOut || !isAuthenticated) return <LoadingScreen />;

  return <>{children}</>;
}
