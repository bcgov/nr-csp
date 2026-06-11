import { render } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import { LoadingScreen } from './index';

describe('LoadingScreen', () => {
  it('renders a Carbon loading spinner with an overlay', () => {
    const { container } = render(<LoadingScreen />);
    expect(container.querySelector('.cds--loading-overlay')).not.toBeNull();
    expect(container.querySelector('.cds--loading')).not.toBeNull();
  });

  it('exposes an accessible description on the spinner', () => {
    const { container } = render(<LoadingScreen />);
    // Carbon renders the `description` as the SVG <title>.
    expect(container.querySelector('title')?.textContent).toBe('Loading…');
  });
});
