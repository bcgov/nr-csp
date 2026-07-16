import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import DetailSection, { type DetailItem } from './index';

describe('DetailSection', () => {
  it('renders a heading when a title is provided', () => {
    render(<DetailSection title="My Section" items={[{ label: 'A', value: 'a' }]} />);
    const heading = screen.getByRole('heading', { name: 'My Section' });
    expect(heading).toBeTruthy();
    expect(heading.classList.contains('detail-section__title')).toBe(true);
  });

  it('does not render a heading when no title is provided', () => {
    render(<DetailSection items={[{ label: 'A', value: 'a' }]} />);
    expect(screen.queryByRole('heading')).toBeNull();
  });

  it('renders a normal value', () => {
    render(<DetailSection items={[{ label: 'Name', value: 'Alice' }]} />);
    expect(screen.getByText('Name')).toBeTruthy();
    expect(screen.getByText('Alice')).toBeTruthy();
  });

  it('renders an em-dash for null, undefined, and empty-string values', () => {
    const items: DetailItem[] = [
      { label: 'Null', value: null },
      { label: 'Undefined', value: undefined },
      { label: 'Empty', value: '' },
    ];
    const { container } = render(<DetailSection items={items} />);
    const values = container.querySelectorAll('.detail-section__value');
    expect(values).toHaveLength(3);
    values.forEach((v) => expect(v.textContent).toBe('—'));
  });

  it('adds the full-width class when fullWidth is true', () => {
    const { container } = render(
      <DetailSection items={[{ label: 'Notes', value: 'long', fullWidth: true }]} />,
    );
    const item = container.querySelector('.detail-section__item') as HTMLElement;
    expect(item.classList.contains('detail-section__item--full')).toBe(true);
  });

  it('adds the error class and renders error text when errorText is provided', () => {
    const { container } = render(
      <DetailSection items={[{ label: 'Field', value: 'bad', errorText: 'Something went wrong' }]} />,
    );
    const item = container.querySelector('.detail-section__item') as HTMLElement;
    expect(item.classList.contains('detail-section__item--error')).toBe(true);
    expect(screen.getByText('Something went wrong')).toBeTruthy();
  });

  it('renders warning text when warningText is provided', () => {
    render(<DetailSection items={[{ label: 'Field', value: 'ok', warningText: 'Heads up' }]} />);
    expect(screen.getByText('Heads up')).toBeTruthy();
  });

  it('prefixes the section class when className is provided', () => {
    const { container } = render(
      <DetailSection className="extra" items={[{ label: 'A', value: 'a' }]} />,
    );
    const section = container.querySelector('section') as HTMLElement;
    expect(section.className).toBe('extra detail-section');
  });

  it('uses only the base section class when no className is provided', () => {
    const { container } = render(<DetailSection items={[{ label: 'A', value: 'a' }]} />);
    const section = container.querySelector('section') as HTMLElement;
    expect(section.className).toBe('detail-section');
  });

  it('renders children beneath the grid', () => {
    render(
      <DetailSection items={[{ label: 'A', value: 'a' }]}>
        <div data-testid="child">Child content</div>
      </DetailSection>,
    );
    expect(screen.getByTestId('child')).toBeTruthy();
  });
});
