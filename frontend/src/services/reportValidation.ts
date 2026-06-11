import { isValidationErrorResponse, type ValidationMessageResponse } from '@/services/invoice.service';

export type { ValidationMessageResponse };

export const parseReportValidationError = async (error: unknown): Promise<ValidationMessageResponse[]> => {
  const data = (error as { response?: { data?: unknown } })?.response?.data;
  if (data == null) return [];

  let parsed: unknown = data;
  if (data instanceof Blob) {
    try {
      parsed = JSON.parse(await data.text());
    } catch {
      return [];
    }
  }

  return isValidationErrorResponse(parsed) ? parsed.errors : [];
};
