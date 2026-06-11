import { Button, Grid, Column } from '@carbon/react';
import { ArrowRight } from '@carbon/icons-react';
import { Navigate } from 'react-router-dom';

import { useAuth } from '@/context/auth/useAuth';
import { ROUTES } from '@/routes/routePaths';

import './index.scss';

export function LoginPage() {
  const { isAuthenticated, isLoading, signIn } = useAuth();

  if (isLoading) return null;

  // Authenticated users should not see the login page.
  if (isAuthenticated) return <Navigate to={ROUTES.SEARCH} replace />;

  return (
    <div className="login-page">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="login-page__card">
            <h1 className="login-page__title">Coast Selling Application</h1>
            <p className="login-page__subtitle">Sign in with your BC Government credentials to continue.</p>
            <Button
              kind="primary"
              size="lg"
              renderIcon={ArrowRight}
              iconDescription="Sign in"
              onClick={() => void signIn()}
            >
              Sign in with BC Government
            </Button>
          </div>
        </Column>
      </Grid>
    </div>
  );
}
