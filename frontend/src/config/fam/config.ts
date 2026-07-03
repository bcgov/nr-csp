export const famConfig = {
  clientId: globalThis.amplifyConfig?.famClientId ?? '',
  environment: globalThis.amplifyConfig?.appEnv ?? 'dev',
} as const;
