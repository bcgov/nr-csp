import { describe, it, expect } from 'vitest';

import { isModifiedClick } from './link';

const click = (over: Partial<MouseEvent> = {}) => ({
  metaKey: false,
  ctrlKey: false,
  shiftKey: false,
  altKey: false,
  button: 0,
  ...over,
});

describe('isModifiedClick', () => {
  it('is false for a plain primary click, which the app routes itself', () => {
    expect(isModifiedClick(click())).toBe(false);
  });

  it.each([
    ['meta (open in a new tab on macOS)', { metaKey: true }],
    ['ctrl (open in a new tab elsewhere)', { ctrlKey: true }],
    ['shift (open in a new window)', { shiftKey: true }],
    ['alt (download the target)', { altKey: true }],
    ['middle button (open in a background tab)', { button: 1 }],
  ])('is true for %s, which the browser must handle', (_label, over) => {
    expect(isModifiedClick(click(over))).toBe(true);
  });
});
