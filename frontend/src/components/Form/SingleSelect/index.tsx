import { Dropdown, type DropdownProps } from '@carbon/react';

import './index.scss';

// Unique sentinel for the prepended "Select..." item. Using a Symbol (rather than
// null) means Carbon/Downshift treats it as a real selected value and renders it via
// itemToString in the trigger — instead of falling back to the `label` placeholder
// when the parent's state is null/undefined.
const BLANK_SENTINEL: unique symbol = Symbol('single-select-blank');

/**
 * SingleSelect is a thin wrapper around Carbon's Dropdown that prevents
 * menu items from truncating. The dropdown list expands to the longest
 * item's width and is never narrower than the trigger input.
 *
 * A "Select..." blank option is automatically prepended to the items list and
 * is shown in the trigger whenever the parent's `selectedItem` is null or
 * undefined. Selecting it fires onChange with { selectedItem: null }, allowing
 * callers to reset their state with the standard `selectedItem ?? null` pattern.
 *
 * All other Carbon Dropdown props are forwarded unchanged.
 *
 * @template ItemType The type of items in the list.
 * @param {DropdownProps<ItemType>} props - Carbon Dropdown props.
 * @returns {React.ReactElement} The rendered dropdown.
 */
const SingleSelect = <ItemType,>({
  className,
  label = 'Select...',
  items,
  itemToString,
  selectedItem,
  onChange,
  ...props
}: DropdownProps<ItemType>): React.ReactElement => {
  const blankItem = BLANK_SENTINEL as unknown as ItemType;
  const itemsWithBlank = [blankItem, ...items];

  const resolvedItemToString = (item: ItemType | null | undefined): string => {
    if (item === null || item === undefined || (item as unknown) === BLANK_SENTINEL) {
      return 'Select...';
    }
    return itemToString ? itemToString(item) : String(item);
  };

  const resolvedSelectedItem = selectedItem ?? blankItem;

  const handleChange: NonNullable<DropdownProps<ItemType>['onChange']> = (data) => {
    onChange?.({
      ...data,
      selectedItem: (data.selectedItem as unknown) === BLANK_SENTINEL ? null : data.selectedItem,
    });
  };

  return (
    <Dropdown
      {...props}
      label={label}
      items={itemsWithBlank}
      itemToString={resolvedItemToString}
      selectedItem={resolvedSelectedItem}
      onChange={handleChange}
      className={`single-select${className ? ` ${className}` : ''}`}
    />
  );
};

export default SingleSelect;
