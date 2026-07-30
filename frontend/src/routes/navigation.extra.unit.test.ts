import { describe, expect, it } from 'vitest';

import { NAVIGATION_ITEMS, useAppNavigation } from './navigation';

describe('useAppNavigation', () => {
  it('is callable and currently returns nothing', () => {
    expect(useAppNavigation()).toBeUndefined();
  });
});

describe('NAVIGATION_ITEMS structure', () => {
  it('every item has a name and an icon', () => {
    for (const item of NAVIGATION_ITEMS) {
      expect(item.name).toBeTruthy();
      expect(item.icon).toBeDefined();
    }
  });

  it('every item has either a path or children with paths', () => {
    for (const item of NAVIGATION_ITEMS) {
      if ('children' in item && item.children) {
        expect(item.children.length).toBeGreaterThan(0);
        for (const child of item.children) {
          expect(child.path).toBeTruthy();
          expect(child.name).toBeTruthy();
        }
      } else {
        expect(item.path).toBeTruthy();
      }
    }
  });
});
