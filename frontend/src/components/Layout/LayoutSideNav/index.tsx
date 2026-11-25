import { Help } from '@carbon/icons-react';
import { SideNav, SideNavItems, SideNavLink, SideNavMenu, SideNavMenuItem } from '@carbon/react';
import { Link, matchPath, useLocation } from 'react-router';

import { useAuth } from '@/context/auth/useAuth';
import { useLayout } from '@/context/layout/useLayout';
import { visibleNavItems } from '@/routes/access';
import { NAVIGATION_ITEMS } from '@/routes/navigation';
import { isModifiedClick } from '@/utils/link';
import { userManualsForRoles } from '@/utils/support';

import type { MouseEvent } from 'react';

import './index.scss';

export function LayoutSideNav() {
  const { isSideNavExpanded, toggleSideNav, closeMobileSideNav } = useLayout();
  const { pathname } = useLocation();
  const { user } = useAuth();
  const isCurrent = (path: string) => Boolean(matchPath({ path, end: false }, pathname));
  const userManuals = userManualsForRoles(user?.privileges);

  const onNavigate = (event: MouseEvent<HTMLAnchorElement>) => {
    if (!isModifiedClick(event)) closeMobileSideNav();
  };

  const dismissMenu = () => {
    toggleSideNav();
    document.getElementById('navigation-toggle')?.focus();
  };

  return (
    <SideNav
      id="side-navigation"
      aria-label="Side navigation"
      expanded={isSideNavExpanded}
      hidden={!isSideNavExpanded}
      isPersistent={isSideNavExpanded}
      onOverlayClick={dismissMenu}
      onKeyDown={(event) => {
        if (event.key === 'Escape' && !event.defaultPrevented) dismissMenu();
      }}
    >
      <SideNavItems>
        {visibleNavItems(NAVIGATION_ITEMS, user).map((item) => {
          if (item.children) {
            const containsCurrentPage = item.children.some((child) => isCurrent(child.path));
            return (
              <SideNavMenu
                key={`${item.name}-${containsCurrentPage}`}
                title={item.name}
                renderIcon={item.icon}
                defaultExpanded={containsCurrentPage}
                onKeyDown={(event) => {
                  if (event.key !== 'Escape') return;
                  const trigger = event.currentTarget.querySelector<HTMLButtonElement>('button[aria-expanded="true"]');
                  if (trigger) {
                    event.stopPropagation();
                    trigger.focus();
                  }
                }}
              >
                {item.children.map((child) => (
                  <SideNavMenuItem
                    key={child.path}
                    as={Link}
                    to={child.path}
                    aria-current={isCurrent(child.path) ? 'page' : undefined}
                    onClick={onNavigate}
                  >
                    {child.name}
                  </SideNavMenuItem>
                ))}
              </SideNavMenu>
            );
          }
          return (
            <SideNavLink
              key={item.path}
              as={Link}
              to={item.path}
              aria-current={isCurrent(item.path) ? 'page' : undefined}
              renderIcon={item.icon}
              onClick={onNavigate}
            >
              {item.name}
            </SideNavLink>
          );
        })}
        {userManuals.length > 0 && (
          <SideNavMenu title="Support" renderIcon={Help}>
            {userManuals.map((userManual) => (
              <SideNavMenuItem key={userManual.href} href={userManual.href} download>
                {userManual.label}
              </SideNavMenuItem>
            ))}
          </SideNavMenu>
        )}
      </SideNavItems>
    </SideNav>
  );
}
