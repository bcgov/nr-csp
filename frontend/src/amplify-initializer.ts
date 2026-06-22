export function getAmplifyConfig() {
  const c = window.amplifyConfig;
  if (!c) {
    throw new Error(
      '[amplify] window.amplifyConfig not found — ensure amplify-config.js is loaded before the app bundle',
    );
  }

  const origin = window.location.origin;

  // amplify-config.js uses a flat structure; map it to the Amplify v6 nested format.
  const redirectSignIn = import.meta.env.DEV ? [`${origin}/`] : [String(c.redirectSignIn)];
  const redirectSignOut = [String(c.redirectSignOut)];

  return {
    appEnv: c.appEnv,
    Auth: {
      Cognito: {
        region: c.region,
        userPoolId: c.userPoolId,
        userPoolClientId: c.userPoolClientId,
        loginWith: {
          oauth: {
            domain: c.cognitoDomain,
            scopes: c.oauthScopes ?? ['openid', 'profile', 'email'],
            redirectSignIn,
            redirectSignOut,
            responseType: 'code' as const,
          },
        },
      },
    },
  };
}
