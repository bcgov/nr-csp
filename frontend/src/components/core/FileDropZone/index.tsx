import { Upload } from '@carbon/icons-react';
import { Button } from '@carbon/react';
import { useRef, useState, type DragEvent, type FC } from 'react';

import './index.scss';

/**
 * Props for the FileDropZone component.
 *
 * @property {(file: File) => void} onFileSelected - Called with the chosen file when the user browses or drops one.
 * @property {string} [accept] - Comma-separated list of accepted file extensions / MIME types (e.g. ".xml").
 * @property {string} [description] - Instructional text shown beside the upload icon.
 * @property {string} [buttonLabel] - Label for the browse button.
 * @property {boolean} [disabled] - Disables browsing and drop handling.
 */
interface FileDropZoneProps {
  onFileSelected: (file: File) => void;
  accept?: string;
  description?: string;
  buttonLabel?: string;
  disabled?: boolean;
}

/**
 * FileDropZone renders a bordered drag-and-drop area with an upload icon,
 * instructional text, and a browse button — the standard file intake control.
 * It owns only the picking interaction: the parent decides what to do with the
 * selected file via {@link FileDropZoneProps.onFileSelected}.
 *
 * @param {FileDropZoneProps} props - The component props.
 * @returns {JSX.Element} The rendered drop zone.
 */
const FileDropZone: FC<FileDropZoneProps> = ({
  onFileSelected,
  accept = '.xml',
  description = 'Select or drag and drop your file to upload.',
  buttonLabel = 'Browse files',
  disabled = false,
}) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragging, setIsDragging] = useState(false);

  const openPicker = () => {
    if (!disabled) inputRef.current?.click();
  };

  const handleFile = (fileList: FileList | null) => {
    const file = fileList?.[0];
    if (file) onFileSelected(file);
  };

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!disabled) setIsDragging(true);
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    if (!disabled) handleFile(e.dataTransfer.files);
  };

  return (
    <div
      className={`file-drop-zone${isDragging ? ' file-drop-zone--dragging' : ''}${
        disabled ? ' file-drop-zone--disabled' : ''
      }`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <div className="file-drop-zone__info">
        <span className="file-drop-zone__icon" aria-hidden="true">
          <Upload size={20} />
        </span>
        <p className="file-drop-zone__text">{description}</p>
      </div>
      <Button kind="primary" onClick={openPicker} disabled={disabled}>
        {buttonLabel}
      </Button>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className="file-drop-zone__input"
        disabled={disabled}
        onChange={(e) => {
          handleFile(e.target.files);
          // Reset so selecting the same file again still fires onChange.
          e.target.value = '';
        }}
      />
    </div>
  );
};

export default FileDropZone;
