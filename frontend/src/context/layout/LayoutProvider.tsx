import { useEffect, useState, type ReactNode } from 'react';

import { LayoutContext } from './LayoutContext';

const LG_BREAKPOINT = '(min-width: 66rem)';

function isLargeViewport() {
  return typeof window !== 'undefined' && window.matchMedia?.(LG_BREAKPOINT).matches;
}

export function LayoutProvider({ children }: { children: ReactNode }) {
  const [largeViewport, setLargeViewport] = useState(isLargeViewport);
  const [sideNavExpanded, setSideNavExpanded] = useState(isLargeViewport);
  const [headerPanelOpen, setHeaderPanelOpen] = useState(false);

  useEffect(() => {
    const mql = window.matchMedia?.(LG_BREAKPOINT);
    if (!mql) return;

    const handler = (e: MediaQueryListEvent) => {
      setLargeViewport(e.matches);
      setSideNavExpanded(e.matches);
    };

    mql.addEventListener('change', handler);
    return () => mql.removeEventListener('change', handler);
  }, []);

  return (
    <LayoutContext.Provider
      value={{
        isLargeViewport: largeViewport,
        isSideNavExpanded: sideNavExpanded,
        toggleSideNav: () => {
          if (largeViewport) return;
          setSideNavExpanded((prev) => !prev);
        },
        isHeaderPanelOpen: headerPanelOpen,
        toggleHeaderPanel: () => setHeaderPanelOpen((prev) => !prev),
        closeHeaderPanel: () => setHeaderPanelOpen(false),
      }}
    >
      {children}
    </LayoutContext.Provider>
  );
}
