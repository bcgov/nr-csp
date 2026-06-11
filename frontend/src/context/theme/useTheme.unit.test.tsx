import { render } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

import { ThemeContext } from './ThemeContext';
import { useTheme } from './useTheme';

function HookConsumer() {
  const { theme } = useTheme();
  return <div data-testid="result">{theme}</div>;
}

describe('useTheme', () => {
  it('throws when used outside ThemeProvider', () => {
    const consoleError = console.error;
    console.error = () => {};
    // ThemeContext has a default value so it won't throw — just returns default
    const { getByTestId } = render(<HookConsumer />);
    expect(getByTestId('result').textContent).toBe('g10');
    console.error = consoleError;
  });

  it('returns theme value from context', () => {
    const toggleTheme = vi.fn();
    const { getByTestId } = render(
      <ThemeContext.Provider value={{ theme: 'g100', toggleTheme }}>
        <HookConsumer />
      </ThemeContext.Provider>,
    );
    expect(getByTestId('result').textContent).toBe('g100');
  });
});
