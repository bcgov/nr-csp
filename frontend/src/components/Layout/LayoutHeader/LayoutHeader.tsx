import { Header, HeaderGlobalBar, HeaderMenuButton, HeaderName, SkipToContent } from '@carbon/react';
import { type FC } from 'react';
import { Link } from 'react-router';

import { LayoutHeaderPanel } from '@/components/Layout/LayoutHeaderPanel';
import { LayoutSideNav } from '@/components/Layout/LayoutSideNav';
import { ThemeToggle } from '@/components/Layout/ThemeToggle';
import { useAuth } from '@/context/auth/useAuth';
import { useLayout } from '@/context/layout/useLayout';
import { homeRouteFor } from '@/routes/access';
import { ROUTES } from '@/routes/routePaths';

import LayoutHeaderGlobalBar from './LayoutHeaderGlobalBar';

import './index.scss';

const APP_NAME = 'Coast Selling Price System';

export const LayoutHeader: FC = () => {
  const { isSideNavExpanded, toggleSideNav } = useLayout();
  const { isAuthenticated, user } = useAuth();

  return (
    <Header aria-label={APP_NAME} className="bc-header">
      <SkipToContent />
      {isAuthenticated && (
        <HeaderMenuButton
          id="navigation-toggle"
          aria-controls="side-navigation"
          aria-expanded={isSideNavExpanded}
          aria-label={isSideNavExpanded ? 'Close menu' : 'Open menu'}
          isActive={isSideNavExpanded}
          isCollapsible
          onClick={toggleSideNav}
        />
      )}
      <HeaderName
        as={Link}
        to={isAuthenticated ? homeRouteFor(user) : ROUTES.LANDING}
        prefix="CSP"
        aria-label={`CSP ${APP_NAME}`}
      >
        {APP_NAME}
      </HeaderName>
      <HeaderGlobalBar>
        <ThemeToggle />
        <LayoutHeaderGlobalBar />
      </HeaderGlobalBar>
      <LayoutHeaderPanel />
      {isAuthenticated && <LayoutSideNav />}
    </Header>
  );
};
