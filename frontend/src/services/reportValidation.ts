import { isValidationErrorResponse, type ValidationMessageResponse } from '@/services/invoice.service';

export type { ValidationMessageResponse };

// Reports are requested with responseType 'blob', so an error body arrives as a
// Blob that has to be read back into JSON before it can be inspected.
const readErrorPayload = async (error: unknown): Promise<unknown> => {
  const data = (error as { response?: { data?: unknown } })?.response?.data;
  if (data == null) return null;
  if (data instanceof Blob) {
    try {
      return JSON.parse(await data.text());
    } catch {
      return null;
    }
  }
  return data;
};

export const parseReportValidationError = async (error: unknown): Promise<ValidationMessageResponse[]> => {
  const parsed = await readErrorPayload(error);
  return isValidationErrorResponse(parsed) ? parsed.errors : [];
};

/**
 * The `message` of a plain `ApiError` body (`{ code, message }`) — the shape the
 * backend returns for request-level rejections that carry no structured
 * validation messages. Undefined when the body has no usable message.
 */
export const parseReportErrorMessage = async (error: unknown): Promise<string | undefined> => {
  const parsed = await readErrorPayload(error);
  const message = (parsed as { message?: unknown } | null)?.message;
  return typeof message === 'string' && message.trim() ? message : undefined;
};
