import { FilterableMultiSelect, TextInputSkeleton, type FilterableMultiSelectProps } from '@carbon/react';
import type { ReactElement } from 'react';

import './index.scss';

/**
 * Props for the ActiveMultiSelect component.
 *
 * @template ItemType The type of the items in the multi-select.
 * @extends FilterableMultiSelectProps<ItemType>
 * @property {boolean} [showSkeleton=false] - If true, displays a skeleton loader instead of the multi-select component.
 */
interface ActiveMultiSelectProps<ItemType> extends FilterableMultiSelectProps<ItemType> {
  showSkeleton?: boolean;
}

/**
 * Renders the skeleton loader for the ActiveMultiSelect component.
 *
 * @returns {ReactElement} The skeleton loader component.
 */
const renderSkeleton = (): ReactElement => {
  return <TextInputSkeleton hideLabel />;
};

/**
 * Renders the FilterableMultiSelect component with default selection feedback.
 *
 * @template ItemType The type of the items in the multi-select.
 * @param {ActiveMultiSelectProps<ItemType>} props - The component props.
 * @returns {ReactElement} The multi-select component.
 */
const renderMultiSelect = <ItemType,>({
  selectionFeedback = 'top-after-reopen',
  className,
  ...props
}: ActiveMultiSelectProps<ItemType>): ReactElement => {
  return (
    <FilterableMultiSelect
      {...props}
      selectionFeedback={selectionFeedback}
      className={`active-multi-select${className ? ` ${className}` : ''}`}
    />
  );
};

/**
 * ActiveMultiSelect is a custom multi-select component based on Carbon's `FilterableMultiSelect`.
 * It conditionally renders a skeleton loader (`TextInputSkeleton`) if `showSkeleton` is true.
 *
 * @template ItemType The type of the items in the multi-select.
 * @param {ActiveMultiSelectProps<ItemType>} props - The component props.
 * @returns {ReactElement} The rendered component.
 */
const ActiveMultiSelect = <ItemType,>({
  showSkeleton = false,
  ...props
}: ActiveMultiSelectProps<ItemType>): ReactElement => {
  return showSkeleton ? renderSkeleton() : renderMultiSelect(props);
};

export default ActiveMultiSelect;
