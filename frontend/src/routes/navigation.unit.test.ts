import { describe, it, expect } from 'vitest';

import { NAVIGATION_ITEMS } from './navigation';

describe('NAVIGATION_ITEMS', () => {
  it('is an array', () => {
    expect(Array.isArray(NAVIGATION_ITEMS)).toBe(true);
  });

  it('has at least one entry', () => {
    expect(NAVIGATION_ITEMS.length).toBeGreaterThan(0);
  });

  it('Search entry points to "/"', () => {
    const search = NAVIGATION_ITEMS.find((item) => item.name === 'Search');
    expect(search).toBeDefined();
    expect(search?.path).toBe('/search');
  });
});
