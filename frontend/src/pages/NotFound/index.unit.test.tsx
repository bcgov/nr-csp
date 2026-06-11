import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import { NotFoundPage } from './index';

describe('NotFoundPage', () => {
  it('renders 404 heading', () => {
    render(<NotFoundPage />);
    expect(screen.getByRole('heading', { name: /404/i })).toBeInTheDocument();
  });
});
