import { useEffect } from 'react';
import { useDeleteFlatPriceConversionMutation, extractApiErrorMessage } from '@/services/flatPriceConversion.service';

export function useFlatPriceConversionDelete(open: boolean, onSuccess: (id: number) => void) {
  const mutation = useDeleteFlatPriceConversionMutation();

  useEffect(() => {
    if (open) mutation.reset();
    // Intentionally depend only on `open`: reset mutation state every time the modal opens, not on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const handleConfirm = (id: number) => {
    mutation.mutate(id, {
      onSuccess: () => onSuccess(id),
    });
  };

  const apiErrorMessage = mutation.isError && mutation.error ? extractApiErrorMessage(mutation.error) : null;

  return { handleConfirm, isPending: mutation.isPending, apiErrorMessage };
}
