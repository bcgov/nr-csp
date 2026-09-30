import { type Page, expect } from '@playwright/test';

/**
 * Race-free assertions about the app's toast notifications.
 *
 * ---------------------------------------------------------------------------
 * WHY THIS EXISTS — TOASTS ARE TRANSIENT, SO ASSERTING THEM LIVE IS A RACE
 * ---------------------------------------------------------------------------
 * `Layout/index.tsx` renders every non-persistent notification with `timeout={5000}`, so a toast
 * removes ITSELF from the DOM five seconds after it appears. Nothing the test does keeps it alive.
 *
 * That makes `expect(page.locator('.layout-toast-container').filter({ hasText: … }))` unsound as a
 * post-hoc assertion, because the action helpers legitimately settle on something slower than the
 * toast's lifetime. `InvoicePage.unapprove()`, for instance, clicks and then waits up to 30s for the
 * Approve button to appear; on a loaded machine that settle can take longer than 5s, by which point
 * the toast the step is about to assert has already dismissed itself. The test then fails claiming
 * the app never showed a toast, when in fact it showed one and took it away again.
 *
 * Observed exactly that way: UC-INBOX-005 failed on `toast('unapproved.')` with "element(s) not
 * found" while the SAME snapshot showed the status pill at UNAPPROVED and the button already
 * swapped to "Approve" — i.e. the unapprove had fully succeeded. The suite was running alongside a
 * `mvn verify` on the same box, which is what pushed the settle past 5s. The race was always there;
 * load only decided when it lost.
 *
 * ---------------------------------------------------------------------------
 * HOW IT WORKS
 * ---------------------------------------------------------------------------
 * An init script installs a MutationObserver that records each toast's text the moment it is added
 * to the DOM, into `window.__cspToasts`. Assertions then read that log instead of the live DOM, so
 * they are immune to when the toast dismissed — but NOT weaker: a toast the app never rendered is
 * never recorded, so a genuine missing-toast bug still fails. It fails with a better message, too,
 * because the poll prints every toast that WAS shown.
 *
 * `addInitScript` runs before the app's own scripts on every document, so the observer is in place
 * before React can mount and fire the first notification. The log is per-document, which is the
 * correct scope: it survives the SPA's client-side route changes (same document) and resets on a
 * real reload, exactly as the DOM does.
 */

/** The recorded-toast log the init script maintains on `window`. */
declare global {
  interface Window {
    __cspToasts?: string[];
  }
}

/**
 * Install the recorder on a page. Call ONCE per page, before any navigation — the `page` fixture
 * override in `steps/fixtures/global.ts` does this for every scenario, so tests never call it.
 */
export async function installToastRecorder(page: Page): Promise<void> {
  await page.addInitScript(() => {
    window.__cspToasts = [];
    // Dedupe by exact text: the observer rescans on every mutation batch (cheaper and far more
    // robust than trying to identify which specific added node was the toast), so without this a
    // single toast would be recorded once per unrelated DOM change while it is on screen.
    const seen = new Set<string>();

    const scan = () => {
      for (const el of document.querySelectorAll('.layout-toast-container > *')) {
        const text = (el.textContent ?? '').trim();
        if (text && !seen.has(text)) {
          seen.add(text);
          window.__cspToasts?.push(text);
        }
      }
    };

    const start = () => {
      scan();
      new MutationObserver(scan).observe(document.body, { childList: true, subtree: true });
    };

    // At init-script time the document is usually still empty, so `document.body` may not exist yet.
    if (document.body) start();
    else document.addEventListener('DOMContentLoaded', start, { once: true });
  });
}

/** Every toast text recorded on the current document, oldest first. */
export async function recordedToasts(page: Page): Promise<string[]> {
  return page.evaluate(() => window.__cspToasts ?? []);
}

/**
 * Assert the app showed a toast whose text contains `text`, whenever it appeared.
 *
 * Polls rather than reading once, because the toast can land slightly after the action's settle
 * signal (the mutation's `onSuccess` fires the notification, and React renders it on a later tick).
 * On failure the received value is the full list of toasts shown, which is what you need to tell
 * "no toast at all" (the action silently did nothing) from "the wrong toast" (e.g. an error toast).
 */
export async function expectToastShown(page: Page, text: string, timeout = 15_000): Promise<void> {
  await expect
    .poll(() => recordedToasts(page), { timeout })
    .toEqual(expect.arrayContaining([expect.stringContaining(text)]));
}
