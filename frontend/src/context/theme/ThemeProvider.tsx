import { Theme as CarbonTheme } from '@carbon/react';
import { useEffect, useState, type ReactNode } from 'react';

import { type Theme, ThemeContext } from './ThemeContext';

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>('g10');

  useEffect(() => {
    document.documentElement.dataset.carbonTheme = theme;
  }, [theme]);

  const toggleTheme = () => setTheme((t) => (t === 'g10' ? 'g100' : 'g10'));

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      <CarbonTheme theme={theme}>{children}</CarbonTheme>
    </ThemeContext.Provider>
  );
}
