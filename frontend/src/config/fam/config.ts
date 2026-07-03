export const famConfig = {
  clientId: window.amplifyConfig?.famClientId ?? '',
  environment: window.amplifyConfig?.appEnv ?? 'dev',
} as const;
