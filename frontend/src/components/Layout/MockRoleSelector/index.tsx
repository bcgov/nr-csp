import { ChevronDown } from '@carbon/icons-react';
import { type ChangeEvent } from 'react';

import { MOCK_ROLE_KEY, getStoredRole } from '@/context/auth/MockAuthProvider';
import { ROLES } from '@/context/auth/permissions';

import './index.scss';

export function MockRoleSelector() {
  const currentRole = getStoredRole();

  function handleChange(e: ChangeEvent<HTMLSelectElement>) {
    localStorage.setItem(MOCK_ROLE_KEY, e.target.value);
    window.location.reload();
  }

  return (
    <div className="mock-role-selector">
      <select aria-label="Mock user role" value={currentRole} onChange={handleChange}>
        {ROLES.map((role) => (
          <option key={role} value={role}>
            {role}
          </option>
        ))}
      </select>
      <span className="mock-role-selector__caret">
        <ChevronDown size={16} />
      </span>
    </div>
  );
}
