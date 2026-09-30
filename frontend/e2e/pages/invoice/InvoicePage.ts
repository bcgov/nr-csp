import { type Page, type Locator, expect } from '@playwright/test';

import { signInAsMockUser, setDateField, type MockRole } from '../common/authNav';
import { fieldError } from '../common/carbonHelpers';

/**
 * Page Object — Invoice details (`/invoice` for a new invoice, `/invoice/:id` for a saved one).
 *
 * SHARED ACROSS DOMAINS, which is why it lives in `pages/invoice/` rather than under one domain:
 * the SUBM journey drives this screen to create and submit an invoice, and the INBOX review
 * journey lands on it to approve one. Its fixture therefore lives in `steps/fixtures/global.ts`
 * (the documented home for a page object no single subject area owns), not in a domain file.
 *
 * Selectors only; business assertions live in the steps — `steps/subm/invoice.steps.ts` for the
 * create/submit vocabulary, `steps/inbox/review.steps.ts` for the review/approve vocabulary.
 *
 * RE-GROUNDING NOTE. The legacy Gherkin for UC-SUBM-001..004 was written against the JSF screen
 * `invoiceDetails.xhtml` and addresses fields by suffix-matched JSF client ids
 * (`[id$='Invoice-Number']`, `[id$='InvoiceType']`, …). None of those exist here. The React page
 * gives every control a short, stable `id` (frontend/src/pages/Invoice/index.tsx), so this object
 * uses those ids plus ARIA roles, and never a Carbon class name or `nth-child`.
 *
 * THREE CARBON PATTERNS THIS PAGE USES, and why each needs its own helper:
 *   * SingleSelect  = Carbon `Dropdown`  -> a BUTTON that opens a listbox. You cannot type into it;
 *     you click the trigger then click the option. (Invoice type, Submitted by, Maturity, and the
 *     Add-item modal's Secondary Sort / Species / Grade.)
 *   * ClientAutocomplete / ClientNumberAutocomplete = Carbon `ComboBox` -> a real text INPUT that
 *     queries `/api/clients` after a 300 ms debounce and only at >= 2 typed characters. You type,
 *     then click the returned option; selecting one auto-fills the paired number/location fields.
 *   * TagInput = a text input whose value is committed to a chip on Enter (or comma, or blur).
 *     Typing alone leaves the value UNCOMMITTED, so the request body would ship an empty list.
 */
export class InvoicePage {
  /** Route for a brand-new invoice — ROUTES.INVOICE in frontend/src/routes/routePaths.ts. */
  static readonly NEW_ROUTE = '/invoice';

  constructor(private readonly page: Page) {}

  // -------------------------------------------------------------------------
  // Navigation / readiness
  // -------------------------------------------------------------------------

  /** Seed the mock identity, then load the blank invoice form (see pages/common/authNav.ts). */
  async openNewInvoice(role: MockRole = 'ADMIN'): Promise<void> {
    await signInAsMockUser(this.page, role);
    await this.page.goto(InvoicePage.NEW_ROUTE);
    await this.waitForFormReady();
  }

  /**
   * Seed the mock identity, then open an EXISTING invoice directly by id.
   *
   * Used by scenarios whose subject is the invoice screen itself rather than the path to it — the
   * review journey reaches this page by following the Inbox's own links instead, which is what
   * makes it a navigation test.
   */
  async openInvoiceById(invoiceId: number, role: MockRole = 'ADMIN'): Promise<void> {
    await signInAsMockUser(this.page, role);
    await this.page.goto(`${InvoicePage.NEW_ROUTE}/${invoiceId}`);
    await this.waitForExistingInvoiceLoaded();
  }

  /**
   * Readiness for an EXISTING invoice. `waitForFormReady` is not enough on its own: the page
   * renders a skeleton until the invoice AND both client lookups have resolved
   * (`initialLoadComplete`), and the action buttons are skeletons until then — so a button-state
   * assertion made too early reads the skeleton, not the real control.
   */
  async waitForExistingInvoiceLoaded(): Promise<void> {
    await this.waitForFormReady();
    await expect(this.statusTag).not.toBeEmpty({ timeout: 30_000 });
  }

