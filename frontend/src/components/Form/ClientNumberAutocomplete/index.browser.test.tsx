import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import * as searchService from '@/services/search.service';

import ClientNumberAutocomplete, { type ClientLocationResponse } from './index';

vi.mock('@/services/search.service', () => ({
  getClientsByName: vi.fn(),
  getClientsByNumber: vi.fn(),
}));

const mockGetByNumber = searchService.getClientsByNumber as ReturnType<typeof vi.fn>;

const client = (overrides: Partial<ClientLocationResponse> = {}): ClientLocationResponse => ({
  clientNumber: '00123',
  clientName: 'Acme Logging',
  clientLocnCode: '00',
  clientLocnName: 'Head Office',
  city: 'Victoria',
  province: 'BC',
  ...overrides,
});

const renderPage = async (props: Partial<React.ComponentProps<typeof ClientNumberAutocomplete>> = {}) => {
  const onSelect = vi.fn();
  const qc = new QueryClient();
  await act(() =>
    render(
      <QueryClientProvider client={qc}>
        <ClientNumberAutocomplete id="seller-num" titleText="Seller number" onSelect={onSelect} {...props} />
      </QueryClientProvider>,
    ),
  );
  return { onSelect };
};

describe('ClientNumberAutocomplete', () => {
  beforeEach(() => {
    mockGetByNumber.mockReset();
    mockGetByNumber.mockResolvedValue([client()]);
  });

  it('renders a combobox with the given title', async () => {
    await renderPage();
    expect(screen.getByRole('combobox', { name: /seller number/i })).toBeInTheDocument();
  });

  it('queries clients by number as the user types', async () => {
    await renderPage();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: '001' } });
    await waitFor(() => expect(mockGetByNumber).toHaveBeenCalledWith('001'));
  });

  it('seeds the input with the selected client number (not the name)', async () => {
    await renderPage({ selectedClient: client({ clientNumber: '99999', clientName: 'Cedar Co' }) });
    expect(screen.getByRole('combobox')).toHaveValue('99999');
  });
});
