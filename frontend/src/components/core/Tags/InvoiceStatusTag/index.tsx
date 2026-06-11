import { Tag } from '@carbon/react';
import { type FC } from 'react';

import './index.scss';

export type InvoiceStatusTagProps = {
  status: string;
};

const APPROVED_STATUSES = new Set(['APP', 'Approved']);

type TagType =
  | 'red'
  | 'magenta'
  | 'purple'
  | 'blue'
  | 'cyan'
  | 'teal'
  | 'green'
  | 'gray'
  | 'cool-gray'
  | 'warm-gray'
  | 'high-contrast'
  | 'outline';

const STATUS_COLOUR_MAP: Record<string, TagType> = {
  // By code
  APP: 'green',
  VER: 'teal',
  PRO: 'blue',
  DFT: 'gray',
  UNA: 'cool-gray',
  CAN: 'warm-gray',
  REJ: 'red',
  DVF: 'purple',
  // By description (returned by backend)
  Approved: 'green',
  Verified: 'teal',
  Processing: 'blue',
  Draft: 'gray',
  Unapproved: 'cool-gray',
  Cancelled: 'warm-gray',
  Rejected: 'red',
  Deverified: 'purple',
};

/**
 * Displays an invoice status as a coloured Carbon Tag.
 * Accepts either the status code (e.g. "APP") or its description (e.g. "Approved").
 * Falls back to a neutral gray tag for any unrecognised status.
 *
 * @param {InvoiceStatusTagProps} props - Component props.
 * @param {string} props.status - The status code or description to display.
 * @returns {JSX.Element} A coloured tag representing the invoice status.
 */
const InvoiceStatusTag: FC<InvoiceStatusTagProps> = ({ status }) => (
  <Tag
    type={STATUS_COLOUR_MAP[status] ?? 'gray'}
    size="sm"
    className={`invoice-status-tag${APPROVED_STATUSES.has(status) ? ' invoice-status-tag--approved' : ''}`}
  >
    {status}
  </Tag>
);

export default InvoiceStatusTag;
