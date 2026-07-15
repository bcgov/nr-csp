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
  submissionId: number | null;
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
 */
export const validateSubmissionBusiness = (file: File): Promise<SubmissionValidationResponse> =>
  apiClient
    .post<SubmissionValidationResponse>('/submissions/validate/business', buildFormData(file))
    .then(({ data }) => data);

/**
 * Business-validate and persist an uploaded submission. Returns 200 with the new
 * `submissionId` when saved, or 422 (thrown) when any invoice is rejected — the
 * error body is a {@link SubmissionSubmitResponse}.
 */
export const submitSubmission = (file: File): Promise<SubmissionSubmitResponse> =>
  apiClient.post<SubmissionSubmitResponse>('/submissions/submit', buildFormData(file)).then(({ data }) => data);

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
