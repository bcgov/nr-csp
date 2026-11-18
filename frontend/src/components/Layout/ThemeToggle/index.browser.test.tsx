import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { userEvent } from 'vitest/browser';

import { ThemeProvider } from '@/context/theme/ThemeProvider';

import { ThemeToggle } from './index';

describe('ThemeToggle', () => {
  it('switches themes with Enter and Space while keeping keyboard focus', async () => {
    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>,
    );
    const toggle = screen.getByRole('switch', { name: 'Dark theme' });
    toggle.focus();
    await userEvent.keyboard('{Enter}');
    await vi.waitFor(() => expect(toggle).toBeChecked());
    await userEvent.keyboard(' ');
    await vi.waitFor(() => expect(toggle).not.toBeChecked());
    expect(toggle).toHaveFocus();
  });
});
