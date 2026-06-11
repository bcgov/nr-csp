import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import * as useLayoutModule from '@/context/layout/useLayout';

import { LayoutHeaderPanel } from './index';

vi.mock('@/context/layout/useLayout', () => ({ useLayout: vi.fn() }));
// Isolate from the profile panel's own context dependencies.
vi.mock('@/components/Layout/HeaderPanelProfile', () => ({
  HeaderPanelProfile: () => <div data-testid="profile" />,
}));

const mockUseLayout = useLayoutModule.useLayout as ReturnType<typeof vi.fn>;

describe('LayoutHeaderPanel', () => {
  beforeEach(() => mockUseLayout.mockReset());

  it('renders nothing when the header panel is closed', () => {
    mockUseLayout.mockReturnValue({ isHeaderPanelOpen: false, closeHeaderPanel: vi.fn() });
    render(<LayoutHeaderPanel />);
    expect(screen.queryByText('My Profile')).not.toBeInTheDocument();
  });

  it('renders the profile panel when open', () => {
    mockUseLayout.mockReturnValue({ isHeaderPanelOpen: true, closeHeaderPanel: vi.fn() });
    render(<LayoutHeaderPanel />);
    expect(screen.getByText('My Profile')).toBeInTheDocument();
    expect(screen.getByTestId('profile')).toBeInTheDocument();
  });

  it('calls closeHeaderPanel when the close button is clicked', () => {
    const closeHeaderPanel = vi.fn();
    mockUseLayout.mockReturnValue({ isHeaderPanelOpen: true, closeHeaderPanel });
    render(<LayoutHeaderPanel />);
    fireEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(closeHeaderPanel).toHaveBeenCalledTimes(1);
  });
});
