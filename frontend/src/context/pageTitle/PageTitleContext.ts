import { createContext } from 'react';

export interface PageTitleContextValue {
  setPageTitle: (title: string, priority?: number) => void;
}

export const PageTitleContext = createContext<PageTitleContextValue | null>(null);
