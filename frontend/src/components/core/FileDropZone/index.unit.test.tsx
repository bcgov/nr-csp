import { render, screen, fireEvent } from '@testing-library/react';
import { afterEach, describe, it, expect, vi } from 'vitest';

import FileDropZone from './index';

const makeFile = () => new File(['x'], 'f.xml', { type: 'text/xml' });

describe('FileDropZone', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the default description and button label', () => {
    render(<FileDropZone onFileSelected={vi.fn()} />);
    expect(screen.getByText('Select or drag and drop your file to upload.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Browse files' })).toBeTruthy();
  });

  it('renders a custom description and button label when passed', () => {
    render(
      <FileDropZone onFileSelected={vi.fn()} description="Custom description" buttonLabel="Pick a file" />,
    );
    expect(screen.getByText('Custom description')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Pick a file' })).toBeTruthy();
  });

  it('opens the file picker when the browse button is clicked', () => {
    const clickSpy = vi.spyOn(HTMLInputElement.prototype, 'click');
    render(<FileDropZone onFileSelected={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: 'Browse files' }));
    expect(clickSpy).toHaveBeenCalledTimes(1);
  });

  it('does not open the file picker when disabled and the button is clicked', () => {
    const clickSpy = vi.spyOn(HTMLInputElement.prototype, 'click');
    render(<FileDropZone onFileSelected={vi.fn()} disabled />);
    // Disabled Carbon button won't fire onClick; call the handler path via the DOM anyway.
    fireEvent.click(screen.getByRole('button', { name: 'Browse files' }));
    expect(clickSpy).not.toHaveBeenCalled();
  });

  it('fires onFileSelected and resets the input value when a file is selected', () => {
    const onFileSelected = vi.fn();
    const { container } = render(<FileDropZone onFileSelected={onFileSelected} />);
    const input = container.querySelector('input[type=file]') as HTMLInputElement;
    const file = makeFile();

    fireEvent.change(input, { target: { files: [file] } });

    expect(onFileSelected).toHaveBeenCalledTimes(1);
    expect(onFileSelected).toHaveBeenCalledWith(file);
    expect(input.value).toBe('');
  });

  it('does not fire onFileSelected when the change has no files', () => {
    const onFileSelected = vi.fn();
    const { container } = render(<FileDropZone onFileSelected={onFileSelected} />);
    const input = container.querySelector('input[type=file]') as HTMLInputElement;

    fireEvent.change(input, { target: { files: [] } });

    expect(onFileSelected).not.toHaveBeenCalled();
  });

  it('adds the dragging class on dragOver and removes it on dragLeave', () => {
    const { container } = render(<FileDropZone onFileSelected={vi.fn()} />);
    const zone = container.querySelector('.file-drop-zone') as HTMLElement;

    fireEvent.dragOver(zone);
    expect(zone.classList.contains('file-drop-zone--dragging')).toBe(true);

    fireEvent.dragLeave(zone);
    expect(zone.classList.contains('file-drop-zone--dragging')).toBe(false);
  });

  it('fires onFileSelected on drop', () => {
    const onFileSelected = vi.fn();
    const { container } = render(<FileDropZone onFileSelected={onFileSelected} />);
    const zone = container.querySelector('.file-drop-zone') as HTMLElement;
    const file = makeFile();

    fireEvent.drop(zone, { dataTransfer: { files: [file] } });

    expect(onFileSelected).toHaveBeenCalledTimes(1);
    expect(onFileSelected).toHaveBeenCalledWith(file);
  });

  it('does not add the dragging class on dragOver when disabled', () => {
    const { container } = render(<FileDropZone onFileSelected={vi.fn()} disabled />);
    const zone = container.querySelector('.file-drop-zone') as HTMLElement;

    fireEvent.dragOver(zone);
    expect(zone.classList.contains('file-drop-zone--dragging')).toBe(false);
  });

  it('does not fire onFileSelected on drop when disabled', () => {
    const onFileSelected = vi.fn();
    const { container } = render(<FileDropZone onFileSelected={onFileSelected} disabled />);
    const zone = container.querySelector('.file-drop-zone') as HTMLElement;

    fireEvent.drop(zone, { dataTransfer: { files: [makeFile()] } });

    expect(onFileSelected).not.toHaveBeenCalled();
  });
});
