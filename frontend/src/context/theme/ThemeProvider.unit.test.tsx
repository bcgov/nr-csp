import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

vi.mock('@carbon/react', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@carbon/react')>();
  return {
    ...actual,
    Theme: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  };
});

import { ThemeProvider } from './ThemeProvider';
import { useTheme } from './useTheme';

function Consumer() {
  const { theme, toggleTheme } = useTheme();
  return (
    <div>
      <span data-testid="theme">{theme}</span>
      <button onClick={toggleTheme}>toggle</button>
    </div>
  );
}

describe('ThemeProvider', () => {
  it('provides g10 as the default theme', () => {
    render(
      <ThemeProvider>
        <Consumer />
      </ThemeProvider>,
    );
    expect(screen.getByTestId('theme').textContent).toBe('g10');
  });

  it('toggles from g10 to g100', () => {
    render(
      <ThemeProvider>
        <Consumer />
      </ThemeProvider>,
    );
    act(() => screen.getByText('toggle').click());
    expect(screen.getByTestId('theme').textContent).toBe('g100');
  });

  it('toggles back from g100 to g10', () => {
    render(
      <ThemeProvider>
        <Consumer />
      </ThemeProvider>,
    );
    act(() => screen.getByText('toggle').click());
    act(() => screen.getByText('toggle').click());
    expect(screen.getByTestId('theme').textContent).toBe('g10');
  });

  it('sets data-carbon-theme attribute on document root', () => {
    render(
      <ThemeProvider>
        <Consumer />
      </ThemeProvider>,
    );
    expect(document.documentElement.dataset.carbonTheme).toBe('g10');
  });
});
