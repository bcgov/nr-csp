import { useContext } from 'react';

import { PageTitleContext } from './PageTitleContext';

export function usePageTitle() {
  const ctx = useContext(PageTitleContext);
  if (!ctx) throw new Error('usePageTitle must be used within PageTitleProvider');
  return ctx;
}
