import { useState, type ReactNode } from 'react';

import { LayoutContext } from './LayoutContext';

export function LayoutProvider({ children }: { children: ReactNode }) {
  const [sideNavExpanded, setSideNavExpanded] = useState(true);
  const [headerPanelOpen, setHeaderPanelOpen] = useState(false);

  return (
    <LayoutContext.Provider
      value={{
        isSideNavExpanded: sideNavExpanded,
        toggleSideNav: () => setSideNavExpanded((prev) => !prev),
        isHeaderPanelOpen: headerPanelOpen,
        toggleHeaderPanel: () => setHeaderPanelOpen((prev) => !prev),
        closeHeaderPanel: () => setHeaderPanelOpen(false),
      }}
    >
      {children}
    </LayoutContext.Provider>
  );
}
