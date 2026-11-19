import { render, screen, act } from '@testing-library/react';
import { afterEach, beforeEach, describe, it, expect, vi } from 'vitest';

import { LayoutProvider } from './LayoutProvider';
import { useLayout } from './useLayout';

function Consumer() {
  const { isSideNavExpanded, isHeaderPanelOpen, toggleSideNav, toggleHeaderPanel, closeHeaderPanel } = useLayout();
  return (
    <div>
      <span data-testid="sidenav">{String(isSideNavExpanded)}</span>
      <span data-testid="panel">{String(isHeaderPanelOpen)}</span>
      <button onClick={toggleSideNav}>toggleNav</button>
      <button onClick={toggleHeaderPanel}>togglePanel</button>
      <button onClick={closeHeaderPanel}>closePanel</button>
    </div>
  );
}

describe('LayoutProvider', () => {
  let desktopViewport: MediaQueryList;
  beforeEach(() => {
    desktopViewport = window.matchMedia('(min-width: 66rem)');
    vi.spyOn(desktopViewport, 'matches', 'get').mockReturnValue(true);
    vi.spyOn(window, 'matchMedia').mockReturnValue(desktopViewport);
  });
  afterEach(() => vi.restoreAllMocks());

  it('starts with the menu closed on a narrow screen', () => {
    vi.spyOn(desktopViewport, 'matches', 'get').mockReturnValue(false);
    render(
      <LayoutProvider>
        <Consumer />
      </LayoutProvider>,
    );
    expect(screen.getByTestId('sidenav')).toHaveTextContent('false');
  });

  it('renders children', () => {
    render(
      <LayoutProvider>
        <div data-testid="child">child</div>
      </LayoutProvider>,
    );
    expect(screen.getByTestId('child')).toBeInTheDocument();
  });

  it('expands the side nav by default', () => {
    render(
      <LayoutProvider>
        <Consumer />
      </LayoutProvider>,
    );
    expect(screen.getByTestId('sidenav').textContent).toBe('true');
  });

  it('toggles side nav', () => {
    render(
      <LayoutProvider>
        <Consumer />
      </LayoutProvider>,
    );
    act(() => screen.getByText('toggleNav').click());
    expect(screen.getByTestId('sidenav').textContent).toBe('false');
    act(() => screen.getByText('toggleNav').click());
    expect(screen.getByTestId('sidenav').textContent).toBe('true');
  });

  it('toggles header panel', () => {
    render(
      <LayoutProvider>
        <Consumer />
      </LayoutProvider>,
    );
    act(() => screen.getByText('togglePanel').click());
    expect(screen.getByTestId('panel').textContent).toBe('true');
  });

  it('closes header panel', () => {
    render(
      <LayoutProvider>
        <Consumer />
      </LayoutProvider>,
    );
    act(() => screen.getByText('togglePanel').click());
    act(() => screen.getByText('closePanel').click());
    expect(screen.getByTestId('panel').textContent).toBe('false');
  });
});
