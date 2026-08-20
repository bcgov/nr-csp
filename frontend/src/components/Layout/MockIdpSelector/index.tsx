import { ChevronDown } from '@carbon/icons-react';
import { type ChangeEvent } from 'react';

import { MOCK_IDP_KEY, getStoredIdp } from '@/context/auth/MockAuthProvider';

import './index.scss';

const IDP_OPTIONS = ['IDIR', 'BCEID'] as const;

/** Dev-only: lets a developer flip the mock user between IDIR and BCeID to exercise BCeID's page restrictions locally. */
export function MockIdpSelector() {
  const currentIdp = getStoredIdp();

  function handleChange(e: ChangeEvent<HTMLSelectElement>) {
    localStorage.setItem(MOCK_IDP_KEY, e.target.value);
    window.location.reload();
  }

  return (
    <div className="mock-idp-selector">
      <select aria-label="Mock identity provider" value={currentIdp} onChange={handleChange}>
        {IDP_OPTIONS.map((idp) => (
          <option key={idp} value={idp}>
            {idp}
          </option>
        ))}
      </select>
      <span className="mock-idp-selector__caret">
        <ChevronDown size={16} />
      </span>
    </div>
  );
}
