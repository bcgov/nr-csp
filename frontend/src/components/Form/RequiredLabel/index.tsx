import type { ReactNode } from 'react';

import './index.scss';

interface RequiredLabelProps {
  children: ReactNode;
}

/**
 * Wraps a field label with a red asterisk prefix to signal that the field
 * is required.  Replaces the page-local RequiredLabel copies that previously
 * existed in every report and invoice page.
 *
 * Usage:
 *   labelText={<RequiredLabel>Start date</RequiredLabel>}
 */
const RequiredLabel = ({ children }: RequiredLabelProps) => (
  <>
    <span className="required-label__star">*</span> {children}
  </>
);

export default RequiredLabel;
