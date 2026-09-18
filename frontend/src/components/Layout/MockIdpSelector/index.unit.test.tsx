import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { MOCK_IDP_KEY } from '@/context/auth/MockAuthProvider';

import { MockIdpSelector } from './index';

beforeEach(() => {
  localStorage.clear();
  vi.stubGlobal('location', { reload: vi.fn() });
});

describe('MockIdpSelector', () => {
  it('renders a select with IDIR and BCEIDBUSINESS options', () => {
    render(<MockIdpSelector />);
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'IDIR' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'BCEIDBUSINESS' })).toBeInTheDocument();
  });

  it('shows IDIR as selected when localStorage is empty', () => {
    render(<MockIdpSelector />);
    expect(screen.getByRole('combobox')).toHaveValue('IDIR');
  });

  it('shows the stored idp as selected', () => {
    localStorage.setItem(MOCK_IDP_KEY, 'BCEIDBUSINESS');
    render(<MockIdpSelector />);
    expect(screen.getByRole('combobox')).toHaveValue('BCEIDBUSINESS');
  });

  it('writes the new idp to localStorage and reloads on change', async () => {
    render(<MockIdpSelector />);
    await userEvent.selectOptions(screen.getByRole('combobox'), 'BCEIDBUSINESS');
    expect(localStorage.getItem(MOCK_IDP_KEY)).toBe('BCEIDBUSINESS');
    expect(window.location.reload).toHaveBeenCalledOnce();
  });
});
