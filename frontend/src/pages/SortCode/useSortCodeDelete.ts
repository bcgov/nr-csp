import { useEffect } from 'react';
import { useDeleteSortCodeMutation, extractApiErrorMessage } from '@/services/sortcode.service';

export function useSortCodeDelete(open: boolean, onSuccess: (code: string) => void) {
  const mutation = useDeleteSortCodeMutation();

  useEffect(() => {
    if (open) mutation.reset();
    // Intentionally depend only on `open`: reset mutation state every time the modal opens, not on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const handleConfirm = (sortCode: string) => {
    mutation.mutate(sortCode, {
      onSuccess: () => onSuccess(sortCode),
    });
  };

  const apiErrorMessage = mutation.isError && mutation.error ? extractApiErrorMessage(mutation.error) : null;

  return { handleConfirm, isPending: mutation.isPending, apiErrorMessage };
}
