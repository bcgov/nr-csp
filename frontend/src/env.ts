declare global {
  interface Window {
    amplifyConfig?: {
      appEnv: string;
      region: string;
      userPoolId: string;
      userPoolClientId: string;
      cognitoDomain: string;
      oauthScopes: string[];
      redirectSignIn: string;
      redirectSignOut: string;
    };
  }
}

export const env = {
  mockUser: import.meta.env.VITE_MOCK_USER === 'true',
  appEnv: window.amplifyConfig?.appEnv ?? 'development',
  isDevelopment: import.meta.env.DEV,
  isProduction: import.meta.env.PROD,
} as const;
