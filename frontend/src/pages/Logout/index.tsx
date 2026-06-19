import { Button } from '@carbon/react';
import { ArrowRight } from '@carbon/icons-react';

import { useAuth } from '@/context/auth/useAuth';

import './index.scss';

export function LogoutPage() {
  const { signIn } = useAuth();

  return (
    <div className="logout-page">
      <div className="logout-page__content">
        <h1 className="logout-page__heading">You have been signed out.</h1>
        <p className="logout-page__body">Thank you for using the Coast Selling Application.</p>
        <Button kind="primary" size="lg" renderIcon={ArrowRight} onClick={() => void signIn()}>
          Sign in again
        </Button>
      </div>
    </div>
  );
}
