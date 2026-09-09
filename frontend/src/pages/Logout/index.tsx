import { Home } from '@carbon/icons-react';
import { Button } from '@carbon/react';
import { useState } from 'react';
import { useNavigate } from 'react-router';

import signedOutDark from '@/assets/img/logout-illustration-dark.png';
import signedOutLight from '@/assets/img/logout-illustration.png';
import expiredDark from '@/assets/img/session-expired-illustration-dark.png';
import expiredLight from '@/assets/img/session-expired-illustration.png';
import { LayoutHeader } from '@/components/Layout/LayoutHeader/LayoutHeader';
import { takeSignOutReason } from '@/context/auth/signOutReason';
import { LayoutProvider } from '@/context/layout/LayoutProvider';
import { useTheme } from '@/context/theme/useTheme';
import { ROUTES } from '@/routes/routePaths';

import './index.scss';

/**
 * A deliberate sign-out and an expired session both land here — the URL the
 * federated logout chain returns to is fixed (see signOutReason) — so the
 * wording and illustration come from the reason stashed before the chain ran.
 *
 * Each illustration ships as a light/dark pair: its ground shadow is near-white
 * by design, which would otherwise be the brightest thing on a g100 page. The
 * src is chosen here rather than in CSS so the artwork can stay a real
 * `<img alt="">` — decorative, and excluded from the accessibility tree.
 */
const VARIANTS = {
  user: {
    modifier: 'logout-page--signed-out',
    heading: 'You’ve successfully logged out',
    body: ['You have been securely logged out of CSP.\nTo continue working please sign back in.'],
    illustration: { g10: signedOutLight, g100: signedOutDark },
  },
  timeout: {
    modifier: 'logout-page--expired',
    heading: 'You’ve been signed out',
    body: [
      'Your session has ended and you’ve been securely logged out of CSP.',
      'To continue working please sign back in.',
    ],
    illustration: { g10: expiredLight, g100: expiredDark },
  },
} as const;

export function LogoutPage() {
  const navigate = useNavigate();
  const { theme } = useTheme();
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
        {/* Decorative: an empty alt keeps it out of the accessibility tree. */}
        <img className="logout-page__illustration" src={variant.illustration[theme]} alt="" />
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
