import { Logout } from '@carbon/icons-react';
import { Button } from '@carbon/react';
import { type FC } from 'react';

import { useAuth } from '@/context/auth/useAuth';

import './index.scss';

export const HeaderPanelProfile: FC = () => {
  const { user, signOut } = useAuth();

  return (
    <div className="header-panel-profile-container">
      <div className="header-panel-profile-content">
        <p className="header-panel-profile-name">{user?.displayName ?? user?.username}</p>
        <p className="header-panel-profile-email">{user?.email}</p>
      </div>
      <Button kind="ghost" renderIcon={Logout} iconDescription="Sign out" onClick={signOut}>
        Sign out
      </Button>
    </div>
  );
};
