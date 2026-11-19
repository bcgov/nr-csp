import { useEffect, useState, type ReactNode } from 'react';

import { LayoutContext } from './LayoutContext';

export function LayoutProvider({ children }: { children: ReactNode }) {
  // Carbon switches from a persistent sidebar to an overlay at its lg breakpoint.
  const [desktopViewport] = useState(() => window.matchMedia('(min-width: 66rem)'));
  const [sideNavExpanded, setSideNavExpanded] = useState(desktopViewport.matches);
  const [headerPanelOpen, setHeaderPanelOpen] = useState(false);

  useEffect(() => {
    const onViewportChange = (event: MediaQueryListEvent) => setSideNavExpanded(event.matches);
    desktopViewport.addEventListener('change', onViewportChange);
    return () => desktopViewport.removeEventListener('change', onViewportChange);
  }, [desktopViewport]);

  return (
    <LayoutContext.Provider
      value={{
        isSideNavExpanded: sideNavExpanded,
        toggleSideNav: () => setSideNavExpanded((prev) => !prev),
        closeMobileSideNav: () => {
          if (!desktopViewport.matches) setSideNavExpanded(false);
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
