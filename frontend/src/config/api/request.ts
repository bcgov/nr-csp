import { fetchAuthSession } from 'aws-amplify/auth';
import axios from 'axios';

import { emitSessionExpired } from '@/context/auth/sessionExpiredSignal';

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 60000,
});

apiClient.interceptors.request.use(async (config) => {
  try {
    const session = await fetchAuthSession();
    const token = session.tokens?.idToken?.toString();
    if (token) config.headers.Authorization = `Bearer ${token}`;
  } catch {
    // unauthenticated request — let the server respond with 401
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // A real 401 from our backend is the one unambiguous signal that the session is
    // actually dead (vs. a transient network/Cognito blip) — see RealAuthProvider.
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      emitSessionExpired();
    }
    return Promise.reject(error);
  },
);
