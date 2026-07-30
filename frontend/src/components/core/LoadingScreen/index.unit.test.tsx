import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { LoadingScreen } from './index';

describe('LoadingScreen', () => {
  it('renders a loading indicator with an accessible description', () => {
    render(<LoadingScreen />);
    expect(screen.getByTitle('Loading…')).toBeInTheDocument();
  });

  it('renders the loading indicator with an overlay', () => {
    const { container } = render(<LoadingScreen />);
    expect(container.querySelector('.cds--loading-overlay')).toBeInTheDocument();
  });
});
