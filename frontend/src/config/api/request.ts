import { fetchAuthSession } from 'aws-amplify/auth';
import axios from 'axios';

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
