import { UserAvatar } from '@carbon/icons-react';
import { HeaderGlobalAction } from '@carbon/react';
import { type FC } from 'react';

import { MockRoleSelector } from '@/components/Layout/MockRoleSelector';
import { useAuth } from '@/context/auth/useAuth';
import { useLayout } from '@/context/layout/useLayout';
import { env } from '@/env';

import './LayoutHeaderGlobalBar.scss';

const LayoutHeaderGlobalBar: FC = () => {
  const { toggleHeaderPanel, isHeaderPanelOpen } = useLayout();
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) return null;

  // Dev-only: render role switcher when mock auth is active
  if (env.mockUser) {
    return <MockRoleSelector />;
  }

  return (
    <HeaderGlobalAction
      aria-label="User settings"
      tooltipAlignment="end"
      onClick={toggleHeaderPanel}
      isActive={isHeaderPanelOpen}
      className="profile-action-button"
    >
      <UserAvatar size={20} />
    </HeaderGlobalAction>
  );
};

export default LayoutHeaderGlobalBar;
