import { useCallback, useRef, type FC, type ReactNode } from 'react';

import { PageTitleContext } from './PageTitleContext';

const PageTitleProvider: FC<{ children: ReactNode }> = ({ children }) => {
  const currentPriority = useRef<number>(0);

  const setPageTitle = useCallback((title: string, priority = 1) => {
    if (priority >= currentPriority.current) {
      document.title = title;
      currentPriority.current = priority;
    }
  }, []);

  return <PageTitleContext.Provider value={{ setPageTitle }}>{children}</PageTitleContext.Provider>;
};

export default PageTitleProvider;
