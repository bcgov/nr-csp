declare global {
  interface Window {
    amplifyConfig?: {
      appEnv: string;
      idpName: string;
      region: string;
      userPoolId: string;
      userPoolClientId: string;
      cognitoDomain: string;
      oauthScopes?: string[];
      redirectSignIn: string;
      redirectSignOut: string;
    };
  }
}

// Mock authentication bypasses Cognito entirely (auto-login as ADMIN), so it is
// permitted ONLY on a local machine. It requires BOTH the build opt-in
// (VITE_MOCK_USER=true) AND the app actually being served from a local host.
const LOCAL_HOSTNAMES = new Set(['localhost', '127.0.0.1', '0.0.0.0', '::1', '[::1]']);
const isLocalHost =
  typeof window !== 'undefined' &&
  (LOCAL_HOSTNAMES.has(window.location.hostname) || window.location.hostname.endsWith('.localhost'));

export const env = {
  mockUser: import.meta.env.VITE_MOCK_USER === 'true' && isLocalHost,
  appEnv: window.amplifyConfig?.appEnv ?? 'development',
  isDevelopment: import.meta.env.DEV,
  isProduction: import.meta.env.PROD,
} as const;
