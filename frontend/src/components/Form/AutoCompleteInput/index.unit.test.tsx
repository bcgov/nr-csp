import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import AutoCompleteInput from './index';

type Client = { id: string; name: string };

const CLIENTS: Client[] = [
  { id: '1', name: 'ACME FOREST LTD' },
  { id: '2', name: 'BIG TIMBER INC' },
];

const renderWithQuery = (ui: React.ReactElement) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

describe('AutoCompleteInput', () => {
  const onSelect = vi.fn();
  const onTypedChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const arrange = (onAutoCompleteChange = vi.fn().mockResolvedValue(CLIENTS)) => {
    renderWithQuery(
      <AutoCompleteInput<Client>
        id="client"
        titleText="Client name"
        debounceTime={0}
        onAutoCompleteChange={onAutoCompleteChange}
        onSelect={onSelect}
        onTypedChange={onTypedChange}
      />,
    );
    return onAutoCompleteChange;
  };

  it('does not query for fewer than two typed characters', async () => {
    const user = userEvent.setup();
    const fetcher = arrange();

    await user.type(screen.getByRole('combobox', { name: /client name/i }), 'A');

    expect(onTypedChange).toHaveBeenCalledWith('A');
    await waitFor(() => expect(fetcher).not.toHaveBeenCalled());
  });

  it('fetches suggestions after two characters and shows them in the listbox', async () => {
    const user = userEvent.setup();
    const fetcher = arrange();

    await user.type(screen.getByRole('combobox', { name: /client name/i }), 'AC');

    await waitFor(() => expect(fetcher).toHaveBeenCalledWith('AC'));
    expect(await screen.findByText('ACME FOREST LTD')).toBeInTheDocument();
    expect(screen.getByText('BIG TIMBER INC')).toBeInTheDocument();
  });

  it('calls onSelect with the chosen suggestion', async () => {
    const user = userEvent.setup();
    arrange();

    await user.type(screen.getByRole('combobox', { name: /client name/i }), 'AC');
    await user.click(await screen.findByText('ACME FOREST LTD'));

    expect(onSelect).toHaveBeenCalledWith(CLIENTS[0]);
  });

  it('shows a "no data" entry when the query returns an empty list', async () => {
    const user = userEvent.setup();
    arrange(vi.fn().mockResolvedValue([]));

    await user.type(screen.getByRole('combobox', { name: /client name/i }), 'ZZ');

    expect(await screen.findByText(/no data found/i)).toBeInTheDocument();
  });

  it('reports every keystroke through onTypedChange', async () => {
    const user = userEvent.setup();
    arrange();

    await user.type(screen.getByRole('combobox', { name: /client name/i }), 'AB');

    expect(onTypedChange).toHaveBeenNthCalledWith(1, 'A');
    expect(onTypedChange).toHaveBeenNthCalledWith(2, 'AB');
  });
});
