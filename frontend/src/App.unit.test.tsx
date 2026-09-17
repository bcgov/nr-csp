import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import App from './App';

// Force the mock auth path so the full provider tree renders without Cognito.
vi.mock('@/env', () => ({ env: { mockUser: true } }));

const useSubmissionDetailQuery = vi.fn();
vi.mock('@/services/submissionHistory.service', () => ({
  useSubmissionDetailQuery: (submissionId: string | undefined) => useSubmissionDetailQuery(submissionId),
}));

describe('App', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/logout');
  });

  it('renders the provider tree and serves the public logout route', async () => {
    render(<App />);

    expect(await screen.findByRole('heading', { name: /you’ve successfully logged out/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /back to home/i })).toBeInTheDocument();
  });

  // Guards the wiring between the route's param name and the page's useParams:
  // a mismatch leaves the id undefined and the detail page silently empty.
  it('passes the submission history url segment through to the detail lookup', async () => {
    useSubmissionDetailQuery.mockReturnValue({ data: undefined, isLoading: true, isError: false, error: null });
    window.history.pushState({}, '', '/submission-history/9001');

    render(<App />);

    await waitFor(() => expect(useSubmissionDetailQuery).toHaveBeenCalledWith('9001'));
  });
});
