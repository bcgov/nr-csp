import { Logout } from '@carbon/icons-react';
import { type FC } from 'react';

import AvatarImage from '@/components/Layout/AvatarImage';
import { BCEID_BUSINESS } from '@/context/auth/famRoles';
import { useAuth } from '@/context/auth/useAuth';

import type { Role } from '@/context/auth/permissions';

import './index.scss';

// User-facing labels for the app-level role constants (see permissions.ts).
const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Admin',
  APPROVE: 'Approver',
  VIEW: 'Viewer',
  SUBMITTER: 'Submitter',
};

// A user can hold several privileges; show the highest one, matching the single
// "Role: …" line in the design.
const ROLE_PRECEDENCE: Role[] = ['ADMIN', 'APPROVE', 'VIEW', 'SUBMITTER'];

const primaryRoleLabel = (privileges: Role[]): string | undefined => {
  const role = ROLE_PRECEDENCE.find((r) => privileges.includes(r));
  return role ? ROLE_LABELS[role] : undefined;
};

export const HeaderPanelProfile: FC = () => {
  const { user, signOut } = useAuth();

  // Never fall back to `username`: without a directory username that is the
  // raw Keycloak subject (e.g. "<guid>@azureidir") — the exact unformatted
  // identifier this panel is meant to hide. Prefer the display name, then the IDIR username, then the
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
          {user?.idirUsername && (
            <p className="header-panel-profile-meta">
              {user.identityProvider === BCEID_BUSINESS ? 'BCeID' : 'IDIR'}: {user.idirUsername}
            </p>
          )}
          {user?.email && <p className="header-panel-profile-meta header-panel-profile-email">{user.email}</p>}
        </div>
      </div>

      <hr className="header-panel-profile-divider" />

      {/* Wrapped rather than passed directly: signOut takes an optional reason,
          and `onClick={signOut}` would forward the click event as one. */}
      <button type="button" className="header-panel-profile-logout" onClick={() => void signOut()}>
        <Logout />
        <span>Log out</span>
      </button>
    </div>
  );
};
