import { CheckmarkFilled, ErrorFilled, WarningAltFilled } from '@carbon/icons-react';
import { useId, type FC } from 'react';

import { type CellIssue } from '@/components/core/DataPreviewTable';

import { invoiceSeverity, type InvoiceIssue } from './submissionErrors';

/** "n thing" / "n things". */
const count = (n: number, noun: string): string => `${n} ${noun}${n === 1 ? '' : 's'}`;

/**
 * A single error/warning icon shown beside a field that has validation issues,
 * with the message(s) in a native hover tooltip. ERROR takes icon precedence.
 * Used to mark individual fields in the expanded "Invoice details" card.
 */
export const FieldIssueMarker: FC<{ issues: CellIssue[] }> = ({ issues }) => {
  if (issues.length === 0) return null;
  const hasError = issues.some((i) => i.type === 'ERROR');
  const Icon = hasError ? ErrorFilled : WarningAltFilled;
  return (
    <span
      className={`invoice-field-issue invoice-field-issue--${hasError ? 'error' : 'warning'}`}
      title={issues.map((i) => i.message).join('\n')}
    >
      <Icon size={16} />
    </span>
  );
};

/**
 * Compact per-invoice severity badge shown on the invoice row: red "N errors" /
 * amber "N warnings" (both when a mix), or a muted "No issues" check when clean.
 * Summarises the invoice's own issues plus all of its line items so the row
 * signals its state at a glance, even while collapsed.
 */
export const InvoiceIssueBadge: FC<{ issues: InvoiceIssue[] }> = ({ issues }) => {
  const severity = invoiceSeverity(issues);

  if (severity === 'none') {
    return (
      <span className="invoice-issue-badge invoice-issue-badge--none">
        <CheckmarkFilled size={16} />
        No issues
      </span>
    );
  }

  const errors = issues.filter((i) => i.type === 'ERROR').length;
  const warnings = issues.length - errors;

  // Errors and warnings each render as their own icon+text segment so the
  // warning count keeps its amber colour and warning icon even alongside errors.
  return (
    <span className="invoice-issue-badge">
      {errors > 0 && (
        <span className="invoice-issue-badge__segment invoice-issue-badge__segment--error">
          <ErrorFilled size={16} />
          {count(errors, 'error')}
        </span>
      )}
      {warnings > 0 && (
        <span className="invoice-issue-badge__segment invoice-issue-badge__segment--warning">
          <WarningAltFilled size={16} />
          {count(warnings, 'warning')}
        </span>
      )}
    </span>
  );
};

/**
 * Readable, local list of an invoice's issues, rendered at the top of its
 * expanded panel (above the line-items table) so the messages sit right next to
 * the data they concern. Renders nothing when the invoice is clean.
 */
export const InvoiceIssueList: FC<{ invoiceNumber: string; issues: InvoiceIssue[] }> = ({ invoiceNumber, issues }) => {
  // Stable, unique id so the list can be named by its visible title via
  // aria-labelledby (multiple invoices render this list on the same page).
  const titleId = useId();
  if (issues.length === 0) return null;

  return (
    <div className="invoice-issue-list">
      <p className="invoice-issue-list__title" id={titleId}>
        Issues for {invoiceNumber}
      </p>
      <ul className="invoice-issue-list__items" aria-labelledby={titleId}>
        {issues.map((issue, i) => {
          const Icon = issue.type === 'ERROR' ? ErrorFilled : WarningAltFilled;
          return (
            <li
              key={`${issue.type}-${issue.lineIndex ?? 'inv'}-${i}`}
              className={`invoice-issue-list__item invoice-issue-list__item--${issue.type.toLowerCase()}`}
            >
              <Icon size={16} className="invoice-issue-list__icon" />
              <span>
                {issue.lineIndex != null ? <strong>Line {issue.lineIndex} · </strong> : null}
                {issue.message}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
};
