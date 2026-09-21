/**
 * Clicks that must be left to the browser rather than routed in-app: a modified
 * click (open in a new tab or window, download, extend selection) or any
 * non-primary button. A handler that calls preventDefault() unconditionally
 * swallows these, so Cmd/Ctrl+Click replaces the current tab instead of opening
 * a background one — which is how inbox users line several records up at once.
 *
 * Takes the fields off a mouse event rather than the event itself, so it works
 * with both React's synthetic event and a plain DOM one.
 */
export const isModifiedClick = (
  event: Pick<MouseEvent, 'metaKey' | 'ctrlKey' | 'shiftKey' | 'altKey' | 'button'>,
): boolean => event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0;
