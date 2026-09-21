import { apiClient } from '@/config/api/request';
import { type ValidationMessageResponse } from '@/services/invoice.service';

/** One invoice row parsed from an uploaded submission (Invoice Details table). */
export interface ParsedInvoice {
  index: number;
  invoiceNumber: string | null;
  invoiceDate: string | null;
  invoiceType: string | null;
  sellerClientNumber: string | null;
  buyerClientNumber: string | null;
  maturity: string | null;
  locationFOB: string | null;
  totalAmount: number | null;
  totalVolume: number | null;
  totalPieces: number | null;
  // Supplementary detail fields, shown in the expanded row's "Invoice details" card.
  replacesInvoiceNumbers: string | null;
  adjustsInvoiceNumbers: string | null;
  sellerClientLocnCode: string | null;
  buyerClientLocnCode: string | null;
  otherPartyName: string | null;
  otherPartyCity: string | null;
  otherPartyProvState: string | null;
  primarySortCode: string | null;
  clientPrimarySortCode: string | null;
  boomNumbers: string | null;
  timberMarks: string | null;
  weighSlipNumbers: string | null;
  submitterNotes: string | null;
}

/** One line-item row parsed from an uploaded submission (Invoice Line Items table). */
export interface ParsedLineItem {
  invoiceIndex: number;
  lineIndex: number;
  invoiceNumber: string | null;
  species: string | null;
  grade: string | null;
  secondarySortCode: string | null;
  clientSecondarySortCode: string | null;
  numberOfPieces: number | null;
  volume: number | null;
  price: number | null;
}

/** Parsed submission content used to populate the upload form. */
export interface ParsedSubmission {
  email: string | null;
  telephone: string | null;
  monthComplete: string | null;
  sellerSubmission: string | null;
  submissionClientNumber: string | null;
  submissionClientLocnCode: string | null;
  invoices: ParsedInvoice[];
  lineItems: ParsedLineItem[];
}

/** Response of POST /api/submissions/parse (structural validation + parsed content). */
export interface SubmissionParseResponse {
  valid: boolean;
  code: string;
  message: string;
  errors: ValidationMessageResponse[];
  submission: ParsedSubmission | null;
}

/** Response of the validate endpoints (structural / business). */
export interface SubmissionValidationResponse {
  valid: boolean;
  code: string;
  message: string;
  acceptedInvoices: string[];
  rejectedInvoices: string[];
  errors: ValidationMessageResponse[];
}

/** Response of POST /api/submissions/submit (persist). */
export interface SubmissionSubmitResponse {
  valid: boolean;
  code: string;
  message: string;
  // Internal csp submission id — keys the invoice-comments sub-resource.
  submissionId: number | null;
  // Business submission number — what the submission detail page is keyed on.
  submissionNumber: number | null;
  acceptedInvoices: string[];
  rejectedInvoices: string[];
  errors: ValidationMessageResponse[];
}

const buildFormData = (file: File): FormData => {
  const form = new FormData();
  form.append('file', file);
  return form;
};

/**
 * The editable submission metadata the business rules actually depend on. Sent
 * alongside the file so the backend overlays it on the parsed document before
 * validating — which is what lets the form re-validate an edited field without
 * re-uploading. A blank value leaves the parsed value in place.
 */
export interface SubmissionMetadataEdits {
  submissionClientNumber: string;
  submissionClientLocnCode: string;
  monthComplete: string;
  sellerSubmission: string;
}

/**
 * The submission metadata the user can edit before submitting. Sent alongside the
 * file so the backend overrides the parsed values before validating and saving.
 * Email/telephone originate in the ESF envelope but are editable on the form and
 * are persisted on the submission, so they are sent too; a blank value lets the
 * backend fall back to the parsed envelope value. No business rule reads them, so
 * they are not part of {@link SubmissionMetadataEdits}.
 */
export interface SubmissionEdits extends SubmissionMetadataEdits {
  email: string;
  telephone: string;
}

/** Appends the business-relevant metadata edits as multipart form fields. */
const appendMetadataEdits = (form: FormData, edits: SubmissionMetadataEdits): void => {
  form.append('submissionClientNumber', edits.submissionClientNumber);
  form.append('submissionClientLocnCode', edits.submissionClientLocnCode);
  form.append('monthComplete', edits.monthComplete);
  form.append('sellerSubmission', edits.sellerSubmission);
};

/**
 * Parse (and structurally validate) an uploaded XML file. On a structural
 * failure the backend responds 422; the response body is still a
 * {@link SubmissionParseResponse} (with `valid: false` and populated `errors`),
 * so callers should read it off the thrown Axios error's `response.data`.
 */
export const parseSubmission = (file: File): Promise<SubmissionParseResponse> =>
  apiClient.post<SubmissionParseResponse>('/submissions/parse', buildFormData(file)).then(({ data }) => data);

/**
 * Run business-rule validation on an uploaded XML file. Returns 200 when fully
 * accepted and 422 when rejected; both bodies are a
 * {@link SubmissionValidationResponse}.
 *
 * `edits` overlays the user's editable metadata on the parsed document before the
 * rules run, so the form can re-validate after a field is corrected. Omit it to
 * validate the file exactly as uploaded (the first run, straight after parse).
 */
export const validateSubmissionBusiness = (
  file: File,
  edits?: SubmissionMetadataEdits,
): Promise<SubmissionValidationResponse> => {
  const form = buildFormData(file);
  if (edits) appendMetadataEdits(form, edits);
  return apiClient.post<SubmissionValidationResponse>('/submissions/validate/business', form).then(({ data }) => data);
};

/**
 * Business-validate and persist an uploaded submission. Returns 200 with the new
 * `submissionId` when saved, or 422 (thrown) when any invoice is rejected — the
 * error body is a {@link SubmissionSubmitResponse}.
 */
export const submitSubmission = (file: File, edits: SubmissionEdits): Promise<SubmissionSubmitResponse> => {
  const form = buildFormData(file);
  appendMetadataEdits(form, edits);
  form.append('email', edits.email);
  form.append('telephone', edits.telephone);
  return apiClient.post<SubmissionSubmitResponse>('/submissions/submit', form).then(({ data }) => data);
};

/**
 * Extracts a {@link SubmissionParseResponse} / {@link SubmissionValidationResponse}
 * from an Axios error body. The validate/parse endpoints return their normal
 * envelope on a 422, so a rejected submission surfaces as a thrown error whose
 * `response.data` is the envelope. Returns null for non-envelope errors (e.g. a
 * network failure or a 401).
 */
export const submissionErrorBody = <T extends { errors?: ValidationMessageResponse[] }>(error: unknown): T | null => {
  const data = (error as { response?: { data?: unknown } })?.response?.data;
  if (data && typeof data === 'object' && 'errors' in data) return data as T;
  return null;
};
