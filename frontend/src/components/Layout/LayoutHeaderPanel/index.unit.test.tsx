import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useLayout } from '@/context/layout/useLayout';

import { LayoutHeaderPanel } from './index';

vi.mock('@/context/layout/useLayout', () => ({ useLayout: vi.fn() }));
// HeaderPanelProfile has its own auth-context needs; stub it so this test stays
// focused on the panel shell itself.
vi.mock('@/components/Layout/HeaderPanelProfile', () => ({
  HeaderPanelProfile: () => <div data-testid="header-panel-profile" />,
}));

describe('LayoutHeaderPanel', () => {
  const closeHeaderPanel = vi.fn();

  const arrange = ({ isHeaderPanelOpen = true } = {}) => {
    vi.mocked(useLayout).mockReturnValue({ isHeaderPanelOpen, closeHeaderPanel } as unknown as ReturnType<
      typeof useLayout
    >);
    return render(<LayoutHeaderPanel />);
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when the header panel is closed', () => {
    const { container } = arrange({ isHeaderPanelOpen: false });
    expect(container).toBeEmptyDOMElement();
  });

  it('renders the profile panel with its title when open', () => {
    arrange();
    expect(screen.getByRole('heading', { name: 'My Profile' })).toBeInTheDocument();
    expect(screen.getByTestId('header-panel-profile')).toBeInTheDocument();
  });

  it('closes the panel when the close button is clicked', () => {
    arrange();
    fireEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(closeHeaderPanel).toHaveBeenCalledTimes(1);
  });
});
