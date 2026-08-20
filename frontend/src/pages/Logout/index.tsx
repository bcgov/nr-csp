import { Button } from '@carbon/react';
import { ArrowRight } from '@carbon/icons-react';
import { useNavigate } from 'react-router';

import { ROUTES } from '@/routes/routePaths';

import './index.scss';

export function LogoutPage() {
  const navigate = useNavigate();

  return (
    <div className="logout-page">
      <div className="logout-page__content">
        <h1 className="logout-page__heading">You have been signed out.</h1>
        <p className="logout-page__body">Thank you for using the Coast Selling Application.</p>
        <Button kind="primary" size="lg" renderIcon={ArrowRight} onClick={() => navigate(ROUTES.LOGIN)}>
          Sign in again
        </Button>
      </div>
    </div>
  );
}
