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

export interface InvoiceFieldValues {
  invNumber: string;
  invDate: string; // ISO yyyy-MM-dd, or '' when unset
  invType: string;
  submittedBy: string;
  submitterLocation: string;
  otherClientLocation: string;
}

export function validate(values: InvoiceFieldValues): ValidationResult {
  const messages = new MessageCollector();
  const t = (s: string) => s.trim();

  // invNumber — @NotBlank + ^[A-Z0-9-]+$
  if (!t(values.invNumber)) {
    messages.addError('invoice.client.invnumber.required.error');
  } else if (!INVOICE_NUMBER_PATTERN.test(t(values.invNumber))) {
    messages.addError('invoice.client.invnumber.pattern.error');
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

const INVTYPE_ADJUST = 'ADJ';

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
