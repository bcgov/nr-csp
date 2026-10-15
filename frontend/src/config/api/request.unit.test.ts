import { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { ErrorResponse, type User } from 'oidc-client-ts';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import * as sessionExpiredSignal from '@/context/auth/sessionExpiredSignal';
import { env } from '@/env';
import { getOidcUser } from '@/service/oidc-service';

import { apiClient } from './request';

import type * as OidcService from '@/service/oidc-service';

vi.mock('@/service/oidc-service', async (importOriginal) => ({
  getOidcUser: vi.fn(),
  isSessionEnded: (await importOriginal<typeof OidcService>()).isSessionEnded,
}));
vi.mock('@/env', () => ({ env: { mockUser: false } }));

vi.mock('@/context/auth/sessionExpiredSignal', () => ({
  emitSessionExpired: vi.fn(),
}));

const mockGetOidcUser = vi.mocked(getOidcUser);
const mockEmitSessionExpired = vi.mocked(sessionExpiredSignal.emitSessionExpired);

// Stands in for the network: answers with the Authorization header the request
// interceptors produced, so each test exercises the real interceptor chain.
const adapter = vi.fn((config: InternalAxiosRequestConfig) =>
  Promise.resolve({ data: config.headers.Authorization, status: 200, statusText: 'OK', headers: {}, config }),
);

function sendRequest() {
  return apiClient.get<string | undefined>('/probe', { adapter });
}

describe('apiClient request interceptor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    (env as { mockUser: boolean }).mockUser = false;
  });

  it('sends the access token, not the ID token, as the bearer token', async () => {
    mockGetOidcUser.mockResolvedValue({ access_token: 'access-token', id_token: 'id-token' } as User);

    const response = await sendRequest();

    expect(response.data).toBe('Bearer access-token');
    expect(mockGetOidcUser).toHaveBeenCalledWith();
  });

  it('sends no Authorization header without a signed-in user', async () => {
    mockGetOidcUser.mockResolvedValue(null);

    const response = await sendRequest();

    expect(response.data).toBeUndefined();
  });

  it('bypasses OIDC entirely in mock-user mode', async () => {
    (env as { mockUser: boolean }).mockUser = true;

    const response = await sendRequest();

    expect(response.data).toBeUndefined();
    expect(mockGetOidcUser).not.toHaveBeenCalled();
  });

  it('ends the session and does not send the request when SSO rejects the renewal', async () => {
    const rejected = new ErrorResponse({ error: 'invalid_grant' });
    mockGetOidcUser.mockRejectedValue(rejected);

    await expect(sendRequest()).rejects.toBe(rejected);

    expect(mockEmitSessionExpired).toHaveBeenCalledTimes(1);
    expect(adapter).not.toHaveBeenCalled();
  });

  it('keeps the session but fails the request when renewal fails temporarily', async () => {
    const temporary = new TypeError('Failed to fetch');
    mockGetOidcUser.mockRejectedValue(temporary);

    await expect(sendRequest()).rejects.toBe(temporary);

    expect(mockEmitSessionExpired).not.toHaveBeenCalled();
    expect(adapter).not.toHaveBeenCalled();
  });
});

function axiosErrorWithStatus(status: number | undefined): AxiosError {
  const error = new AxiosError('request failed');
  if (status !== undefined) {
    error.response = { status } as AxiosError['response'];
  }
  return error;
}

describe('apiClient response interceptor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetOidcUser.mockResolvedValue(null);
  });

  it('emits sessionExpired on a real 401 response', async () => {
    const error = axiosErrorWithStatus(401);
    await expect(apiClient.interceptors.response.handlers[0].rejected(error)).rejects.toBe(error);

    expect(mockEmitSessionExpired).toHaveBeenCalledTimes(1);
  });

  it('does not emit sessionExpired on other error statuses', async () => {
    const error = axiosErrorWithStatus(404);
    await expect(apiClient.interceptors.response.handlers[0].rejected(error)).rejects.toBe(error);

    expect(mockEmitSessionExpired).not.toHaveBeenCalled();
  });

  it('does not emit sessionExpired on a response-less network error', async () => {
    const error = axiosErrorWithStatus(undefined);
    await expect(apiClient.interceptors.response.handlers[0].rejected(error)).rejects.toBe(error);

    expect(mockEmitSessionExpired).not.toHaveBeenCalled();
  });

  it('does not emit sessionExpired for a non-axios error', async () => {
    const error = new Error('boom');
    await expect(apiClient.interceptors.response.handlers[0].rejected(error)).rejects.toBe(error);

    expect(mockEmitSessionExpired).not.toHaveBeenCalled();
  });
});
