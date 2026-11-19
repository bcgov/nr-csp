import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useTheme } from '@/context/theme/useTheme';

import { ThemeToggle } from './index';

import type { Theme } from '@/context/theme/ThemeContext';

vi.mock('@/context/theme/useTheme', () => ({ useTheme: vi.fn() }));

describe('ThemeToggle', () => {
  const toggleTheme = vi.fn();

  const arrange = (theme: Theme = 'g10') => {
    vi.mocked(useTheme).mockReturnValue({ theme, toggleTheme });
    return render(<ThemeToggle />);
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it.each([
    ['g10', false],
    ['g100', true],
  ] as const)('reports the dark theme switch state for %s', (theme, checked) => {
    arrange(theme);
    expect(screen.getByRole('switch', { name: 'Dark theme', checked })).toBeInTheDocument();
  });

  it('toggles the theme once per click', () => {
    arrange();
    fireEvent.click(screen.getByRole('switch'));
    expect(toggleTheme).toHaveBeenCalledTimes(1);
  });
});
