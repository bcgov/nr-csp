import { type FC, type ReactNode } from 'react';

import './index.scss';

/** A single label / value pair rendered within a {@link DetailSection}. */
export interface DetailItem {
  label: string;
  value: ReactNode;
  /** When true, the item spans every grid column instead of a single cell —
   * useful for long free-text values (notes, descriptions) that should run the
   * full width rather than wrap inside one narrow column. */
  fullWidth?: boolean;
  /** Error text shown beneath the value; also marks the item invalid. */
  errorText?: string;
  /** Warning text shown beneath the value. */
  warningText?: string;
}

interface DetailSectionProps {
  /** Optional section heading rendered above the grid. */
  title?: string;
  /** Label/value pairs displayed as a responsive grid. */
  items: DetailItem[];
  /** Optional extra content rendered beneath the grid (e.g. a table). */
  children?: ReactNode;
  /** Optional additional class names. */
  className?: string;
}

/**
 * DetailSection renders a titled, read-only block of label/value pairs — the
 * standard layout for the header of a detail/view page. Values are rendered
 * as-is, so callers can pass formatted strings, tags, or links. An em-dash is
 * shown for null/undefined/empty values so the grid stays aligned.
 *
 * @param {DetailSectionProps} props - The component props.
 * @returns {JSX.Element} The rendered detail section.
 */
const DetailSection: FC<DetailSectionProps> = ({ title, items, children, className }) => (
  <section className={className ? `${className} detail-section` : 'detail-section'}>
    {title ? <h2 className="detail-section__title">{title}</h2> : null}
    <dl className="detail-section__grid">
      {items.map((item) => {
        const classes = ['detail-section__item'];
        if (item.fullWidth) classes.push('detail-section__item--full');
        if (item.errorText) classes.push('detail-section__item--error');
        return (
          <div key={item.label} className={classes.join(' ')}>
            <dt className="detail-section__label">{item.label}</dt>
            <dd className="detail-section__value">
              {item.value === null || item.value === undefined || item.value === '' ? '—' : item.value}
            </dd>
            {item.errorText ? <p className="detail-section__error">{item.errorText}</p> : null}
            {item.warningText ? <p className="detail-section__warning">{item.warningText}</p> : null}
          </div>
        );
      })}
    </dl>
    {children}
  </section>
);

export default DetailSection;
