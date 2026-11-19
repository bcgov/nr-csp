import { AsleepFilled, LightFilled } from '@carbon/icons-react';
import { type FC } from 'react';

import { useTheme } from '@/context/theme/useTheme';

import './index.scss';

export const ThemeToggle: FC = () => {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'g100';

  return (
    <button
      type="button"
      className="theme-toggle"
      role="switch"
      aria-label="Dark theme"
      aria-checked={isDark}
      onClick={toggleTheme}
    >
      <span className="theme-toggle__thumb">{isDark ? <AsleepFilled /> : <LightFilled />}</span>
    </button>
  );
};
