import { useEffect } from 'react';

import { calculateEndDateFromTimeFrame } from '@/utils/report';

interface UseEndDateAutoFillOptions {
  startDate: Date | null;
  timeFrame: string;
  setEndDate: (date: Date) => void;
  clearEndDateError: () => void;
}

/**
 * Auto-fills end date from start date + time frame whenever either changes.
 * The end-date field stays fully editable afterward — this only re-fires
 * (overwriting again) if start date or time frame subsequently change.
 */
export function useEndDateAutoFill({
  startDate,
  timeFrame,
  setEndDate,
  clearEndDateError,
}: UseEndDateAutoFillOptions): void {
  useEffect(() => {
    if (startDate && timeFrame) {
      setEndDate(calculateEndDateFromTimeFrame(startDate, timeFrame));
      clearEndDateError();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [startDate, timeFrame]);
}
