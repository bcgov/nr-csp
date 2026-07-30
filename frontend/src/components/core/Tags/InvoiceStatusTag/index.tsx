import { Tag } from '@carbon/react';
import { type FC } from 'react';

import './index.scss';

export type InvoiceStatusTagProps = {
  status: string;
};

// Maps a status code or its backend description to the pill colour modifier.
const STATUS_KEY_MAP: Record<string, string> = {
  // By code
  PRO: 'processing',
  UNA: 'unapproved',
  APP: 'approved',
  CAN: 'cancelled',
  DFT: 'draft',
  DVF: 'deverified',
  REJ: 'rejected',
  VER: 'verified',
  // By description (returned by backend)
  Processing: 'processing',
  Unapproved: 'unapproved',
  Approved: 'approved',
  Cancelled: 'cancelled',
  Draft: 'draft',
  Deverified: 'deverified',
  Rejected: 'rejected',
  Verified: 'verified',
};

/**
 * Displays an invoice status as a coloured Carbon Tag pill.
 * Accepts either the status code (e.g. "APP") or its description (e.g. "Approved").
 * Falls back to a neutral gray tag for any unrecognised status.
 *
 * @param {InvoiceStatusTagProps} props - Component props.
 * @param {string} props.status - The status code or description to display.
 * @returns {JSX.Element} A coloured pill representing the invoice status.
 */
const InvoiceStatusTag: FC<InvoiceStatusTagProps> = ({ status }) => {
  const key = STATUS_KEY_MAP[status];
  return (
    <Tag type="gray" size="sm" className={`invoice-status-tag${key ? ` invoice-status-tag--${key}` : ''}`}>
      {status}
    </Tag>
  );
};

export default InvoiceStatusTag;
