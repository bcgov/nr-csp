import { Tag } from '@carbon/react';
import { type FC } from 'react';

import './index.scss';

export type InvoiceDetailsTagProps = {
  label: string;
};

/**
 * Displays an invoice-details indicator (e.g. "New") as a green Carbon Tag
 *
 * @param {InvoiceDetailsTagProps} props - Component props.
 * @param {string} props.label - Text to display inside the tag.
 * @returns {JSX.Element} A green tag rendering `label`.
 */
const InvoiceDetailsTag: FC<InvoiceDetailsTagProps> = ({ label }) => (
  <Tag type="green" size="sm" className="invoice-details-tag">
    {label}
  </Tag>
);

export default InvoiceDetailsTag;
