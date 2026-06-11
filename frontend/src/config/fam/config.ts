export const famConfig = {
  clientId: import.meta.env.VITE_FAM_CLIENT_ID ?? '',
  environment: window.amplifyConfig?.appEnv ?? 'development',
} as const;
