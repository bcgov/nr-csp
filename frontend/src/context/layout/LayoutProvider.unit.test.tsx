import { render, screen, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

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
