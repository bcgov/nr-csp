import { SideNav, SideNavItems, SideNavLink } from '@carbon/react';
import { type FC } from 'react';
import { Link, useLocation } from 'react-router';

import { useAuth } from '@/context/auth/useAuth';
import { useLayout } from '@/context/layout/useLayout';
import { NAVIGATION_ITEMS } from '@/routes/navigation';

import './index.scss';

type NavLink = {
  name: string;
  path: string;
  icon?: React.ComponentType<{ size?: number | string }>;
  /** Set on the small set of nav entries BCeID users are permitted to see. */
  bceidAllowed?: true;
};

type NavGroup = {
  name: string;
  icon?: React.ComponentType<{ size?: number | string }>;
  children: Array<NavLink | NavGroup>;
};

type NavItem = NavLink | NavGroup;

const isNavGroup = (item: NavItem): item is NavGroup => 'children' in item;

/**
 * Recursively drops nav entries BCeID users aren't allowed to see, and any
 * group left with no visible children. IDIR users see everything unchanged.
 */
function filterForIdp(items: NavItem[], isBceid: boolean): NavItem[] {
  if (!isBceid) return items;

  return items.reduce<NavItem[]>((visible, item) => {
    if (isNavGroup(item)) {
      const children = filterForIdp(item.children, isBceid);
      if (children.length > 0) visible.push({ ...item, children });
    } else if (item.bceidAllowed) {
      visible.push(item);
    }
    return visible;
  }, []);
}

export const LayoutSideNav: FC = () => {
  const { isSideNavExpanded } = useLayout();
  const location = useLocation();
  const { user } = useAuth();
  const visibleItems = filterForIdp(NAVIGATION_ITEMS as NavItem[], user?.idpProvider === 'BCEID');

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
        {visibleItems.map((item) => (isNavGroup(item) ? renderGroup(item) : renderNavLink(item)))}
      </SideNavItems>
    </SideNav>
  );
};
