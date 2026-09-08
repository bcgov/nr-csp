import { Logout } from '@carbon/icons-react';
import { type FC } from 'react';

import AvatarImage from '@/components/Layout/AvatarImage';
import { useAuth } from '@/context/auth/useAuth';
import type { Role } from '@/context/auth/permissions';

import './index.scss';

// User-facing labels for the app-level role constants (see permissions.ts).
const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Admin',
  APPROVE: 'Approver',
  VIEW: 'Viewer',
};

// A user can hold several privileges; show the highest one, matching the single
// "Role: …" line in the design.
const ROLE_PRECEDENCE: Role[] = ['ADMIN', 'APPROVE', 'VIEW'];

const primaryRoleLabel = (privileges: Role[]): string | undefined => {
  const role = ROLE_PRECEDENCE.find((r) => privileges.includes(r));
  return role ? ROLE_LABELS[role] : undefined;
};

export const HeaderPanelProfile: FC = () => {
  const { user, signOut } = useAuth();

  // Never fall back to `username`: that is the raw Cognito id (e.g.
  // "dev-idir_<uuid>@idir") — the exact unformatted identifier this panel is
  // meant to hide. Prefer the display name, then the IDIR username, then the
  // local part of the email.
  const name = user?.displayName?.trim() || user?.idirUsername?.trim() || user?.email?.split('@')[0] || '';
  const roleLabel = user ? primaryRoleLabel(user.privileges) : undefined;

  return (
    <div className="header-panel-profile">
      <div className="header-panel-profile-identity">
        <AvatarImage userName={name} size="large" />
        <div className="header-panel-profile-details">
          <p className="header-panel-profile-name">{name}</p>
          {roleLabel && <p className="header-panel-profile-meta">Role: {roleLabel}</p>}
          {user?.idirUsername && <p className="header-panel-profile-meta">IDIR: {user.idirUsername}</p>}
          {user?.email && <p className="header-panel-profile-meta header-panel-profile-email">{user.email}</p>}
        </div>
      </div>

      <hr className="header-panel-profile-divider" />

      <button type="button" className="header-panel-profile-logout" onClick={signOut}>
        <Logout />
        <span>Log out</span>
      </button>
    </div>
  );
};
