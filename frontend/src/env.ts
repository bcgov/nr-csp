declare global {
  var amplifyConfig:
    | {
        appEnv: string;
        idpName: string;
        region: string;
        userPoolId: string;
        userPoolClientId: string;
        cognitoDomain: string;
        oauthScopes?: string[];
        redirectSignIn: string;
        redirectSignOut: string;
        mockUser?: boolean;
        famClientId?: string;
      }
    | undefined;
}

// Mock authentication bypasses Cognito entirely (auto-login as ADMIN), so it is
// permitted ONLY on a local machine. It requires BOTH the runtime opt-in
// (mockUser: true in amplify-config.js) AND the app actually being served from
// a local host.
const LOCAL_HOSTNAMES = new Set(['localhost', '127.0.0.1', '0.0.0.0', '::1', '[::1]']);
const isLocalHost =
  globalThis.window !== undefined &&
  (LOCAL_HOSTNAMES.has(globalThis.window.location.hostname) ||
    globalThis.window.location.hostname.endsWith('.localhost'));

export const env = {
  mockUser: globalThis.amplifyConfig?.mockUser === true && isLocalHost,
  appEnv: globalThis.amplifyConfig?.appEnv ?? 'dev',
  isDevelopment: import.meta.env.DEV,
  isProduction: import.meta.env.PROD,
} as const;
