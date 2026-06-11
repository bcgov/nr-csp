import { useState, useEffect } from 'react';
import type React from 'react';
import {
  useCreateSortCodeMutation,
  useUpdateSortCodeMutation,
  extractApiErrorMessage,
  type SortCodeResponse,
} from '@/services/sortcode.service';

interface FormValues {
  sortCode: string;
  description: string;
  effectiveDate: string;
  expiryDate: string;
}

interface FormErrors {
  sortCode?: string;
  description?: string;
  effectiveDate?: string;
  expiryDate?: string;
}

const empty: FormValues = { sortCode: '', description: '', effectiveDate: '', expiryDate: '' };

function validate(values: FormValues, mode: 'add' | 'edit'): FormErrors {
  const errors: FormErrors = {};
  if (mode === 'add') {
    if (!values.sortCode.trim()) errors.sortCode = 'Sort code is required.';
    else if (values.sortCode.trim().length > 1) errors.sortCode = 'Sort code must be at most 1 character.';
  }
  if (!values.description.trim()) errors.description = 'Description is required.';
  if (!values.effectiveDate) errors.effectiveDate = 'Effective date is required.';
  if (!values.expiryDate) errors.expiryDate = 'Expiry date is required.';
  if (!errors.effectiveDate && !errors.expiryDate && values.effectiveDate && values.expiryDate) {
    if (values.effectiveDate > values.expiryDate) {
      errors.expiryDate = 'Expiry date must not be before effective date.';
    }
  }
  return errors;
}

export function useSortCodeForm(
  open: boolean,
  mode: 'add' | 'edit',
  initialValues: SortCodeResponse | undefined,
  onSuccess: (code: string) => void,
) {
  const [values, setValues] = useState<FormValues>(empty);
  const [errors, setErrors] = useState<FormErrors>({});

  const createMutation = useCreateSortCodeMutation();
  const updateMutation = useUpdateSortCodeMutation();
  const mutation = mode === 'add' ? createMutation : updateMutation;

  useEffect(() => {
    if (open) {
      setValues(
        initialValues
          ? {
              sortCode: initialValues.sortCode,
              description: initialValues.description,
              effectiveDate: initialValues.effectiveDate,
              expiryDate: initialValues.expiryDate,
            }
          : empty,
      );
      setErrors({});
      createMutation.reset();
      updateMutation.reset();
    }
    // Intentionally depend only on `open`: re-initialise state every time the modal opens, not on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const set = (field: keyof FormValues) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setValues((v) => ({ ...v, [field]: e.target.value }));

  const setValue = (field: keyof FormValues, value: string) => setValues((v) => ({ ...v, [field]: value }));

  const handleSubmit = () => {
    const errs = validate(values, mode);
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setErrors({});

    const code = values.sortCode.toUpperCase();

    if (mode === 'add') {
      createMutation.mutate(
        {
          sortCode: code,
          description: values.description,
          effectiveDate: values.effectiveDate,
          expiryDate: values.expiryDate,
        },
        { onSuccess: () => onSuccess(code) },
      );
    } else {
      const existingCode = (initialValues?.sortCode ?? '').toUpperCase();
      updateMutation.mutate(
        {
          code: existingCode,
          req: { description: values.description, effectiveDate: values.effectiveDate, expiryDate: values.expiryDate },
        },
        { onSuccess: () => onSuccess(existingCode) },
      );
    }
  };

  const apiErrorMessage = mutation.isError && mutation.error ? extractApiErrorMessage(mutation.error) : null;

  return { values, errors, set, setValue, handleSubmit, isPending: mutation.isPending, apiErrorMessage };
}
