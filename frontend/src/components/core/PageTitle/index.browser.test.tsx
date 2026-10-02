// src/components/PageTitle/PageTitle.browser.test.tsx

import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { describe, it, expect } from 'vitest';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';

import PageTitle from './index';

// Helper function to render PageTitle with props
const renderPageTitle = (props: React.ComponentProps<typeof PageTitle> & { children?: React.ReactNode }) => {
  render(
    <MemoryRouter>
      <PageTitleProvider>
        <PageTitle {...props}>{props.children}</PageTitle>
      </PageTitleProvider>
    </MemoryRouter>,
  );
};

describe('PageTitle (browser)', () => {
  it('renders title and subtitle', () => {
    renderPageTitle({ title: 'Test Title', subtitle: 'Test Subtitle' });

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Test Title');
    expect(screen.getByText('Test Subtitle')).toBeInTheDocument();
  });

  it('renders breadcrumbs', async () => {
    const user = userEvent.setup();

    const breadCrumbs = [
      { name: 'Home', path: '/' },
      { name: 'Dashboard', path: '/dashboard' },
    ];

    renderPageTitle({ title: 'With Breadcrumbs', breadCrumbs });

    const dashboardCrumb = screen.getByText('Dashboard');
    expect(dashboardCrumb).toBeInTheDocument();

    // We're not mocking navigation here — just verifying it doesn't crash
    await user.click(dashboardCrumb);
  });

  // Breadcrumbs used to be spans with an onClick: clickable by mouse, but with no link semantics,
  // no keyboard focus and nothing for a screen reader to announce as navigation. On the Invoice
  // screen the breadcrumb is the only way back to the search results, so that was the sole route
  // out — and it was unreachable without a mouse.
  it('renders a navigable crumb as a real, keyboard-focusable link', () => {
    renderPageTitle({
      title: 'With Breadcrumbs',
      breadCrumbs: [
        { name: 'Invoice search', path: '/search' },
        { name: 'Invoice', path: '#' },
      ],
    });

    const link = screen.getByRole('link', { name: 'Invoice search' });
    expect(link).toHaveAttribute('href', '/search');
    // Anchors with an href are in the tab order natively — no tabindex juggling required.
    link.focus();
    expect(link).toHaveFocus();
  });

  // A plain click must route client-side (no document load), but Ctrl/Cmd+click and Shift+click are
  // the browser's own open-in-new-tab/window gestures. Calling preventDefault unconditionally
  // swallowed them and routed in place instead, which silently broke opening a crumb in a new tab —
  // the thing the href was added for. Asserting on defaultPrevented is what distinguishes the two:
  // it is the single bit that decides whether the browser still gets to act on the anchor.
  it('lets a modified click fall through to the browser instead of routing', () => {
    renderPageTitle({
      title: 'With Breadcrumbs',
      breadCrumbs: [
        { name: 'Invoice search', path: '/search' },
        { name: 'Invoice', path: '#' },
      ],
    });

    const link = screen.getByRole('link', { name: 'Invoice search' });

    // `defaultPrevented` is the single bit that decides whether the browser still gets to act on
    // the anchor, so it is what distinguishes "fell through to the browser" from "routed in place".
    //
    // ⚠ This listener is REQUIRED, not incidental. Falling through is the behaviour under test, so
    // on a modified click nothing calls preventDefault — and the browser then really does follow
    // href="/search", navigating the Vitest browser runner's own iframe away and killing the whole
    // file with "Cannot connect to the iframe". Registering on `document` means this runs in the
    // bubble phase, AFTER the component's handler: it reads whether the component prevented the
    // event, then prevents it so the real navigation never happens.
    const prevented: boolean[] = [];
    const recordAndBlockNavigation = (event: Event) => {
      prevented.push(event.defaultPrevented);
      event.preventDefault();
    };
    document.addEventListener('click', recordAndBlockNavigation);
    try {
      // act(): the plain click routes via `navigate`, which updates router state.
      for (const modifiers of [{ ctrlKey: true }, { metaKey: true }, { shiftKey: true }, {}]) {
        act(() => {
          link.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, ...modifiers }));
        });
      }
    } finally {
      document.removeEventListener('click', recordAndBlockNavigation);
    }

    // Ctrl, Meta and Shift are left for the browser; the plain left click is taken by the router.
    expect(prevented).toEqual([false, false, false, true]);
  });

  it('marks the last crumb as the current page and does not link it', () => {
    renderPageTitle({
      title: 'With Breadcrumbs',
      breadCrumbs: [
        { name: 'Reports', path: '/reports/r06-invoice-print-out' },
        { name: 'R13 — Ad hoc', path: '/reports/r13-ad-hoc' },
      ],
    });

    expect(screen.getByRole('link', { name: 'Reports' })).toBeInTheDocument();
    // You are already here, so it is announced as current rather than offered as a destination.
    expect(screen.queryByRole('link', { name: 'R13 — Ad hoc' })).not.toBeInTheDocument();
    expect(screen.getByText('R13 — Ad hoc').closest('li')).toHaveClass('cds--breadcrumb-item--current');
  });

  it('does not link a placeholder crumb whose path is "#"', () => {
    renderPageTitle({
      title: 'With Breadcrumbs',
      breadCrumbs: [
        { name: 'Invoice search', path: '/search' },
        { name: 'Invoice', path: '#' },
        { name: 'INV-001', path: '#' },
      ],
    });

    // "Invoice" is a label, not a destination — it must not advertise itself as one.
    expect(screen.queryByRole('link', { name: 'Invoice' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'INV-001' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Invoice search' })).toBeInTheDocument();
  });

  it('renders the experimental tag when experimental is true', () => {
    renderPageTitle({ title: 'Experimental Page', experimental: true });
    // UnderConstructionTag renders a tag with text 'Under Construction' by default
    expect(screen.getByText(/under construction/i)).toBeInTheDocument();
  });

  it('renders children components', () => {
    renderPageTitle({
      title: 'With Children',
      children: <span data-testid="custom-child">Child Content</span>,
    });
    expect(screen.getByTestId('custom-child')).toHaveTextContent('Child Content');
  });
});
