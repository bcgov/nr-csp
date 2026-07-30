import { MessageCollector, type ValidationResult } from '@/validations/validationResult';

/**
 * The editable submission-metadata values validated before submit. Mirrors the
 * subset of {@link EditableFields} that is persisted; email/telephone are not
 * validated here (they are display-only until the schema stores them).
 */
export interface SubmissionMetadataValues {
  submissionClientNumber: string;
  submissionClientLocnCode: string;
  monthComplete: string;
  sellerSubmission: string;
}

/** Maps each client-side message key to the metadata field it highlights. */
export const SUBMISSION_METADATA_KEY_TO_FIELD: Record<string, string> = {
  'submission.client.clientnumber.required.error': 'submissionClientNumber',
  'submission.client.clientnumber.pattern.error': 'submissionClientNumber',
  'submission.client.locationcode.required.error': 'submissionClientLocnCode',
  'submission.client.locationcode.pattern.error': 'submissionClientLocnCode',
  'submission.client.monthcomplete.required.error': 'monthComplete',
  'submission.client.monthcomplete.pattern.error': 'monthComplete',
  'submission.client.sellersubmission.required.error': 'sellerSubmission',
  'submission.client.sellersubmission.pattern.error': 'sellerSubmission',
};

/**
 * Client-side structural validation of the editable submission-metadata fields,
 * run on submit so an offending field surfaces an inline error (the same
 * required/pattern approach the report filters use). Backend business rules
 * (client existence, seller/submitter matching, …) still run server-side.
 */
export function validateSubmissionMetadata(values: SubmissionMetadataValues): ValidationResult {
  const messages = new MessageCollector();

  const clientNumber = values.submissionClientNumber.trim();
  if (!clientNumber) {
    messages.addError('submission.client.clientnumber.required.error');
  } else if (!/^\d{8}$/.test(clientNumber)) {
    messages.addError('submission.client.clientnumber.pattern.error');
  }

  const locnCode = values.submissionClientLocnCode.trim();
  if (!locnCode) {
    messages.addError('submission.client.locationcode.required.error');
  } else if (!/^\d{2}$/.test(locnCode)) {
    messages.addError('submission.client.locationcode.pattern.error');
  }

  const monthComplete = values.monthComplete.trim();
  if (!monthComplete) {
    messages.addError('submission.client.monthcomplete.required.error');
  } else if (!/^[YN]$/i.test(monthComplete)) {
    messages.addError('submission.client.monthcomplete.pattern.error');
  }

  const sellerSubmission = values.sellerSubmission.trim();
  if (!sellerSubmission) {
    messages.addError('submission.client.sellersubmission.required.error');
  } else if (!/^[YN]$/i.test(sellerSubmission)) {
    messages.addError('submission.client.sellersubmission.pattern.error');
  }

  return messages.result();
}
