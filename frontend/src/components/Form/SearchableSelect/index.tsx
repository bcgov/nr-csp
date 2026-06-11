import { useState } from 'react';
import { ComboBox } from '@carbon/react';

import './index.scss';

type Props = {
  id: string;
  titleText: React.ReactNode;
  label?: string;
  items: string[];
  selectedItem?: string | null;
  onChange: (data: { selectedItem: string | null }) => void;
  disabled?: boolean;
  invalid?: boolean;
  invalidText?: React.ReactNode;
  className?: string;
};

function renderWithHighlight(text: string, query: string): React.ReactNode {
  if (!query) return text;
  const idx = text.toLowerCase().indexOf(query.toLowerCase());
  if (idx === -1) return text;
  return (
    <>
      {text.slice(0, idx)}
      <span className="searchable-select__match">{text.slice(idx, idx + query.length)}</span>
      {text.slice(idx + query.length)}
    </>
  );
}

const SearchableSelect = ({
  id,
  titleText,
  label,
  items,
  selectedItem,
  onChange,
  disabled,
  invalid,
  invalidText,
  className,
}: Props) => {
  const [inputQuery, setInputQuery] = useState('');

  return (
    <ComboBox
      id={id}
      titleText={titleText}
      placeholder={label ?? ''}
      items={items}
      selectedItem={selectedItem ?? null}
      onChange={({ selectedItem: sel }) => onChange({ selectedItem: sel ?? null })}
      onInputChange={(val) => setInputQuery(val ?? '')}
      itemToString={(item) => item ?? ''}
      itemToElement={(item) => <span>{renderWithHighlight(item, inputQuery)}</span>}
      disabled={disabled}
      invalid={invalid}
      invalidText={invalidText}
      className={`searchable-select${className ? ` ${className}` : ''}`}
    />
  );
};

export default SearchableSelect;