  /**
   * Readiness: the header form is painted. `#inv-number` is the first editable control and only
   * renders once the page is past its skeleton, so it is the cheapest honest readiness signal.
   */
  async waitForFormReady(): Promise<void> {
    await expect(this.invoiceNumberInput).toBeVisible({ timeout: 30_000 });
  }

  /** The invoice id in the URL, or null while the invoice is still unsaved (`/invoice`). */
  async currentInvoiceId(): Promise<string | null> {
    const match = /\/invoice\/(\d+)(?:[/?#]|$)/.exec(this.page.url());
    return match ? match[1] : null;
  }

  /**
   * Wait for `handleSave`'s post-create `navigate('/invoice/{id}')` and return the new id.
   * Creating an invoice is the ONLY place the URL gains an id, so this doubles as the proof that
   * the POST succeeded — a failed create leaves the browser on `/invoice`.
   */
  async waitForSavedInvoiceId(): Promise<string> {
    await this.page.waitForURL(/\/invoice\/\d+(?:[/?#]|$)/, { timeout: 30_000 });
    const id = await this.currentInvoiceId();
    if (!id) throw new Error(`URL matched the saved-invoice pattern but no id parsed: ${this.page.url()}`);
    return id;
  }

  // -------------------------------------------------------------------------
  // Shared Carbon interaction helpers
  // -------------------------------------------------------------------------

  /**
   * Choose an option from a Carbon `Dropdown` (the app's SingleSelect wrapper).
   *
   * `optionLabel` is the rendered text, which is `itemToString` — the lookup DESCRIPTION for
   * Invoice type / Maturity ("Purchase", "Old Growth"), but `"<code> - <description>"` for the
   * sort-code dropdowns and the bare CODE for Species / Grade. Scoped to the open listbox so a
   * label that also appears elsewhere on the page cannot match.
   */
  private async selectFromDropdown(fieldId: string, optionLabel: string, scope?: Locator): Promise<void> {
    const root = scope ?? this.page;
    await root.locator(fieldId).click();
    await root.getByRole('option', { name: optionLabel, exact: true }).click();
  }

  /**
   * Type into a Carbon `ComboBox` client autocomplete and pick the named result.
   *
   * `searchTerm` must be >= 2 characters (the query is gated on `debounce.length >= 2`) and should
   * return exactly one client, so the option is unambiguous. Playwright's auto-wait on the option
   * locator covers the 300 ms debounce plus the round trip — no explicit sleep.
   */
  private async pickClient(fieldId: string, searchTerm: string, clientName: string): Promise<void> {
    const input = this.page.locator(fieldId);
    await input.click();
    await input.fill(searchTerm);
    await this.page.getByRole('option', { name: clientName, exact: true }).click();
  }

  /**
   * Commit a value into a TagInput as a chip.
   *
   * Enter is what `handleKeyDown` commits on. Filling the input and moving on would leave the value
   * in the component's local `draft` state and out of the request body — the field would look
   * populated on screen while the invoice saved without it.
   */
  private async addTag(fieldId: string, value: string): Promise<void> {
    const input = this.page.locator(fieldId);
    await input.click();
    await input.fill(value);
    await input.press('Enter');
  }

  // -------------------------------------------------------------------------
  // Section 1 — Invoice details
  // -------------------------------------------------------------------------

  get invoiceNumberInput(): Locator {
    return this.page.locator('#inv-number');
  }

  async setInvoiceNumber(value: string): Promise<void> {
    await this.invoiceNumberInput.fill(value);
  }

  async selectInvoiceType(label: string): Promise<void> {
    await this.selectFromDropdown('#inv-type', label);
  }

  /** Carbon DatePicker — via the shared click/fill/Escape/blur helper (the overlay swallows a plain fill). */
  async setInvoiceDate(isoDate: string): Promise<void> {
    await setDateField(this.page, '#inv-date', isoDate);
  }

  // -------------------------------------------------------------------------
  // Section 2 — Invoice address information
  // -------------------------------------------------------------------------

  async selectSubmittedBy(label: string): Promise<void> {
    await this.selectFromDropdown('#submitted-by', label);
  }

  async chooseSubmittingClient(searchTerm: string, clientName: string): Promise<void> {
    await this.pickClient('#submitting-client-name', searchTerm, clientName);
  }

  async chooseOtherParty(searchTerm: string, clientName: string): Promise<void> {
    await this.pickClient('#other-client-name', searchTerm, clientName);
  }

  /** Auto-filled by `handleSubmittingClientSelect` from the chosen client — read, never typed. */
  get submittingClientNumberInput(): Locator {
    return this.page.locator('#submitting-client-number');
  }

  get submittingClientLocationInput(): Locator {
    return this.page.locator('#submitting-client-location');
  }

  get otherClientNumberInput(): Locator {
    return this.page.locator('#other-client-number');
  }

  get otherClientLocationInput(): Locator {
    return this.page.locator('#other-client-location');
  }

  // -------------------------------------------------------------------------
  // Section 3 — Invoice detail information
  // -------------------------------------------------------------------------

  async selectMaturity(label: string): Promise<void> {
    await this.selectFromDropdown('#maturity', label);
  }

  /**
   * FOB is a free-text code checked against `/api/lookup/fob` ON BLUR (`validateFobOnBlur`), so the
   * blur is part of entering the value — without it the inline check never runs and a typo would
   * only surface as a server-side 400 later.
   */
  async setFobCode(code: string): Promise<void> {
    const input = this.page.locator('#fob-code');
    await input.fill(code);
    await input.blur();
  }

  async addBoomNumber(value: string): Promise<void> {
    await this.addTag('#boom-numbers', value);
  }

  /**
   * The three source-document fields. All three always render, in every status — so their presence
   * proves the panel exists, not that the invoice carries any values. Use `tagChipsFor` for that.
   */
  get boomNumbersInput(): Locator {
    return this.page.locator('#boom-numbers');
  }

  get timberMarksInput(): Locator {
    return this.page.locator('#timber-marks');
  }

  get weighSlipsInput(): Locator {
    return this.page.locator('#weigh-slips');
  }

  /** The committed chip for a tag value, e.g. to prove the Enter actually landed. */
  boomNumberChip(value: string): Locator {
    return this.page.getByTitle(`Remove ${value}`);
  }

  // -------------------------------------------------------------------------
  // Status, notifications, totals
  // -------------------------------------------------------------------------

  /**
   * The status pill beside the page title. Renders the raw status code for a saved invoice
   * (InvoiceStatusTag: "DFT", "PRO", …) and the literal "New" for an unsaved one
   * (InvoiceDetailsTag). Both live in the same `.invoice-page__status-tag` slot.
   */
  get statusTag(): Locator {
    return this.page.locator('.invoice-page__status-tag');
  }

  /**
   * A page-level WARNING banner by its text — driven by the `warnings[]` the backend returns on a
   * successful mutation or read. Not the bottom-right toasts.
   *
   * Scoped by Carbon's severity class, not just by text. The app gives warning and error banners
   * the SAME `invoice-page__notification` class and distinguishes them only by `kind`
   * (`pages/Invoice/index.tsx`), so a text-only locator matches either — which would let a message
   * demoted from `errors[]` to `warnings[]` still satisfy an "error banner" assertion, hiding
   * exactly the severity regression such an assertion exists to catch.
   */
  warningBanner(text: string): Locator {
    return this.page
      .locator('.invoice-page__notification.cds--inline-notification--warning')
      .filter({ hasText: text });
  }

  /** A page-level ERROR banner by its text. Scoped by severity — see `warningBanner`. */
  errorBanner(text: string): Locator {
    return this.page
      .locator('.invoice-page__notification.cds--inline-notification--error')
      .filter({ hasText: text });
  }

  /**
   * A bottom-right Carbon toast by its title, e.g. "Invoice 'E2E-123' created.".
   * Rendered by components/Layout (the notification context), outside the invoice page's own DOM.
   */
  toast(text: string): Locator {
    return this.page.locator('.layout-toast-container').filter({ hasText: text });
  }

  /**
   * A breadcrumb crumb by its label.
   *
   * The "Invoice search" crumb appears ONLY when the page was reached with
   * `state: { fromSearch: true }` — which the search results' invoice link sets. It is this app's
   * equivalent of the legacy screen's Back button, so a scenario arriving from search asserts it,
   * and one arriving any other way asserts it is absent.
   *
   * Addressed by the `link` role, which is also the assertion: a navigable crumb must be a real
   * anchor. It used to be a `<span>` with an `onClick` — clickable by mouse, but with no link
   * semantics and no keyboard focus — which forced this locator to be structural. That was BUG-002
   * in the UC-SRCH defects.md, now fixed, so the role-based locator both works and guards it.
   */
  breadcrumb(label: string): Locator {
    return this.page
      .locator('.page-title-breadcrumb')
      .getByRole('link', { name: label, exact: true });
  }

  /** Every action button in the page's action row, by visible label. */
  actionButton(label: string): Locator {
    return this.page.locator('.invoice-page__actions').getByRole('button', { name: label, exact: true });
  }

  /**
   * A read-only meta value from the header sections, addressed by its visible label
   * ("Total pieces", "Total volume (m3)", "Total amount", "Submission ID", …).
   *
   * The label and value are sibling spans inside one `.invoice-page__meta-col`, so this filters to
   * the column holding the label and reads its value span — no positional indexing.
   */
  metaValue(label: string): Locator {
    return this.page
      .locator('.invoice-page__meta-col')
      .filter({ has: this.page.locator('.invoice-page__meta-label', { hasText: label }) })
      .locator('.invoice-page__meta-value');
  }

  // -------------------------------------------------------------------------
  // Action buttons
  // -------------------------------------------------------------------------

  /**
   * `exact: true` matters on all of these: "Save" would otherwise also match the group-edit modal's
   * "Save", and "Submit" is a prefix of nothing here but is kept exact for symmetry.
   */
  get saveButton(): Locator {
    return this.page.getByRole('button', { name: 'Save', exact: true });
  }

  get submitButton(): Locator {
    return this.page.getByRole('button', { name: 'Submit', exact: true });
  }

  /**
   * Click Save and wait for the write to complete.
   *
   * Waiting for the button to be enabled again is NOT sufficient on its own, and was a bug: that
   * is the same condition asserted before the click, so it resolves on the first poll whenever
   * React has not yet flipped the button into its `anyMutationPending` disabled state — and
   * `save()` then returns before the request has even been issued.
   *
   * So wait for the RESPONSE. A new invoice POSTs to /api/invoices and an existing one PUTs to
   * /api/invoices/{id}; either satisfies this. `waitForResponse` is armed before the click so a
   * fast response cannot be missed, and the button check afterwards confirms the page has settled
   * out of its pending state.
   */
  async save(): Promise<void> {
    await expect(this.saveButton).toBeEnabled();
    const responded = this.page.waitForResponse(
      (r) => /\/api\/invoices(\/\d+)?$/.test(new URL(r.url()).pathname) && ['POST', 'PUT'].includes(r.request().method()),
      { timeout: 30_000 },
    );
    await this.saveButton.click();
    await responded;
    await expect(this.saveButton).toBeEnabled({ timeout: 30_000 });
  }

  async submit(): Promise<void> {
    await expect(this.submitButton).toBeEnabled();
    await this.submitButton.click();
    // Submit moves the invoice to PRO, which is NOT in SUBMITTABLE_STATUSES — so the button ends up
    // disabled for a reason other than "still pending". Settle on the status pill instead.
    await expect(this.statusTag).not.toHaveText('DFT', { timeout: 30_000 });
  }

  // -------------------------------------------------------------------------
  // Reviewer actions (UC-INBOX-002 / UC-INBOX-003)
  // -------------------------------------------------------------------------

  /**
   * Approve and Unapprove occupy the SAME slot in the button row — the page renders one or the
   * other, never both (`canUnapprove ? <Unapprove/> : <Approve/>`). So "Approve became disabled
   * and Unapprove became enabled" is really "the Approve button was replaced by an Unapprove
   * button"; the steps assert it that way rather than looking for a disabled Approve that no
   * longer exists in the DOM.
   */
  get approveButton(): Locator {
    return this.page.getByRole('button', { name: 'Approve', exact: true });
  }

  get unapproveButton(): Locator {
    return this.page.getByRole('button', { name: 'Unapprove', exact: true });
  }

  get rejectButton(): Locator {
    return this.page.getByRole('button', { name: 'Reject', exact: true });
  }

  /**
   * The reviewer-comment box. Required by Reject / Cancel / Unapprove, but NOT by Approve.
   *
   * Stays editable in every status — unlike the header fields, it carries no `disabled` prop, so a
   * reviewer can still annotate a locked (REJ/APP/CAN) invoice.
   */
  get reviewerCommentInput(): Locator {
    return this.page.locator('#reviewer-comment');
  }

  /**
   * Replace the reviewer comment.
   *
   * `fill` clears first, which is what we want: a seeded invoice arrives with its existing note
   * already in the box (hydrated from `reviewComments`), and appending would assert against a
   * concatenation rather than the reason the scenario supplied.
   */
  async setReviewerComment(text: string): Promise<void> {
    await this.reviewerCommentInput.fill(text);
  }

  /** The inline error under the comment box when a decision needing one is attempted without it. */
  get reviewerCommentError(): Locator {
    return fieldError(this.page, '#reviewer-comment');
  }

  /**
   * The Carbon inline error rendered under any field, by the field's `#id`.
   *
   * Which validation messages land here rather than in the page banner is decided by
   * `MESSAGE_KEY_TO_FIELD` (pages/Invoice/messageKeyMap.ts): a mapped key becomes an inline error
   * on its field, an unmapped one falls through to the banner.
   */
  fieldErrorFor(fieldId: string): Locator {
    return fieldError(this.page, fieldId);
  }

  /**
   * Click Unapprove and wait for the status change to land.
   *
   * Unapprove is the mirror of Approve and shares its slot, so once the invoice is back in UNA the
   * Unapprove button is gone and an Approve button stands in its place — that swap is the settle
   * signal, not the button re-enabling.
   */
  async unapprove(): Promise<void> {
    await expect(this.unapproveButton).toBeEnabled();
    await this.unapproveButton.click();
    await expect(this.approveButton).toBeVisible({ timeout: 30_000 });
  }

  /**
   * Click Reject and wait for the status change to land.
   *
   * Unlike Approve — which is swapped out for Unapprove — the Reject button stays in the DOM and
   * becomes DISABLED, because REJ is not in `STATUS_CHANGEABLE`. So the settle signal is the
   * status pill, not the button's presence.
   */
  async reject(): Promise<void> {
    await expect(this.rejectButton).toBeEnabled();
    await this.rejectButton.click();
    await expect(this.statusTag).not.toHaveText('UNA', { timeout: 30_000 });
  }

  /**
   * Click Approve and wait for the status change to land.
   *
   * Settles on the Unapprove button appearing rather than on Approve re-enabling: once the invoice
   * is APP the Approve button is gone from the DOM entirely, so waiting for it would time out.
   */
  async approve(): Promise<void> {
    await expect(this.approveButton).toBeEnabled();
    await this.approveButton.click();
    await expect(this.unapproveButton).toBeVisible({ timeout: 30_000 });
  }

  /** A source-document TagInput's committed chips, e.g. the boom numbers on a seeded invoice. */
  tagChipsFor(fieldId: string): Locator {
    return this.page.locator(`${fieldId}`).locator('xpath=ancestor::div[contains(@class,"tag-input")][1]').locator('.cds--tag');
  }

  // -------------------------------------------------------------------------
  // Invoice group summary + Add New Item modal
  // -------------------------------------------------------------------------

  /**
   * The toolbar trigger that opens the Add New Item modal. Scoped by its own class because the
   * modal's footer button carries the SAME visible label ("Add new item") — a bare
   * getByRole('button', { name: 'Add new item' }) is a strict-mode violation once the modal opens.
   */
  get addNewItemTrigger(): Locator {
    return this.page.locator('.invoice-page__add-line-item-btn');
  }

  /** The Add New Item / Edit group / delete-confirmation modal (Carbon ComposedModal). */
  get modal(): Locator {
    return this.page.getByRole('dialog');
  }

  get modalSubmitButton(): Locator {
    return this.modal.getByRole('button', { name: 'Add new item', exact: true });
  }

  /**
   * Fill and submit the Add New Item modal.
   *
   * Species is chosen BEFORE Grade deliberately: `filteredGradeItems` narrows the Grade list to the
   * pairs valid for the chosen species, so this exercises the same cross-filtering a user hits.
   * The modal's submit stays disabled until `isAddLineItemValid`, so waiting for it to enable also
   * asserts the form was accepted client-side.
   */
  async addLineItem(item: {
    secondarySortLabel: string;
    species: string;
    grade: string;
    pieces: number;
    volume: string;
    price: string;
  }): Promise<void> {
    await this.addNewItemTrigger.click();
    await expect(this.modal).toBeVisible();

    await this.selectFromDropdown('#new-line-secondary-sort', item.secondarySortLabel, this.modal);
    await this.selectFromDropdown('#new-line-species', item.species, this.modal);
    await this.selectFromDropdown('#new-line-grade', item.grade, this.modal);
    await this.modal.locator('#new-line-pieces').fill(String(item.pieces));
    await this.modal.locator('#new-line-volume').fill(item.volume);
    await this.modal.locator('#new-line-price').fill(item.price);

    await expect(this.modalSubmitButton).toBeEnabled();
    await this.modalSubmitButton.click();
    // The modal closes only on a SUCCESSFUL add (`onSuccess` -> `setAddLineItemOpen(false)`), so its
    // disappearance is the proof the POST was accepted rather than rejected into the modal's fields.
    await expect(this.modal).toBeHidden({ timeout: 30_000 });
  }

  /** The read-only "$Amount" preview inside the Add New Item modal. */
  get modalAmountPreview(): Locator {
    return this.modal.locator('#new-line-amount');
  }

  /**
   * The OUTER group-summary table.
   *
   * Not a bare `getByRole('table')`: ResultsTable is expandable and Carbon renders every group's
   * expansion content in the DOM whether or not it is expanded, so each group contributes a nested
   * EditableLineItemsTable. On a seeded invoice with a dozen groups that is a dozen-plus tables and
   * any direct assertion on the locator is a strict-mode violation. Filtering on the "Group number"
   * column header — which only the outer table has — picks it unambiguously.
   */
  get groupSummaryTable(): Locator {
    return this.page
      .getByRole('table')
      .filter({ has: this.page.getByRole('columnheader', { name: 'Group number', exact: true }) });
  }

  /**
   * Top-level group rows only.
   *
   * ResultsTable is a Carbon expandable DataTable: every group row is followed by a sibling
   * `tr.cds--expandable-row` holding the nested line-item table, and an empty grid renders a single
   * placeholder row instead. Counting `tbody tr` would therefore count expansion rows and the
   * placeholder as results — this filters to rows that carry the expansion toggle, which only
   * genuine group rows do.
   */
  get groupRows(): Locator {
    return this.groupSummaryTable
      .locator('tbody tr')
      .filter({ has: this.page.getByRole('button', { name: /Expand current row|Collapse current row/ }) });
  }

  /** The group row for a species + secondary-sort pairing. */
  groupRowFor(species: string, secondarySort: string): Locator {
    return this.groupRows.filter({
      has: this.page.getByRole('cell', { name: species, exact: true }),
    }).filter({
      has: this.page.getByRole('cell', { name: secondarySort, exact: true }),
    });
  }

  /** Text of every cell in one row, in column order. */
  async rowCells(row: Locator): Promise<string[]> {
    return (await row.locator('td').allInnerTexts()).map((t) => t.trim());
  }

  /** The bold "Invoice totals" footer row under the group summary. */
  get invoiceTotalsRow(): Locator {
    return this.page.locator('.invoice-page__line-items-totals-row');
  }
}
