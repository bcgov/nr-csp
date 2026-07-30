import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import TagInput from './index';

describe('TagInput', () => {
  const onChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderInput = (values: string[] = [], extra: Partial<Parameters<typeof TagInput>[0]> = {}) =>
    render(<TagInput id="booms" labelText="Boom numbers" values={values} onChange={onChange} {...extra} />);

  it('commits the draft on Enter', async () => {
    const user = userEvent.setup();
    renderInput();

    await user.type(screen.getByRole('textbox', { name: /boom numbers/i }), 'B-1{Enter}');

    expect(onChange).toHaveBeenCalledWith(['B-1']);
  });

  it('splits a pasted comma-separated draft into individual values on commit', async () => {
    const user = userEvent.setup();
    renderInput(['B-1']);

    const input = screen.getByRole('textbox', { name: /boom numbers/i });
    await user.click(input);
    await user.paste('B-2, B-3');
    await user.keyboard('{Enter}');

    expect(onChange).toHaveBeenLastCalledWith(['B-1', 'B-2', 'B-3']);
  });

  it('commits the draft on blur and ignores a blank draft', async () => {
    const user = userEvent.setup();
    renderInput();

    const input = screen.getByRole('textbox', { name: /boom numbers/i });
    await user.type(input, '   ');
    await user.tab();
    expect(onChange).not.toHaveBeenCalled();

    await user.type(input, 'B-9');
    await user.tab();
    expect(onChange).toHaveBeenCalledWith(['B-9']);
  });

  it('dedupes against existing values and within one commit', async () => {
    const user = userEvent.setup();
    renderInput(['B-1']);

    await user.type(screen.getByRole('textbox', { name: /boom numbers/i }), 'B-1,B-2,B-2{Enter}');

    expect(onChange).toHaveBeenLastCalledWith(['B-1', 'B-2']);
  });

  it('stops accepting values beyond maxTags and shows the cap in the helper text', async () => {
    const user = userEvent.setup();
    renderInput(['B-1', 'B-2'], { maxTags: 2 });

    expect(screen.getByText('Maximum 2 reached')).toBeInTheDocument();

    await user.type(screen.getByRole('textbox', { name: /boom numbers/i }), 'B-3{Enter}');
    expect(onChange).toHaveBeenCalledWith(['B-1', 'B-2']);
  });

  it('shows the remaining capacity while under the cap', () => {
    renderInput(['B-1'], { maxTags: 3 });
    expect(screen.getByText('Up to 3')).toBeInTheDocument();
  });

  it('removes the last chip on Backspace in an empty input', async () => {
    const user = userEvent.setup();
    renderInput(['B-1', 'B-2']);

    await user.type(screen.getByRole('textbox', { name: /boom numbers/i }), '{Backspace}');

    expect(onChange).toHaveBeenCalledWith(['B-1']);
  });

  it('renders committed values as chips and removes one via its close button', async () => {
    const user = userEvent.setup();
    renderInput(['B-1', 'B-2']);

    expect(screen.getByText('B-1')).toBeInTheDocument();
    expect(screen.getByText('B-2')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /remove b-1/i }));

    expect(onChange).toHaveBeenCalledWith(['B-2']);
  });
});
