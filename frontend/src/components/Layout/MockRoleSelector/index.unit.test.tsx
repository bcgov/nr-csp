import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { MOCK_ROLE_KEY } from '@/context/auth/mockUser';

import { MockRoleSelector } from './index';

beforeEach(() => {
  localStorage.clear();
  vi.stubGlobal('location', { reload: vi.fn() });
});

describe('MockRoleSelector', () => {
  it('renders a select with the staff roles and the Business BCeID submitter', () => {
    render(<MockRoleSelector />);
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'ADMIN' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'APPROVE' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'VIEW' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'SUBMITTER (BCeID)' })).toHaveValue('SUBMITTER');
  });

  it('shows ADMIN as selected when localStorage is empty', () => {
    render(<MockRoleSelector />);
    expect(screen.getByRole('combobox')).toHaveValue('ADMIN');
  });

  it('shows the stored role as selected', () => {
    localStorage.setItem(MOCK_ROLE_KEY, 'VIEW');
    render(<MockRoleSelector />);
    expect(screen.getByRole('combobox')).toHaveValue('VIEW');
  });

  it('writes the new role to localStorage and reloads on change', async () => {
    render(<MockRoleSelector />);
    await userEvent.selectOptions(screen.getByRole('combobox'), 'APPROVE');
    expect(localStorage.getItem(MOCK_ROLE_KEY)).toBe('APPROVE');
    expect(window.location.reload).toHaveBeenCalledOnce();
  });
});
