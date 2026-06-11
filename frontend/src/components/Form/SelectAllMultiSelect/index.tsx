import { DismissibleTag, FilterableMultiSelect } from '@carbon/react';
import React from 'react';

import './index.scss';

/**
 * A Carbon native `{ isSelectAll: true }` item is prepended to the list.
 * Carbon v11 always renders it first, manages its checked/indeterminate
 * state internally, and fires `onChange` with only real items (sentinel
 * is excluded from the payload).
 */
const SELECT_ALL_ITEM = { isSelectAll: true as const };

function isSelectAllItem(item: unknown): boolean {
  return typeof item === 'object' && item !== null && 'isSelectAll' in item;
}

export interface SelectAllMultiSelectProps<T> {
  id: string;
  titleText: string;
  hideLabel?: boolean;
  size?: 'sm' | 'md' | 'lg';
  placeholder?: string;
  items: T[];
  selectedItems: T[];
  disabled?: boolean;
  itemToString: (item: T | null) => string;
  itemToKey: (item: T) => string;
  onChange: (change: { selectedItems: T[] }) => void;
}

/**
 * Searchable multi-select with a built-in "Select all" option and
 * DismissibleTag chips + "Clear all" button below the field.
 *
 * Uses Carbon's native `{ isSelectAll: true }` sentinel so position,
 * checked/indeterminate state, and the select-all toggle are all managed
 * by Carbon's own `useSelection` hook.
 */
function SelectAllMultiSelect<T>({
  id,
  titleText,
  hideLabel,
  size,
  placeholder,
  items,
  selectedItems,
  disabled,
  itemToString: itemToStr,
  itemToKey,
  onChange,
}: SelectAllMultiSelectProps<T>): React.ReactElement {
  type AugItem = T | typeof SELECT_ALL_ITEM;

  const augItems: AugItem[] = [SELECT_ALL_ITEM, ...items];

  const augItemToString = (item: AugItem | null): string => {
    if (!item) return '';
    if (isSelectAllItem(item)) return 'Select all';
    return itemToStr(item as T);
  };

  const handleChange = ({ selectedItems: next }: { selectedItems: AugItem[] | null }) => {
    // Carbon's useSelection already filters the sentinel out of the payload
    onChange({ selectedItems: (next ?? []).filter((i): i is T => !isSelectAllItem(i)) });
  };

  const handleRemove = (item: T) => {
    const key = itemToKey(item);
    onChange({ selectedItems: selectedItems.filter((i) => itemToKey(i) !== key) });
  };

  return (
    // tagged-multi-select provides field-pinning + tag flex-layout CSS;
    // select-all-multiselect adds the sentinel separator and clear-btn.
    <div className="tagged-multi-select select-all-multiselect">
      <FilterableMultiSelect
        id={id}
        titleText={titleText}
        hideLabel={hideLabel}
        size={size}
        placeholder={placeholder}
        items={augItems as unknown as T[]}
        selectedItems={selectedItems}
        disabled={disabled}
        itemToString={augItemToString as (item: T | null) => string}
        selectionFeedback="fixed"
        className="active-multi-select tagged-multi-select__field"
        onChange={handleChange as unknown as (data: { selectedItems: T[] }) => void}
        itemToElement={(item: T) => {
          if (isSelectAllItem(item)) {
            return <span className="select-all-multiselect__select-all-label">Select all</span>;
          }
          return <span>{itemToStr(item)}</span>;
        }}
      />
      {selectedItems.length > 0 && (
        <div className="tagged-multi-select__tags">
          {selectedItems.map((item) => (
            <DismissibleTag
              key={itemToKey(item)}
              type="blue"
              text={itemToStr(item)}
              title="Remove"
              onClose={() => handleRemove(item)}
            />
          ))}
          <button
            type="button"
            className="select-all-multiselect__clear-btn"
            onClick={() => onChange({ selectedItems: [] })}
          >
            Clear all
          </button>
        </div>
      )}
    </div>
  );
}

export default SelectAllMultiSelect;
