import { render } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

import { LayoutContext } from './LayoutContext';
import { useLayout } from './useLayout';

function HookConsumer() {
  const { isSideNavExpanded } = useLayout();
  return <div data-testid="result">{String(isSideNavExpanded)}</div>;
}

describe('useLayout', () => {
  it('throws when used outside LayoutProvider', () => {
    const consoleError = console.error;
    console.error = () => {};
    expect(() => render(<HookConsumer />)).toThrow('useLayout must be used within LayoutProvider');
    console.error = consoleError;
  });

  it('returns context value when inside provider', () => {
    const value = {
      isSideNavExpanded: true,
      toggleSideNav: vi.fn(),
      isHeaderPanelOpen: false,
      toggleHeaderPanel: vi.fn(),
      closeHeaderPanel: vi.fn(),
    };
    const { getByTestId } = render(
      <LayoutContext.Provider value={value}>
        <HookConsumer />
      </LayoutContext.Provider>,
    );
    expect(getByTestId('result').textContent).toBe('true');
  });
});
