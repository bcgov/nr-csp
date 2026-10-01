import { Breadcrumb, BreadcrumbItem, Column } from '@carbon/react';
import { useEffect, useMemo, type FC, type ReactNode } from 'react';
import { useNavigate } from 'react-router';

import Subtitle from '@/components/core/Subtitle';
import UnderConstructionTag from '@/components/core/Tags/UnderConstructionTag';
import { usePageTitle } from '@/context/pageTitle/usePageTitle';

import { type BreadCrumbType } from './types';

import './index.scss';

/**
 * Props for the PageTitle component.
 *
 * @property {string} title - The main title text for the page.
 * @property {string} [subtitle] - Optional subtitle text for the page.
 * @property {boolean} [experimental] - If true, displays an "Under construction" tag.
 * @property {ReactNode} [children] - Optional elements to render next to the title.
 * @property {BreadCrumbType[]} [breadCrumbs] - Optional array of breadcrumb objects for navigation.
 */
interface PageTitleProps {
  title: string;
  subtitle?: string;
  experimental?: boolean;
  children?: ReactNode;
  breadCrumbs?: BreadCrumbType[];
}

/**
 * PageTitle provides a standardized header for pages, including a title, optional subtitle, breadcrumbs, and an experimental tag.
 * It helps maintain consistent page layouts and navigation.
 *
 * @param {PageTitleProps} props - The props for the component.
 * @returns {JSX.Element} The rendered PageTitle component.
 */
const PageTitle: FC<PageTitleProps> = ({ title, subtitle, experimental, children, breadCrumbs }: PageTitleProps) => {
  const navigate = useNavigate();
  const { setPageTitle } = usePageTitle();

  const breadcrumbTitle = useMemo(() => breadCrumbs?.map((crumb) => crumb.name).join(' - ') ?? '', [breadCrumbs]);

  useEffect(() => {
    setPageTitle(breadcrumbTitle || title, 2);
  }, [title, setPageTitle, breadcrumbTitle]);

  return (
    <Column className="page-title-col" sm={4} md={8} lg={16}>
      {breadCrumbs?.length ? (
        <Breadcrumb className="page-title-breadcrumb">
          {breadCrumbs.map((crumb, i) => {
            // The last crumb is the page you are already on: marked current, never a link.
            const isCurrentPage = i === breadCrumbs.length - 1;
            // '#' is used for crumbs that are labels rather than destinations (the Invoice screen
            // uses it for "Invoice" and the invoice number). Those get no link either.
            const isNavigable = !isCurrentPage && crumb.path !== '#';

            // A crumb only becomes an <a> when Carbon is given an `href` — without one it renders
            // a plain span. Every crumb used to be span + onClick: clickable with a mouse, but
            // carrying no link semantics, no keyboard focus and nothing for a screen reader to
            // announce as navigation. That mattered most on the Invoice screen, where the
            // breadcrumb is the ONLY way back to the search results.
            //
            // `href` makes it a real link (focusable, actionable by keyboard, announced, and
            // open-in-new-tab-able); the click handler still routes client-side so the SPA does not
            // do a full document load. Same pattern the in-page Links already use.
            //
            // The handler only hijacks a PLAIN left-click. Calling preventDefault unconditionally
            // would swallow Ctrl/Cmd+click and Shift+click — the browser's own
            // open-in-new-tab/window gestures — and route in place instead, which would make the
            // open-in-new-tab claim above false for exactly the users who rely on it. The guard is
            // the same one React Router's `Link` applies. (Right-click needs no guard: it fires no
            // click event, so "Open in new tab" from the context menu already worked.)
            return (
              <BreadcrumbItem
                key={crumb.name}
                isCurrentPage={isCurrentPage}
                {...(isNavigable
                  ? {
                      href: crumb.path,
                      onClick: (event: React.MouseEvent<HTMLAnchorElement>) => {
                        const isModifiedClick = event.metaKey || event.altKey || event.ctrlKey || event.shiftKey;
                        if (event.defaultPrevented || event.button !== 0 || isModifiedClick) return;
                        event.preventDefault();
                        // `void`: react-router 8's `navigate` returns a Promise, and nothing here
                        // needs to wait for or handle it — the crumb's only job is to start the
                        // transition. Marking it ignored is explicit about that rather than leaving
                        // a floating promise for a reader (or a linter) to wonder about.
                        void navigate(crumb.path);
                      },
                    }
                  : {})}
              >
                {crumb.name}
              </BreadcrumbItem>
            );
          })}
        </Breadcrumb>
      ) : null}
      <div className="page-title-container">
        <div className="title-container">
          <h1>{title}</h1>
          {children}
          {experimental ? <UnderConstructionTag type="page" /> : null}
        </div>
        {subtitle ? <Subtitle text={subtitle} /> : null}
      </div>
    </Column>
  );
};

export default PageTitle;
