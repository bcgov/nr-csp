// src/components/PageTitle/PageTitle.browser.test.tsx

import { render, screen } from '@testing-library/react';
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
