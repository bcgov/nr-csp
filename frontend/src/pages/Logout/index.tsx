import { Home } from '@carbon/icons-react';
import { Button } from '@carbon/react';
import { useState } from 'react';
import { useNavigate } from 'react-router';

import { LayoutHeader } from '@/components/Layout/LayoutHeader/LayoutHeader';
import { takeSignOutReason } from '@/context/auth/signOutReason';
import { LayoutProvider } from '@/context/layout/LayoutProvider';
import { ROUTES } from '@/routes/routePaths';

import './index.scss';

/**
 * A deliberate sign-out and an expired session both land here — the URL the
 * federated logout chain returns to is fixed (see signOutReason) — so the
 * wording and illustration come from the reason stashed before the chain ran.
 */
const VARIANTS = {
  user: {
    modifier: 'logout-page--signed-out',
    heading: 'You’ve successfully logged out',
    body: ['You have been securely logged out of CSP.\nTo continue working please sign back in.'],
  },
  timeout: {
    modifier: 'logout-page--expired',
    heading: 'You’ve been signed out',
    body: [
      'Your session has ended and you’ve been securely logged out of CSP.',
      'To continue working please sign back in.',
    ],
  },
} as const;

export function LogoutPage() {
  const navigate = useNavigate();
  // Read once on mount: takeSignOutReason() clears the flag, so re-reading on a
  // later render would fall back to the deliberate-sign-out wording.
  const [variant] = useState(() => VARIANTS[takeSignOutReason()]);

  return (
    // The page sits outside the app shell (it must render unauthenticated), so
    // it brings its own header — LayoutProvider is what that header reads its
    // side-nav/profile-panel state from.
    <LayoutProvider>
      <LayoutHeader />
      <main id="main-content" className={`logout-page ${variant.modifier}`}>
        {/* Decorative, and it swaps per theme, so the files are referenced from
            CSS rather than as an <img> src. */}
        <div className="logout-page__illustration" role="presentation" />
        <div className="logout-page__content">
          <h1 className="logout-page__heading">{variant.heading}</h1>
          {variant.body.map((paragraph) => (
            <p className="logout-page__body" key={paragraph}>
              {paragraph}
            </p>
          ))}
          {/* Home is a protected route, so ProtectedRoute picks this up and
              starts a fresh sign-in — unless a session is somehow still live,
              in which case it just goes home. */}
          <Button
            kind="primary"
            size="lg"
            renderIcon={Home}
            className="logout-page__action"
            onClick={() => void navigate(ROUTES.LANDING)}
          >
            Back to home
          </Button>
        </div>
      </main>
    </LayoutProvider>
  );
}
