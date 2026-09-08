import { Home } from '@carbon/icons-react';
import { Button } from '@carbon/react';
import { useNavigate } from 'react-router';

import { LayoutHeader } from '@/components/Layout/LayoutHeader/LayoutHeader';
import { LayoutProvider } from '@/context/layout/LayoutProvider';
import { ROUTES } from '@/routes/routePaths';

import './index.scss';

export function LogoutPage() {
  const navigate = useNavigate();

  return (
    // The page sits outside the app shell (it must render unauthenticated), so
    // it brings its own header — LayoutProvider is what that header reads its
    // side-nav/profile-panel state from.
    <LayoutProvider>
      <LayoutHeader />
      <main id="main-content" className="logout-page">
        {/* Decorative, and it swaps per theme, so the two files are referenced
            from CSS rather than as an <img> src. */}
        <div className="logout-page__illustration" role="presentation" />
        <div className="logout-page__content">
          <h1 className="logout-page__heading">You&rsquo;ve successfully logged out</h1>
          <p className="logout-page__body">
            You have been securely logged out of CSP.
            <br />
            To continue working please sign back in.
          </p>
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
