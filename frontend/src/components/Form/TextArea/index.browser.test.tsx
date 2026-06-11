import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import TextArea from './index';

describe('TextArea', () => {
  it('renders the label and value', () => {
    render(<TextArea id="comments" labelText="Comments" value="hello" onChange={() => {}} />);
    const textarea = screen.getByLabelText('Comments');
    expect(textarea).toBeInTheDocument();
    expect(textarea).toHaveValue('hello');
  });

  it('applies the form-text-area class and any custom className (on the wrapper)', () => {
    const { container } = render(<TextArea id="comments" labelText="Comments" className="extra" onChange={() => {}} />);
    const wrapper = container.querySelector('.form-text-area');
    expect(wrapper).not.toBeNull();
    expect(wrapper).toHaveClass('extra');
  });

  it('forwards Carbon props such as invalid/invalidText', () => {
    render(<TextArea id="comments" labelText="Comments" invalid invalidText="Required" onChange={() => {}} />);
    expect(screen.getByText('Required')).toBeInTheDocument();
  });
});
