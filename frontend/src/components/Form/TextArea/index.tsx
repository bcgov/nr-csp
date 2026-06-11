import { TextArea as CarbonTextArea, type TextAreaProps } from '@carbon/react';
import { type FC } from 'react';

import './index.scss';

/**
 * Thin wrapper around Carbon's TextArea that applies the project's input-field
 * background colour (#f4f4f4 in the g10 theme) so multi-line inputs visually
 * match TextInput / DatePicker / Dropdown / Search across the app. All Carbon
 * TextArea props are forwarded unchanged.
 */
const TextArea: FC<TextAreaProps> = ({ className, ...props }) => (
  <CarbonTextArea {...props} className={`form-text-area${className ? ` ${className}` : ''}`} />
);

export default TextArea;
