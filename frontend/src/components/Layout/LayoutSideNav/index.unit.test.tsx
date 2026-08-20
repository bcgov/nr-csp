import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useAuth } from '@/context/auth/useAuth';
import { useLayout } from '@/context/layout/useLayout';
import { ROUTES } from '@/routes/routePaths';

import { LayoutSideNav } from './index';

vi.mock('@/context/layout/useLayout', () => ({ useLayout: vi.fn() }));
vi.mock('@/context/auth/useAuth', () => ({ useAuth: vi.fn() }));

describe('LayoutSideNav', () => {
  const arrange = (initialPath = '/', idpProvider: 'IDIR' | 'BCEID' = 'IDIR') => {
    vi.mocked(useLayout).mockReturnValue({ isSideNavExpanded: true, toggleSideNav: vi.fn() } as unknown as ReturnType<
      typeof useLayout
    >);
    vi.mocked(useAuth).mockReturnValue({ user: { idpProvider } } as unknown as ReturnType<typeof useAuth>);
    return render(
      <MemoryRouter initialEntries={[initialPath]}>
        <LayoutSideNav />
      </MemoryRouter>,
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the top-level navigation links with their routes', () => {
    arrange();

    // Exact names: report links like "Invoice Print Out" also contain these words.
    expect(screen.getByRole('link', { name: 'Search' })).toHaveAttribute('href', ROUTES.SEARCH);
    expect(screen.getByRole('link', { name: 'Inbox' })).toHaveAttribute('href', ROUTES.INBOX);
    expect(screen.getByRole('link', { name: 'Invoice' })).toHaveAttribute('href', ROUTES.INVOICE);
  });

  it('renders grouped links under their group title', () => {
    arrange();

    expect(screen.getByText('Submissions')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /submission history/i })).toHaveAttribute(
      'href',
      ROUTES.SUBMISSION_HISTORY,
    );
  });

  it('marks the link matching the current location as the current page', () => {
    arrange(ROUTES.SEARCH);

    // Carbon's SideNavLink signals the active item via its --current modifier class.
    expect(screen.getByRole('link', { name: 'Search' })).toHaveClass('cds--side-nav__link--current');
    expect(screen.getByRole('link', { name: 'Inbox' })).not.toHaveClass('cds--side-nav__link--current');
  });

  it('shows only the Submissions group and its two pages for a BCeID user', () => {
    arrange('/', 'BCEID');

    expect(screen.getByText('Submissions')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Upload Submission' })).toHaveAttribute('href', ROUTES.UPLOAD_SUBMISSION);
    expect(screen.getByRole('link', { name: /submission history/i })).toHaveAttribute(
      'href',
      ROUTES.SUBMISSION_HISTORY,
    );
    expect(screen.queryByRole('link', { name: 'Search' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Inbox' })).not.toBeInTheDocument();
    expect(screen.queryByText('Table maintenance')).not.toBeInTheDocument();
    expect(screen.queryByText('Reports')).not.toBeInTheDocument();
  });
});
