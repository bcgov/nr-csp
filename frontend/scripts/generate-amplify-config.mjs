import fs from 'node:fs/promises';
import path from 'node:path';

const outputPath = path.join(import.meta.dirname, '..', 'public', 'amplify-config.js');

const config = {
  appEnv: process.env.APP_ENV ?? 'development',
  idpName: process.env.COGNITO_IDP_NAME ?? 'DEV-IDIR',
  region: process.env.COGNITO_REGION ?? 'ca-central-1',
  userPoolId: process.env.COGNITO_USER_POOL_ID ?? 'ca-central-1_REPLACE_WITH_YOUR_COGNITO_USER_POOL_ID',
  // Each application must provide its own client ID — replace this placeholder
  userPoolClientId: process.env.COGNITO_USER_POOL_CLIENT_ID ?? 'REPLACE_WITH_YOUR_COGNITO_CLIENT_ID',
  cognitoDomain: process.env.COGNITO_DOMAIN ?? 'REPLACE_WITH_YOUR_COGNITO_DOMAIN',
  oauthScopes: (process.env.COGNITO_OAUTH_SCOPES ?? 'openid,profile,email').split(','),
  redirectSignIn: process.env.COGNITO_REDIRECT_SIGN_IN ?? 'http://localhost:3000',
  redirectSignOut: process.env.COGNITO_REDIRECT_SIGN_OUT ?? 'http://localhost:3000',
};

const content = `window.amplifyConfig = ${JSON.stringify(config, null, 2)};\n`;

await fs.mkdir(path.dirname(outputPath), { recursive: true });
await fs.writeFile(outputPath, content, 'utf8');

if (process.env.APP_ENV !== 'production') {
  console.log('[generate-amplify-config] Written to', outputPath);
}
