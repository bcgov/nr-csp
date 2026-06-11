import { DismissibleTag, type FilterableMultiSelectProps } from '@carbon/react';

import ActiveMultiSelect from '../ActiveMultiSelect';

import type { ReactElement } from 'react';

import './index.scss';

/**
 * Props for the TaggedMultiSelect component.
 *
 * @template ItemType The type of the items in the multi-select.
 * @extends FilterableMultiSelectProps<ItemType>
 * @property {(item: ItemType) => string} [itemToKey] - Derives a stable key for
 * each selected item. Defaults to `itemToString`.
 */
interface TaggedMultiSelectProps<ItemType> extends FilterableMultiSelectProps<ItemType> {
  itemToKey?: (item: ItemType) => string;
}

/**
 * TaggedMultiSelect is a searchable multi-select built on the shared
 * ActiveMultiSelect (Carbon's FilterableMultiSelect). The built-in selection
 * count badge is hidden; instead each selected item is surfaced as a
 * DismissibleTag below the field, and clicking a tag's dismiss button removes
 * that item from the selection.
 *
 * @template ItemType The type of the items in the multi-select.
 * @param {TaggedMultiSelectProps<ItemType>} props - The component props.
 * @returns {ReactElement} The rendered component.
 */
const TaggedMultiSelect = <ItemType,>({
  className,
  selectedItems,
  itemToString,
  itemToKey,
  onChange,
  ...props
}: TaggedMultiSelectProps<ItemType>): ReactElement => {
  const selected = selectedItems ?? [];

  const toLabel = (item: ItemType): string => (itemToString ? itemToString(item) : String(item));
  const toKey = (item: ItemType): string => (itemToKey ? itemToKey(item) : toLabel(item));

  const handleRemove = (item: ItemType): void => {
    const key = toKey(item);
    onChange?.({ selectedItems: selected.filter((i) => toKey(i) !== key) });
  };

  return (
    <div className="tagged-multi-select">
      <ActiveMultiSelect
        {...props}
        className={`tagged-multi-select__field${className ? ` ${className}` : ''}`}
        selectedItems={selected}
        itemToString={itemToString}
        onChange={onChange}
      />
      {selected.length > 0 && (
        <div className="tagged-multi-select__tags">
          {selected.map((item) => (
            <DismissibleTag
              key={toKey(item)}
              type="blue"
              text={toLabel(item)}
              title="Remove"
              onClose={() => handleRemove(item)}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default TaggedMultiSelect;
