import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';

import SubmissionStatusTag from './index';

const tagFor = (status: string) => screen.getByText(status).closest('.cds--tag');

describe('SubmissionStatusTag', () => {
  it('renders the status text', () => {
    render(<SubmissionStatusTag status="INB" />);
    expect(screen.getByText('INB')).toBeInTheDocument();
  });

  it('always applies the base class', () => {
    render(<SubmissionStatusTag status="INB" />);
    expect(tagFor('INB')).toHaveClass('submission-status-tag');
  });

  it.each([
    ['COM', 'green'],
    ['INB', 'blue'],
    ['LOB', 'cyan'],
    ['REJ', 'red'],
  ])('maps status code %s to Carbon tag type %s', (status, colour) => {
    render(<SubmissionStatusTag status={status} />);
    expect(tagFor(status)).toHaveClass(`cds--tag--${colour}`);
  });

  it.each([
    ['Complete', 'green'],
    ['Inbox', 'blue'],
    ['Lobby', 'cyan'],
    ['Rejected', 'red'],
  ])('maps status description %s to Carbon tag type %s', (status, colour) => {
    render(<SubmissionStatusTag status={status} />);
    expect(tagFor(status)).toHaveClass(`cds--tag--${colour}`);
  });

  it('falls back to gray for an unrecognised status', () => {
    render(<SubmissionStatusTag status="UNKNOWN" />);
    expect(tagFor('UNKNOWN')).toHaveClass('cds--tag--gray');
  });

  it.each(['COM', 'Complete'])('applies completed modifier for status %s', (status) => {
    render(<SubmissionStatusTag status={status} />);
    expect(tagFor(status)).toHaveClass('submission-status-tag--completed');
  });

  it.each(['INB', 'LOB', 'REJ', 'Inbox', 'Lobby', 'Rejected', 'UNKNOWN'])(
    'does not apply completed modifier for status %s',
    (status) => {
      render(<SubmissionStatusTag status={status} />);
      expect(tagFor(status)).not.toHaveClass('submission-status-tag--completed');
    },
  );
});
