import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { describe, expect, it } from 'vitest';

import { ROUTES } from '@/routes/routePaths';

import { LogoutPage } from './index';

function renderLogoutPage() {
  return render(
    <MemoryRouter initialEntries={[ROUTES.LOGOUT]}>
      <Routes>
        <Route path={ROUTES.LOGOUT} element={<LogoutPage />} />
        <Route path={ROUTES.LOGIN} element={<div>Login Page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('LogoutPage', () => {
  it('shows the signed-out heading and message', () => {
    renderLogoutPage();

    expect(screen.getByRole('heading', { name: /you have been signed out/i })).toBeInTheDocument();
    expect(screen.getByText(/thank you for using the coast selling application/i)).toBeInTheDocument();
  });

  it('offers a sign-in button that navigates to the login page', async () => {
    const user = userEvent.setup();
    renderLogoutPage();

    await user.click(screen.getByRole('button', { name: /sign in again/i }));

    expect(screen.getByText('Login Page')).toBeInTheDocument();
  });
});
