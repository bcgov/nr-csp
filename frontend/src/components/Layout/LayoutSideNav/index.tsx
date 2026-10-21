import { SideNav, SideNavItems, SideNavLink } from '@carbon/react';
import { type FC } from 'react';
import { Link, useLocation } from 'react-router';

import { useAuth } from '@/context/auth/useAuth';
import { useLayout } from '@/context/layout/useLayout';
import { visibleNavItems } from '@/routes/access';
import { NAVIGATION_ITEMS } from '@/routes/navigation';

import './index.scss';

type NavLink = {
  name: string;
  path: string;
  icon?: React.ComponentType<{ size?: number | string }>;
};

type NavGroup = {
  name: string;
  icon?: React.ComponentType<{ size?: number | string }>;
  children: Array<NavLink | NavGroup>;
};

type NavItem = NavLink | NavGroup;

const isNavGroup = (item: NavItem): item is NavGroup => 'children' in item;

export const LayoutSideNav: FC = () => {
  const { isSideNavExpanded } = useLayout();
  const location = useLocation();
  const { user } = useAuth();

  const renderNavLink = (item: NavLink) => (
    <SideNavLink
      key={item.name}
      as={Link}
      to={item.path}
      isActive={item.path === location.pathname}
      renderIcon={item.icon}
    >
      {item.name}
    </SideNavLink>
  );

  const renderGroup = (group: NavGroup): React.ReactNode => (
    <div key={group.name} className="side-nav-group">
      <p className="side-nav-group__title">{group.name}</p>
      {group.children.map((child) => (isNavGroup(child) ? renderGroup(child) : renderNavLink(child)))}
    </div>
  );

  return (
    <SideNav expanded={isSideNavExpanded} isPersistent={isSideNavExpanded} isChildOfHeader>
      <SideNavItems>
        {visibleNavItems(NAVIGATION_ITEMS as NavItem[], user).map((item) =>
          isNavGroup(item) ? renderGroup(item) : renderNavLink(item),
        )}
      </SideNavItems>
    </SideNav>
  );
};
