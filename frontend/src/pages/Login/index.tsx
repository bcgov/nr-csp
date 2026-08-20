import { Login as LoginIcon } from '@carbon/icons-react';
import { Button } from '@carbon/react';

import { useAuth } from '@/context/auth/useAuth';

import './index.scss';

export function LoginPage() {
  const { signIn } = useAuth();

  return (
    <div className="login-page">
      <div className="login-page__content">
        <h1 className="login-page__heading">Welcome to CSP</h1>
        <p className="login-page__body">Coast Selling Application</p>
        <div className="login-page__buttons">
          <Button kind="primary" size="lg" renderIcon={LoginIcon} onClick={() => void signIn('IDIR')}>
            Log in with IDIR
          </Button>
          <Button kind="tertiary" size="lg" renderIcon={LoginIcon} onClick={() => void signIn('BCEID')}>
            Log in with BCeID
          </Button>
        </div>
      </div>
    </div>
  );
}
