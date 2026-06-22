import {
  Header,
  HeaderGlobalAction,
  HeaderGlobalBar,
  HeaderMenuButton,
  HeaderName,
  SkipToContent,
} from '@carbon/react';
import { type FC } from 'react';
import { Link } from 'react-router-dom';

import { LayoutHeaderPanel } from '@/components/Layout/LayoutHeaderPanel';
import { LayoutSideNav } from '@/components/Layout/LayoutSideNav';
import { ThemeToggle } from '@/components/Layout/ThemeToggle';
import { useAuth } from '@/context/auth/useAuth';
import { useLayout } from '@/context/layout/useLayout';

import LayoutHeaderGlobalBar from './LayoutHeaderGlobalBar';

import './index.scss';

const APP_NAME = 'Coast Selling Application (CSP)';

export const LayoutHeader: FC = () => {
  const { isSideNavExpanded, toggleSideNav } = useLayout();
  const { isAuthenticated } = useAuth();

  return (
    <Header aria-label={APP_NAME} className="bc-header">
      <SkipToContent />
      {isAuthenticated && (
        <HeaderMenuButton
          aria-label={isSideNavExpanded ? 'Close menu' : 'Open menu'}
          isActive={isSideNavExpanded}
          isCollapsible
          onClick={toggleSideNav}
        />
      )}
      <HeaderName as={Link} to="/" prefix="">
        {APP_NAME}
      </HeaderName>
      <HeaderGlobalBar>
        <LayoutHeaderGlobalBar />
        <HeaderGlobalAction aria-label="Theme" tooltipAlignment="end" className="theme-toggle-action">
          <ThemeToggle />
        </HeaderGlobalAction>
      </HeaderGlobalBar>
      <LayoutHeaderPanel />
      {isAuthenticated && <LayoutSideNav />}
    </Header>
  );
};
