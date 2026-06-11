import { type ReactNode } from 'react';

import AutoCompleteInput from '@/components/Form/AutoCompleteInput';
import { getClientsByName, type ClientLocationResponse } from '@/services/search.service';

export type { ClientLocationResponse };

type Props = {
  id: string;
  titleText: ReactNode;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
  hideLabel?: boolean;
  /**
   * Controlled selection. When provided (e.g. the Invoice page), the field
   * mirrors this value exactly — selecting sets it via `onSelect`, clearing
   * sets it to null, and external changes (parent hydration, a paired
   * name/number field) re-seed the input via the `key` remount below. Leave
   * undefined to use the field uncontrolled (the report pages).
   */
  selectedClient?: ClientLocationResponse | null;
  onSelect: (client: ClientLocationResponse | null) => void;
};

const itemToString = (item: ClientLocationResponse | null | undefined): string => item?.clientName ?? '';

const ClientAutocomplete = ({ id, titleText, disabled, size = 'lg', hideLabel, selectedClient, onSelect }: Props) => {
  return (
    <AutoCompleteInput<ClientLocationResponse>
      // Remount when the controlled selection changes so Carbon's ComboBox
      // re-seeds its (mount-only) `initialSelectedItem`. 'empty' covers both
      // the cleared and uncontrolled cases.
      key={selectedClient?.clientNumber ?? 'empty'}
      id={id}
      titleText={titleText}
      disabled={disabled}
      size={size}
      hideLabel={hideLabel}
      placeholder=""
      onAutoCompleteChange={getClientsByName}
      itemToString={itemToString}
      onSelect={(item) => onSelect(item ?? null)}
      initialSelectedItem={selectedClient ?? undefined}
    />
  );
};

export default ClientAutocomplete;
