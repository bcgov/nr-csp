import { ALL_MESSAGES } from './messages';

export type MessageType = 'ERROR' | 'WARNING';

const resolveMessage = (messageKey: string, args?: unknown[]): string => {
  const template = ALL_MESSAGES[messageKey];
  if (template == null) return messageKey;
  if (!args || args.length === 0) return template;
  return template.replace(/\{(\d+)\}/g, (match, index) => {
    const value = args[Number(index)];
    return value == null ? match : String(value);
  });
};

export interface ValidationMessage {
  messageKey: string;
  message: string;
  type: MessageType;
}

export class ValidationResult {
  constructor(public readonly messages: ValidationMessage[]) {}

  hasErrors(): boolean {
    return this.messages.some((m) => m.type === 'ERROR');
  }

  get errors(): ValidationMessage[] {
    return this.messages.filter((m) => m.type === 'ERROR');
  }

  get warnings(): ValidationMessage[] {
    return this.messages.filter((m) => m.type === 'WARNING');
  }
}

export class MessageCollector {
  private readonly items: ValidationMessage[] = [];

  addError(messageKey: string, args?: unknown[]): void {
    this.items.push({ messageKey, message: resolveMessage(messageKey, args), type: 'ERROR' });
  }

  addWarning(messageKey: string, args?: unknown[]): void {
    this.items.push({ messageKey, message: resolveMessage(messageKey, args), type: 'WARNING' });
  }

  result(): ValidationResult {
    return new ValidationResult(this.items);
  }
}

export type SplitMessages = {
  fieldErrors: Record<string, string>;
  formErrors: string[];
  warnings: string[];
};

/**
 * Splits validation messages into:
 *  - `fieldErrors`: ERROR messages whose key maps to a field (via `keyToField`),
 *  - `formErrors`: ERROR messages with no field mapping (page/form-level banner),
 *  - `warnings`: WARNING messages.
 *
 * Shared by the report pages and the Invoice page to drive inline + banner
 * display. Accepts any `{ messageKey, message, type }` shape, so both a
 * `ValidationResult`'s `messages` and a server `ValidationMessageResponse[]`
 * can be passed in.
 */
export const splitMessages = (messages: ValidationMessage[], keyToField: Record<string, string>): SplitMessages => {
  const fieldErrors: Record<string, string> = {};
  const formErrors: string[] = [];
  const warnings: string[] = [];

  for (const m of messages) {
    if (m.type === 'WARNING') {
      warnings.push(m.message);
      continue;
    }
    const field = keyToField[m.messageKey];
    if (field) {
      if (!fieldErrors[field]) fieldErrors[field] = m.message;
    } else {
      formErrors.push(m.message);
    }
  }

  return { fieldErrors, formErrors, warnings };
};
