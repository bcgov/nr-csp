import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import * as searchService from '@/services/search.service';

import ClientAutocomplete, { type ClientLocationResponse } from './index';

vi.mock('@/services/search.service', () => ({
  getClientsByName: vi.fn(),
  getClientsByNumber: vi.fn(),
}));

const mockGetByName = searchService.getClientsByName as ReturnType<typeof vi.fn>;

const client = (overrides: Partial<ClientLocationResponse> = {}): ClientLocationResponse => ({
  clientNumber: '00123',
  clientName: 'Acme Logging',
  clientLocnCode: '00',
  clientLocnName: 'Head Office',
  city: 'Victoria',
  province: 'BC',
  ...overrides,
});

const renderPage = async (props: Partial<React.ComponentProps<typeof ClientAutocomplete>> = {}) => {
  const onSelect = vi.fn();
  const qc = new QueryClient();
  await act(() =>
    render(
      <QueryClientProvider client={qc}>
        <ClientAutocomplete id="seller" titleText="Seller client" onSelect={onSelect} {...props} />
      </QueryClientProvider>,
    ),
  );
  return { onSelect };
};

describe('ClientAutocomplete', () => {
  beforeEach(() => {
    mockGetByName.mockReset();
    mockGetByName.mockResolvedValue([client()]);
  });

  it('renders a combobox with the given title', async () => {
    await renderPage();
    expect(screen.getByRole('combobox', { name: /seller client/i })).toBeInTheDocument();
  });

  it('queries clients by name as the user types', async () => {
    await renderPage();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Acme' } });
    await waitFor(() => expect(mockGetByName).toHaveBeenCalledWith('Acme'));
  });

  it('seeds the input with the selected client name', async () => {
    await renderPage({ selectedClient: client({ clientName: 'Cedar Co' }) });
    expect(screen.getByRole('combobox')).toHaveValue('Cedar Co');
  });
});
