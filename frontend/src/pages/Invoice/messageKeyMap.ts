// Maps validator messageKeys (from the backend bundle) to the form field
// they should highlight. Anything not in this map flows through to the
// page-level error banner.
export const MESSAGE_KEY_TO_FIELD: Record<string, string> = {
  // Invoice header — date
  'invoice.date.required.error': 'invDate',
  'invoice.date.in.future.error': 'invDate',
  'invoice.month.completed.warning': 'invDate',
  // Replaces / Adjusts invoice numbers
  'invoice.replace.invoicenumber.error': 'replaceInvNum',
  'invoice.replace.with.itself.error': 'replaceInvNum',
  'invoice.morethanmax.replace.invoicenum.error': 'replaceInvNum',
  'invoice.adjust.invoicenumber.error': 'adjustInvNum',
  'invoice.adjust.with.itself.error': 'adjustInvNum',
  'invoice.morethanmax.adjust.invoicenum.error': 'adjustInvNum',
  'invoice.validation.adjustedInvoiceCancelled': 'adjustInvNum',
  // Submitter / Submitted-by
  'invoice.otherparty.buyer.submission.error': 'submittedBy',
  'invoice.otherparty.seller.submission.error': 'submittedBy',
  'invoice.submitter.not.equal.seller.client.number.error': 'submittingClientNumber',
  'invoice.submitter.not.equal.seller.client.location.error': 'submittingClientLocation',
  // Other-party
  'invoice.otherparty.client.location.invalid.error': 'otherClientLocation',
  'invoice.manual.other.party.name.error': 'otherClientName',
  'invoice.otherparty.buyer.name.required.error': 'otherClientName',
  'invoice.otherparty.seller.name.required.error': 'otherClientName',
  // Invoice type / maturity / sort
  'invoice.type.invalid.error': 'invType',
  'invoice.type.invalid.submitter': 'invType',
  'invoice.type.not.saleorpurchase.warning': 'invType',
  'invoice.maturity.invalid.error': 'maturity',
  'invoice.fob.required.error': 'fobCode',
  'invoice.primary.sortcode.invalid.error': 'primarySortCode',
  // Source-document refs
  'invoice.morethan.Max.boomnumbers.error': 'boomNumbers',
  'invoice.morethan.Max.timbermarks.error': 'timberMarks',
  'invoice.morethan.Max.weighslips.error': 'weighSlips',
  'invoice.boomnumber.duplicate.warning': 'boomNumbers',
  // Approve / Reject / Reviewer
  'invoice.reject.need.reviewer.comment.error': 'reviewerComment',
  'invoice.reviewer.notes.update.warning': 'reviewerComment',
  // Duplicate invoice number — show against the invoice number field
  'invoice.number.duplicate.same.type.warning': 'invNumber',
};

// Client-side validation maps — translate the message keys produced by the
// inline validators (`validations/invoice/invoice.ts`) into the form field
// they highlight, via `splitMessages`. Mirrors the report pages' setup.
export const CLIENT_MESSAGE_KEY_TO_FIELD: Record<string, string> = {
  'invoice.client.invnumber.required.error': 'invNumber',
  'invoice.client.invnumber.pattern.error': 'invNumber',
  'invoice.client.invdate.required.error': 'invDate',
  'invoice.client.invtype.required.error': 'invType',
  'invoice.client.invtype.pattern.error': 'invType',
  'invoice.client.submittedby.required.error': 'submittedBy',
  'invoice.client.submittedby.pattern.error': 'submittedBy',
  'invoice.client.submitterlocation.required.error': 'submittingClientLocation',
  'invoice.client.submitterlocation.pattern.error': 'submittingClientLocation',
  'invoice.client.otherlocation.pattern.error': 'otherClientLocation',
};

export const CLIENT_LINE_ITEM_MESSAGE_KEY_TO_FIELD: Record<string, string> = {
  'invoice.client.pieces.integer.error': 'pieces',
  'invoice.client.pieces.positive.error': 'pieces',
  'invoice.client.volume.numeric.error': 'volume',
  'invoice.client.volume.negative.error': 'volume',
  'invoice.client.price.numeric.error': 'price',
  'invoice.client.price.negative.error': 'price',
};

// Parallel map used only when validation comes back from the
// `POST /api/invoices/{id}/line-items` (or update) endpoint — the same
// `messageKey`s would be ambiguous on a PUT invoice save (could be about
// any existing line), so they live in their own map keyed by the Add New
// Line Item form's field state names.
export const LINE_ITEM_MESSAGE_KEY_TO_FIELD: Record<string, string> = {
  'invoice.lineitem.missing.error': 'secondarySort',
  'invoice.secondry.sortcode.invalid.error': 'secondarySort',
  'invoice.species.grade.combination.error': 'grade',
  'invoice.grade.invalid.required.error': 'grade',
  'invoice.grade.z.warning': 'grade',
  'invoice.numberof.pieces.negative.or.zero.error': 'pieces',
  'invoice.volume.negative.value.error': 'volume',
  'invoice.volume.zero.value.warning': 'volume',
  'invoice.price.negative.value.error': 'price',
  'invoice.price.zero.value.warning': 'price',
};
