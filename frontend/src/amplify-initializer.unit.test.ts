import { describe, it, expect, afterEach } from 'vitest';

import { getAmplifyConfig } from './amplify-initializer';

const validConfig = {
  appEnv: 'test',
  region: 'ca-central-1',
  userPoolId: 'ca-central-1_test',
  userPoolClientId: 'test-client-id',
  cognitoDomain: 'auth.example.com',
  oauthScopes: ['openid'],
  redirectSignIn: 'https://example.com/',
  redirectSignOut: 'https://logontest7.gov.bc.ca/clp-cgi/logoff.cgi?retnow=1&returl=https://example.com/logout',
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

  it('returns correct oauth domain', () => {
    (window as any).amplifyConfig = validConfig;
    const config = getAmplifyConfig();
    expect(config.Auth.Cognito.loginWith.oauth.domain).toBe('auth.example.com');
  });

  it('sets responseType to "code"', () => {
    (window as any).amplifyConfig = validConfig;
    const config = getAmplifyConfig();
    expect(config.Auth.Cognito.loginWith.oauth.responseType).toBe('code');
  });

  it('does not mutate the original window.amplifyConfig', () => {
    (window as any).amplifyConfig = JSON.parse(JSON.stringify(validConfig));
    getAmplifyConfig();
    expect((window as any).amplifyConfig.userPoolId).toBe('ca-central-1_test');
  });
});
