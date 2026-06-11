import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import FormModal from './index';

const renderModal = (props: Partial<React.ComponentProps<typeof FormModal>> = {}) => {
  render(
    <FormModal open onClose={vi.fn()} onSubmit={vi.fn()} {...props}>
      content
    </FormModal>,
  );
};

describe('FormModal', () => {
  it('renders a primary submit button by default', () => {
    renderModal({ submitLabel: 'Save' });
    expect(screen.getByRole('button', { name: 'Save' })).toHaveClass('cds--btn--primary');
  });

  it('renders a danger submit button when danger=true', () => {
    renderModal({ submitLabel: 'Delete', danger: true });
    expect(screen.getByRole('button', { name: /Delete$/ })).toHaveClass('cds--btn--danger');
  });
});
