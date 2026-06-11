import { createContext } from 'react';

export interface LayoutContextType {
  isLargeViewport: boolean;
  isSideNavExpanded: boolean;
  toggleSideNav: () => void;
  isHeaderPanelOpen: boolean;
  toggleHeaderPanel: () => void;
  closeHeaderPanel: () => void;
}

export const LayoutContext = createContext<LayoutContextType | undefined>(undefined);
