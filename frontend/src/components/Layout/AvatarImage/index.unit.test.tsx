import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import AvatarImage from './index';

describe('AvatarImage', () => {
  it('renders the first letters of the first two name parts', () => {
    render(<AvatarImage userName="Jane Doe" size="large" />);
    expect(screen.getByTestId('avatar-initials')).toHaveTextContent('JD');
  });

  it('only uses the first two name parts for longer names', () => {
    render(<AvatarImage userName="Jane Mary Doe" size="large" />);
    expect(screen.getByTestId('avatar-initials')).toHaveTextContent('JM');
  });

  it('renders a single initial for a single-word name', () => {
    render(<AvatarImage userName="Jane" size="small" />);
    expect(screen.getByTestId('avatar-initials')).toHaveTextContent('J');
  });

  it('renders no initials when the username is empty', () => {
    render(<AvatarImage userName="" size="small" />);
    expect(screen.getByTestId('avatar-initials')).toHaveTextContent('');
  });

  it('handles extra whitespace between name parts without crashing', () => {
    render(<AvatarImage userName="Jane  Doe" size="small" />);
    // "Jane  Doe".split(' ') yields ['Jane', '', 'Doe'] so only 'J' is shown.
    expect(screen.getByTestId('avatar-initials')).toHaveTextContent(/^J$/);
  });

  it.each(['small', 'large'] as const)('applies the "%s" size class to the container', (size) => {
    const { container } = render(<AvatarImage userName="Jane Doe" size={size} />);
    expect(container.querySelector('.profile-image')).toHaveClass(size);
  });
});
