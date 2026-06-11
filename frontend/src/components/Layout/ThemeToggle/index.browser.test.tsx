import { render, screen, fireEvent } from '@testing-library/react';
import { vi, describe, beforeEach, it, expect } from 'vitest';

import { useTheme } from '@/context/theme/useTheme';

import { ThemeToggle } from './index';

vi.mock('@/context/theme/useTheme');

describe('ThemeToggle', () => {
  const mockToggleTheme = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    (useTheme as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      theme: 'g10',
      toggleTheme: mockToggleTheme,
    });
  });

  it('renders with light icon when theme is g10', () => {
    render(<ThemeToggle />);
    expect(screen.getByRole('button')).toBeInTheDocument();
    expect(document.querySelector('.icon')).toBeInTheDocument();
  });

  it('renders with dark icon when theme is g100', () => {
    (useTheme as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      theme: 'g100',
      toggleTheme: mockToggleTheme,
    });
    render(<ThemeToggle />);
    expect(document.querySelector('.icon')).toBeInTheDocument();
  });

  it('applies "off" class in light mode', () => {
    render(<ThemeToggle />);
    expect(screen.getByRole('button')).toHaveClass('off');
  });

  it('applies "on" class in dark mode', () => {
    (useTheme as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      theme: 'g100',
      toggleTheme: mockToggleTheme,
    });
    render(<ThemeToggle />);
    expect(screen.getByRole('button')).toHaveClass('on');
  });

  it('calls toggleTheme on click', () => {
    render(<ThemeToggle />);
    fireEvent.click(screen.getByRole('button'));
    expect(mockToggleTheme).toHaveBeenCalled();
  });

  it('calls toggleTheme on Enter key', () => {
    render(<ThemeToggle />);
    fireEvent.keyDown(screen.getByRole('button'), { key: 'Enter' });
    expect(mockToggleTheme).toHaveBeenCalled();
  });

  it('calls toggleTheme on Space key', () => {
    render(<ThemeToggle />);
    fireEvent.keyDown(screen.getByRole('button'), { key: ' ' });
    expect(mockToggleTheme).toHaveBeenCalled();
  });
});
