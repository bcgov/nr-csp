import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import TagWrapper from './TagWrapper';

describe('UnderConstructionTag via TagWrapper', () => {
  const arrange = (props: Partial<React.ComponentProps<typeof TagWrapper>> = {}) =>
    render(
      <TagWrapper {...props}>
        <span data-testid="wrapped-child">Wrapped content</span>
      </TagWrapper>,
    );

  it('renders the wrapped child and the "Under construction" tag', () => {
    arrange();
    expect(screen.getByTestId('wrapped-child')).toBeInTheDocument();
    expect(screen.getByText('Under construction')).toBeInTheDocument();
  });

  it('uses the "feature" tooltip wording by default', () => {
    arrange();
    expect(
      screen.getByText('This feature is under development. Features may be incomplete or display incorrect data.'),
    ).toBeInTheDocument();
  });

  it('uses the "page" tooltip wording when type is page', () => {
    arrange({ type: 'page' });
    expect(
      screen.getByText('This page is under development. Features may be incomplete or display incorrect data.'),
    ).toBeInTheDocument();
  });

  it('places the tag after the child by default (position right)', () => {
    arrange();
    const child = screen.getByTestId('wrapped-child');
    const tag = screen.getByText('Under construction');
    expect(child.compareDocumentPosition(tag) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('places the tag before the child when position is left', () => {
    arrange({ position: 'left' });
    const child = screen.getByTestId('wrapped-child');
    const tag = screen.getByText('Under construction');
    expect(child.compareDocumentPosition(tag) & Node.DOCUMENT_POSITION_PRECEDING).toBeTruthy();
  });
});
