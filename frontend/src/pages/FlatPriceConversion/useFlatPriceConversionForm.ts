import { useState, useEffect } from 'react';
import type React from 'react';
import {
  useCreateFlatPriceConversionMutation,
  useUpdateFlatPriceConversionMutation,
  extractApiErrorMessage,
  type FlatPriceConversionResponse,
} from '@/services/flatPriceConversion.service';

export interface FormValues {
  species: string;
  grade: string;
  sortCode: string;
  maturity: string;
  flatPriceConversion: string;
  effectiveDate: string;
  expiryDate: string;
}

export interface FormErrors {
  species?: string;
  grade?: string;
  sortCode?: string;
  maturity?: string;
  flatPriceConversion?: string;
  effectiveDate?: string;
  expiryDate?: string;
}

const empty: FormValues = {
  species: '',
  grade: '',
  sortCode: '',
  maturity: '',
  flatPriceConversion: '',
  effectiveDate: '',
  expiryDate: '',
};

export function validateFlatPriceConversionForm(values: FormValues): FormErrors {
  const errors: FormErrors = {};

  if (!values.species.trim()) errors.species = 'Species is required.';
  if (!values.grade.trim()) errors.grade = 'Grade is required.';
  if (!values.sortCode.trim()) errors.sortCode = 'Sort code is required.';
  if (!values.maturity.trim()) errors.maturity = 'Maturity is required.';

  if (!values.flatPriceConversion.trim()) {
    errors.flatPriceConversion = 'Flat price conversion is required.';
  } else {
    const num = Number(values.flatPriceConversion);
    if (isNaN(num) || !Number.isInteger(num) || num < 1 || num > 999) {
      errors.flatPriceConversion = 'Must be a number between 1 and 999.';
    }
  }

  if (!values.effectiveDate) errors.effectiveDate = 'Effective date is required.';

  if (values.expiryDate && !errors.effectiveDate && values.effectiveDate > values.expiryDate) {
    errors.expiryDate = 'Expiry date must not be before effective date.';
  }

  return errors;
}

export function useFlatPriceConversionForm(
  open: boolean,
  mode: 'add' | 'edit',
  modellingCode: string,
  initialValues: FlatPriceConversionResponse | undefined,
  onSuccess: (row: FlatPriceConversionResponse) => void,
) {
  const [values, setValues] = useState<FormValues>(empty);
  const [errors, setErrors] = useState<FormErrors>({});

  const createMutation = useCreateFlatPriceConversionMutation();
  const updateMutation = useUpdateFlatPriceConversionMutation();
  const mutation = mode === 'add' ? createMutation : updateMutation;

  useEffect(() => {
    if (open) {
      setValues(
        initialValues
          ? {
              species: initialValues.species,
              grade: initialValues.grade,
              sortCode: initialValues.sortCode,
              maturity: initialValues.maturity,
              flatPriceConversion: String(initialValues.flatPriceConversion),
              effectiveDate: initialValues.effectiveDate,
              expiryDate: initialValues.expiryDate ?? '',
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
    const errs = validateFlatPriceConversionForm(values);
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setErrors({});

    const { species, grade, sortCode, maturity, flatPriceConversion, effectiveDate, expiryDate } = values;
    const details = {
      species,
      grade,
      sortCode,
      maturity,
      flatPriceConversion: Number(flatPriceConversion),
      effectiveDate,
      expiryDate: expiryDate || null,
    };

    if (mode === 'add') {
      createMutation.mutate({ modellingCode, details }, { onSuccess: (row) => onSuccess(row) });
    } else {
      updateMutation.mutate(
        {
          id: initialValues!.id,
          req: { revisionCount: initialValues!.revisionCount, details },
        },
        { onSuccess: (row) => onSuccess(row) },
      );
    }
  };

  const apiErrorMessage = mutation.isError && mutation.error ? extractApiErrorMessage(mutation.error) : null;

  return { values, errors, set, setValue, handleSubmit, isPending: mutation.isPending, apiErrorMessage };
}
