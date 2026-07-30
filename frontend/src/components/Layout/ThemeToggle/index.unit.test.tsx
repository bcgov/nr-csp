import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { Theme } from '@/context/theme/ThemeContext';
import { useTheme } from '@/context/theme/useTheme';

import { ThemeToggle } from './index';

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

  it('offers to switch to the dark theme while the light theme is active', () => {
    arrange('g10');
    const toggle = screen.getByRole('button', { name: 'Switch to dark theme' });
    expect(toggle).toHaveClass('off');
    expect(toggle).not.toHaveClass('on');
  });

  it('offers to switch to the light theme while the dark theme is active', () => {
    arrange('g100');
    const toggle = screen.getByRole('button', { name: 'Switch to light theme' });
    expect(toggle).toHaveClass('on');
  });

  it('toggles the theme on click', () => {
    arrange();
    fireEvent.click(screen.getByRole('button'));
    expect(toggleTheme).toHaveBeenCalledTimes(1);
  });

  it('toggles the theme on Enter key', () => {
    arrange();
    fireEvent.keyDown(screen.getByRole('button'), { key: 'Enter' });
    expect(toggleTheme).toHaveBeenCalledTimes(1);
  });

  it('toggles the theme on Space key', () => {
    arrange();
    fireEvent.keyDown(screen.getByRole('button'), { key: ' ' });
    expect(toggleTheme).toHaveBeenCalledTimes(1);
  });

  it('ignores other keys', () => {
    arrange();
    fireEvent.keyDown(screen.getByRole('button'), { key: 'Escape' });
    expect(toggleTheme).not.toHaveBeenCalled();
  });
});
