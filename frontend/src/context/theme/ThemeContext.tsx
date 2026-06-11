import { createContext } from 'react';

export type Theme = 'g10' | 'g100';

export interface ThemeContextValue {
  theme: Theme;
  toggleTheme: () => void;
}

export const ThemeContext = createContext<ThemeContextValue>({
  theme: 'g10',
  toggleTheme: () => {},
});
