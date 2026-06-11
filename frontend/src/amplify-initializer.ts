export function getAmplifyConfig() {
  const c = window.amplifyConfig;
  if (!c) {
    throw new Error(
      '[amplify] window.amplifyConfig not found — ensure amplify-config.js is loaded before the app bundle',
    );
  }

  const config = JSON.parse(JSON.stringify(c)) as NonNullable<typeof window.amplifyConfig>;

  return {
    Auth: {
      Cognito: {
        userPoolId: config.userPoolId,
        userPoolClientId: config.userPoolClientId,
        loginWith: {
          oauth: {
            domain: config.cognitoDomain,
            scopes: config.oauthScopes as ('openid' | 'profile' | 'email')[],
            redirectSignIn: [config.redirectSignIn],
            redirectSignOut: [config.redirectSignOut],
            responseType: 'code' as const,
          },
        },
      },
    },
  };
}
