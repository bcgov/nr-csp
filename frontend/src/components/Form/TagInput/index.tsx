import { Tag, TextInput } from '@carbon/react';
import { type KeyboardEvent, type ReactNode, useState } from 'react';

import './index.scss';

/**
 * Comma- or Enter-separated multi-value input that renders the committed
 * values as Carbon `Tag` chips beneath the text input. Backspace on an
 * empty input removes the last committed value.
 *
 * The component is fully controlled — pass `values` and handle `onChange`
 * yourself. Used by the Invoice page for boomNumbers / timberMarks /
 * weightSlips since the backend expects `List<String>` for each.
 */
export interface TagInputProps {
  id: string;
  labelText: ReactNode;
  placeholder?: string;
  values: string[];
  onChange: (next: string[]) => void;
  invalid?: boolean;
  invalidText?: string;
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
}

const TagInput = ({
  id,
  labelText,
  placeholder = 'Type and press Enter or comma',
  values,
  onChange,
  invalid,
  invalidText,
  size = 'md',
  disabled = false,
}: TagInputProps) => {
  const [draft, setDraft] = useState('');

  const commit = (raw: string) => {
    const next = raw.trim();
    if (!next) return;
    const pieces = next
      .split(',')
      .map((p) => p.trim())
      .filter(Boolean);
    if (pieces.length === 0) return;
    onChange([...values, ...pieces.filter((p) => !values.includes(p))]);
    setDraft('');
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      commit(draft);
    } else if (e.key === 'Backspace' && draft === '' && values.length > 0) {
      // Remove last chip when backspacing on an empty input.
      onChange(values.slice(0, -1));
    }
  };

  const removeValue = (value: string) => {
    onChange(values.filter((v) => v !== value));
  };

  return (
    <div className="tag-input">
      <TextInput
        id={id}
        labelText={labelText}
        placeholder={placeholder}
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={handleKeyDown}
        onBlur={() => commit(draft)}
        invalid={invalid}
        invalidText={invalidText}
        size={size}
        disabled={disabled}
      />
      {values.length > 0 ? (
        <div className="tag-input__chips">
          {values.map((value) => (
            <Tag
              key={value}
              type="cool-gray"
              size="md"
              filter={!disabled}
              onClose={() => removeValue(value)}
              title={`Remove ${value}`}
            >
              {value}
            </Tag>
          ))}
        </div>
      ) : null}
    </div>
  );
};

export default TagInput;
