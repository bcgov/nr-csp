import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { getAmplifyConfig } from './amplify-initializer';

const validConfig = {
  appEnv: 'test',
  context: '',
  region: 'ca-central-1',
  userPoolId: 'ca-central-1_test',
  userPoolClientId: 'test-client-id',
  cognitoDomain: 'auth.example.com',
  oauthScopes: ['openid', 'email'],
  redirectSignIn: 'http://localhost:3000',
  redirectSignOut: 'http://localhost:3000',
};

describe('getAmplifyConfig', () => {
  afterEach(() => {
    delete (window as any).amplifyConfig;
  });

  it('throws when window.amplifyConfig is not set', () => {
    delete (window as any).amplifyConfig;
    expect(() => getAmplifyConfig()).toThrow(/window\.amplifyConfig not found/);
  });

  it('returns Auth.Cognito config with correct userPoolId', () => {
    (window as any).amplifyConfig = validConfig;
    const config = getAmplifyConfig();
    expect(config.Auth.Cognito.userPoolId).toBe('ca-central-1_test');
  });

  it('returns Auth.Cognito config with correct userPoolClientId', () => {
    (window as any).amplifyConfig = validConfig;
    const config = getAmplifyConfig();
    expect(config.Auth.Cognito.userPoolClientId).toBe('test-client-id');
  });

  it('sets responseType to "code"', () => {
    (window as any).amplifyConfig = validConfig;
    const config = getAmplifyConfig();
    expect(config.Auth.Cognito.loginWith.oauth.responseType).toBe('code');
  });

  it('wraps redirectSignIn in an array', () => {
    (window as any).amplifyConfig = validConfig;
    const config = getAmplifyConfig();
    expect(config.Auth.Cognito.loginWith.oauth.redirectSignIn).toEqual(['http://localhost:3000']);
  });

  it('does not mutate the original window.amplifyConfig', () => {
    (window as any).amplifyConfig = { ...validConfig };
    getAmplifyConfig();
    expect((window as any).amplifyConfig.userPoolId).toBe('ca-central-1_test');
  });
});
