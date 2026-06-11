import { Search } from '@carbon/react';
import { useEffect, useRef, type FC } from 'react';

/**
 * Props for the SearchInput component.
 *
 * @property {string} id - Unique identifier for the search input.
 * @property {string} titleText - The title text for the input.
 * @property {string} placeholder - Placeholder text for the input field.
 * @property {string} value - The current value of the search input.
 * @property {(value: string) => void} onChange - Callback fired when the input value changes.
 */
type SearchInputProps = {
  id: string;
  titleText: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
};

/**
 * SearchInput is a reusable search field built on Carbon's Search component.
 * It provides a controlled input for search queries, with custom mouse tracking logic and keyboard handling for accessibility.
 *
 * @param {SearchInputProps} props - The props for the component.
 * @returns {JSX.Element} The rendered SearchInput component.
 */
const SearchInput: FC<SearchInputProps> = ({ id, titleText, placeholder, onChange, value }) => {
  const mousePosition = useRef<{ x: number; y: number }>({ x: 0, y: 0 });
  const searchButtonRefs = useRef<HTMLButtonElement[]>([]);

  // Track and update the mouse position globally
  useEffect(() => {
    const updateMousePosition = (e: MouseEvent) => {
      mousePosition.current = { x: e.clientX, y: e.clientY };
    };

    document.addEventListener('mousemove', updateMousePosition);
    return () => {
      document.removeEventListener('mousemove', updateMousePosition);
    };
  }, []);

  return (
    <Search
      aria-label={titleText}
      data-testid={id}
      role="searchbox"
      size="md"
      placeholder={placeholder}
      labelText={titleText}
      closeButtonLabelText="Clear search input"
      id={id}
      onBlur={(e) => {
        onChange(e.currentTarget.value);
      }}
      onKeyDown={(e) => {
        if (e.key === 'Enter') {
          e.currentTarget.blur();
        }
      }}
      defaultValue={value}
    />
  );
};

export default SearchInput;
