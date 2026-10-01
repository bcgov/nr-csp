import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import PageTitleProvider from '@/context/pageTitle/PageTitleProvider';

import PageTitle from './index';

/**
 * Node (jsdom) tests for the breadcrumb link behaviour.
 *
 * ---------------------------------------------------------------------------
 * WHY THESE EXIST ALONGSIDE index.browser.test.tsx
 * ---------------------------------------------------------------------------
 * The browser file covers the same component, but coverage is measured on the NODE project only
 * (`test:coverage` runs `vitest run --coverage --project node`), so nothing asserted there counts
 * towards it — the click guard showed up as entirely uncovered new code despite being tested.
 *
 * The split is deliberate rather than duplicated: the browser file keeps what needs a real browser
 * (Carbon's rendering, focus behaviour, a real anchor's default action), and this file covers the
 * decision logic in the click handler and the navigable/non-navigable branches, which is ordinary
 * JavaScript and does not.
 */

const navigate = vi.fn();

vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router');
  return { ...actual, useNavigate: () => navigate };
});

const renderPageTitle = (props: React.ComponentProps<typeof PageTitle>) =>
  render(
    <PageTitleProvider>
      <PageTitle {...props} />
    </PageTitleProvider>,
  );

const twoCrumbs = [
  { name: 'Invoice search', path: '/search' },
  { name: 'Invoice', path: '#' },
];

/**
 * Click the crumb and report whether the component took the event.
 *
 * `defaultPrevented` is the one bit that decides whether the browser still gets to act on the
 * anchor, so it is what separates "routed client-side" from "left for the browser".
 *
 * The document-level listener cancels the event after the component has seen it, so jsdom never
 * attempts the real navigation to href (which it cannot perform, and which logs a "Not implemented:
 * navigation" error that would otherwise be noise in every run).
 */
const clickCrumb = (name: string, init: MouseEventInit = {}): boolean => {
  const link = screen.getByRole('link', { name });
  let prevented = false;
  const capture = (event: Event) => {
    prevented = event.defaultPrevented;
    event.preventDefault();
  };
  document.addEventListener('click', capture);
  try {
    act(() => {
      link.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, ...init }));
    });
  } finally {
    document.removeEventListener('click', capture);
  }
  return prevented;
};

describe('PageTitle breadcrumbs (node)', () => {
  beforeEach(() => {
    navigate.mockClear();
  });

  it('routes a plain left click client-side', () => {
    renderPageTitle({ title: 'T', breadCrumbs: twoCrumbs });

    expect(clickCrumb('Invoice search')).toBe(true);
    expect(navigate).toHaveBeenCalledWith('/search');
  });

  // Each of these is a browser gesture for opening a link elsewhere, so the handler must leave the
  // event alone and let the browser act. Covering them one at a time (rather than one combined
  // click) is what pins each condition of the guard independently — a guard that checked only
  // ctrlKey would still pass a test that pressed every modifier at once.
  it.each([
    ['ctrl', { ctrlKey: true }],
    ['meta', { metaKey: true }],
    ['shift', { shiftKey: true }],
    ['alt', { altKey: true }],
  ])('leaves a %s+click for the browser', (_label, init) => {
    renderPageTitle({ title: 'T', breadCrumbs: twoCrumbs });

    expect(clickCrumb('Invoice search', init)).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });

  // A middle click (button 1) is "open in new tab" too, and a right click (button 2) opens the
  // context menu — neither should be converted into an in-place route.
  it.each([
    ['middle', 1],
    ['right', 2],
  ])('leaves a %s click for the browser', (_label, button) => {
    renderPageTitle({ title: 'T', breadCrumbs: twoCrumbs });

    expect(clickCrumb('Invoice search', { button })).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });

  // If something upstream already cancelled the event, the crumb must not then route on top of it.
  it('does not route an event another handler already prevented', () => {
    renderPageTitle({ title: 'T', breadCrumbs: twoCrumbs });
    const link = screen.getByRole('link', { name: 'Invoice search' });

    const preventFirst = (event: Event) => event.preventDefault();
    link.addEventListener('click', preventFirst);
    try {
      act(() => {
        link.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
      });
    } finally {
      link.removeEventListener('click', preventFirst);
    }

    expect(navigate).not.toHaveBeenCalled();
  });

  // The two ways a crumb is NOT a destination: it is the page you are on, or it is a label whose
  // path is '#'. Both must render without a link rather than as a dead one.
  it('renders the current page and "#" placeholders without links', () => {
    renderPageTitle({
      title: 'T',
      breadCrumbs: [
        { name: 'Invoice search', path: '/search' },
        { name: 'Invoice', path: '#' },
        { name: 'INV-001', path: '#' },
      ],
    });

    expect(screen.getByRole('link', { name: 'Invoice search' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Invoice' })).not.toBeInTheDocument();
    // Last crumb: current page, so unlinked even though the others are.
    expect(screen.queryByRole('link', { name: 'INV-001' })).not.toBeInTheDocument();
  });

  it('renders nothing breadcrumb-shaped when no crumbs are given', () => {
    renderPageTitle({ title: 'T' });

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('T');
  });
});
