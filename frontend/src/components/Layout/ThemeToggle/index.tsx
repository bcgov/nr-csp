import { AsleepFilled, LightFilled } from '@carbon/icons-react';
import { type FC } from 'react';

import { useTheme } from '@/context/theme/useTheme';

import './index.scss';

export const ThemeToggle: FC = () => {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'g100';

  return (
    <div
      className={`theme-toggle ${isDark ? 'on' : 'off'}`}
      onClick={toggleTheme}
      role="button"
      tabIndex={0}
      aria-label={isDark ? 'Switch to light theme' : 'Switch to dark theme'}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') toggleTheme();
      }}
    >
      <div className="circle">{isDark ? <AsleepFilled className="icon" /> : <LightFilled className="icon" />}</div>
    </div>
  );
};
