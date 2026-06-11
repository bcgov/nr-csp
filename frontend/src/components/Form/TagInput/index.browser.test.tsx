import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';

import TagInput from './index';

const setup = (overrides: Partial<React.ComponentProps<typeof TagInput>> = {}) => {
  const onChange = vi.fn();
  const props = {
    id: 'booms',
    labelText: 'Boom numbers',
    values: ['A', 'B'],
    onChange,
    ...overrides,
  };
  render(<TagInput {...props} />);
  return { onChange, input: screen.getByLabelText('Boom numbers') };
};

describe('TagInput', () => {
  it('renders a chip for each committed value', () => {
    setup();
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText('B')).toBeInTheDocument();
  });

  it('commits the draft (appended) when Enter is pressed', async () => {
    const { input, onChange } = setup();
    await userEvent.type(input, 'C{enter}');
    expect(onChange).toHaveBeenCalledWith(['A', 'B', 'C']);
  });

  it('commits the draft when a comma is typed', async () => {
    const { input, onChange } = setup({ values: [] });
    await userEvent.type(input, 'X,');
    expect(onChange).toHaveBeenCalledWith(['X']);
  });

  it('does not add a duplicate value', async () => {
    const { input, onChange } = setup();
    await userEvent.type(input, 'A{enter}');
    expect(onChange).toHaveBeenCalledWith(['A', 'B']);
  });

  it('removes the last value on Backspace when the input is empty', async () => {
    const { input, onChange } = setup();
    await userEvent.type(input, '{backspace}');
    expect(onChange).toHaveBeenCalledWith(['A']);
  });

  it('removes a value when its chip close button is clicked', () => {
    const { onChange } = setup();
    const chip = screen.getByText('A').closest('.cds--tag');
    const closeButton = chip?.querySelector('button');
    expect(closeButton).not.toBeNull();
    fireEvent.click(closeButton as HTMLButtonElement);
    expect(onChange).toHaveBeenCalledWith(['B']);
  });
});
