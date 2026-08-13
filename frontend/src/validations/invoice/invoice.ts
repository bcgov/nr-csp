// Client-side field validation mirroring the backend request-DTO constraints.
// Validators push message KEYS via MessageCollector (text lives in
// `validations/messages.ts`) and return a ValidationResult, matching the
// report-page validators (e.g. validateR07). The page maps the resulting
// message keys to fields via `splitMessages` + a key->field map.

import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

const INVOICE_NUMBER_PATTERN = /^[A-Z0-9-]+$/;
const INVOICE_TYPE_PATTERN = /^[A-Z]+$/;
const LOCATION_PATTERN = /^\d{2}$/;
const SUBMITTED_BY_PATTERN = /^(Buyer|Seller)$/;

const INVOICE_NUMBER_MAX_LENGTH = 15;
const CLIENT_PRIMARY_SORT_CODE_MAX_LENGTH = 100;
const REVIEWER_COMMENT_MAX_LENGTH = 4000;
const SUBMITTER_COMMENT_MAX_LENGTH = 4000;

export interface InvoiceFieldValues {
  invNumber: string;
  invDate: string; // ISO yyyy-MM-dd, or '' when unset
  invType: string;
  submittedBy: string;
  submitterLocation: string;
  otherClientLocation: string;
  clientPrimarySortCode: string;
  reviewerComment: string;
  submitterComment: string;
}

export function validate(values: InvoiceFieldValues): ValidationResult {
  const messages = new MessageCollector();
  const t = (s: string) => s.trim();

  // invNumber — @NotBlank + ^[A-Z0-9-]+$ + max length
  if (!t(values.invNumber)) {
    messages.addError('invoice.client.invnumber.required.error');
  } else if (!INVOICE_NUMBER_PATTERN.test(t(values.invNumber))) {
    messages.addError('invoice.client.invnumber.pattern.error');
  } else if (t(values.invNumber).length > INVOICE_NUMBER_MAX_LENGTH) {
    messages.addError('invoice.client.invnumber.maxlength.error');
  }

  // invoiceDate — @NotNull
  if (!t(values.invDate)) {
    messages.addError('invoice.client.invdate.required.error');
  }

  // invType — @NotBlank + ^[A-Z]+$
  if (!t(values.invType)) {
    messages.addError('invoice.client.invtype.required.error');
  } else if (!INVOICE_TYPE_PATTERN.test(t(values.invType))) {
    messages.addError('invoice.client.invtype.pattern.error');
  }

  // submittedBy — @NotBlank + ^(Buyer|Seller)$
  if (!t(values.submittedBy)) {
    messages.addError('invoice.client.submittedby.required.error');
  } else if (!SUBMITTED_BY_PATTERN.test(t(values.submittedBy))) {
    messages.addError('invoice.client.submittedby.pattern.error');
  }

  // submitterLocation — @NotBlank + ^\d{2}$ (typed field with a visible slot)
  if (!t(values.submitterLocation)) {
    messages.addError('invoice.client.submitterlocation.required.error');
  } else if (!LOCATION_PATTERN.test(t(values.submitterLocation))) {
    messages.addError('invoice.client.submitterlocation.pattern.error');
  }

  // otherClientLocation — optional, ^\d{2}$ (typed field with a visible slot)
  if (t(values.otherClientLocation) && !LOCATION_PATTERN.test(t(values.otherClientLocation))) {
    messages.addError('invoice.client.otherlocation.pattern.error');
  }

  // clientPrimarySortCode — optional, max length only
  if (values.clientPrimarySortCode.length > CLIENT_PRIMARY_SORT_CODE_MAX_LENGTH) {
    messages.addError('invoice.client.clientprimarysortcode.maxlength.error');
  }

  // reviewerComment — optional, max length only
  if (values.reviewerComment.length > REVIEWER_COMMENT_MAX_LENGTH) {
    messages.addError('invoice.client.reviewercomment.maxlength.error');
  }

  // submitterComment — optional, max length only
  if (values.submitterComment.length > SUBMITTER_COMMENT_MAX_LENGTH) {
    messages.addError('invoice.client.submittercomment.maxlength.error');
  }

  return messages.result();
}

// Add New Line Item — structural checks on the free-text numeric inputs.
export interface LineItemFieldValues {
  pieces: string;
  volume: string;
  price: string;
  /** Invoice type code — sign rules are skipped for adjustments ('ADJ'). */
  invType: string;
}

export const INVTYPE_ADJUST = 'ADJ';

/**
 * A line item's `$Amount` preview — volume × price, rounded to 2dp.
 *
 * @param volume  the line's volume
 * @param price   the line's price
 * @param invType the parent invoice's type code, e.g. 'ADJ'
 * @returns the signed amount rounded to 2 decimal places
 */
export const computeLineAmount = (volume: number, price: number, invType: string): number => {
  const effectiveVolume = invType === INVTYPE_ADJUST && volume < 0 && price < 0 ? Math.abs(volume) : volume;
  return Math.round(effectiveVolume * price * 100) / 100;
};

export function validateLineItem(values: LineItemFieldValues): ValidationResult {
  const messages = new MessageCollector();
  const t = (s: string) => s.trim();
  const isAdjust = values.invType === INVTYPE_ADJUST;

  // Pieces — whole number; and, unless this is an adjustment, greater than zero
  // (matches InvoiceLineValidator's negative-or-zero rule).
  if (t(values.pieces)) {
    const n = Number(values.pieces);
    if (!Number.isInteger(n)) {
      messages.addError('invoice.client.pieces.integer.error');
    } else if (!isAdjust && n <= 0) {
      messages.addError('invoice.client.pieces.positive.error');
    }
  }

  // Volume — numeric; and, unless an adjustment, not negative.
  if (t(values.volume)) {
    const n = Number(values.volume);
    if (!Number.isFinite(n)) {
      messages.addError('invoice.client.volume.numeric.error');
    } else if (!isAdjust && n < 0) {
      messages.addError('invoice.client.volume.negative.error');
    }
  }

  // Price — numeric; and, unless an adjustment, not negative.
  if (t(values.price)) {
    const n = Number(values.price);
    if (!Number.isFinite(n)) {
      messages.addError('invoice.client.price.numeric.error');
    } else if (!isAdjust && n < 0) {
      messages.addError('invoice.client.price.negative.error');
    }
  }

  return messages.result();
}
