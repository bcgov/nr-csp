import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { buildFederatedLogoutUrl, clearStoredTokens, openIdirRealmLogoutPopup } from './logoutChain';

const fullConfig = {
  appEnv: 'test',
  idpName: 'TEST-IDIR',
  region: 'ca-central-1',
  userPoolId: 'ca-central-1_test',
  userPoolClientId: 'cognito-client-id',
  cognitoDomain: 'pool.auth.ca-central-1.amazoncognito.com',
  redirectSignIn: 'https://app.example.com',
  redirectSignOut: 'https://app.example.com/logout',
  logoutSiteminderUrl: 'https://logontest7.gov.bc.ca/clp-cgi/logoff.cgi',
  logoutKeycloakUrl: 'https://test.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/logout',
  logoutKeycloakClientId: 'fsa-cognito-idir-dev-4088',
};

describe('buildFederatedLogoutUrl', () => {
  it('nests Cognito inside Keycloak inside SiteMinder, each encoded exactly once', () => {
    const url = buildFederatedLogoutUrl(fullConfig);

    const cognitoLogout =
      'https://pool.auth.ca-central-1.amazoncognito.com/logout' +
      '?client_id=cognito-client-id' +
      `&logout_uri=${encodeURIComponent('https://app.example.com/logout')}`;
    const keycloakLogout =
      'https://test.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/logout' +
      '?client_id=fsa-cognito-idir-dev-4088' +
      `&post_logout_redirect_uri=${encodeURIComponent(cognitoLogout)}`;

    expect(url).toBe(
      `https://logontest7.gov.bc.ca/clp-cgi/logoff.cgi?retnow=1&returl=${encodeURIComponent(keycloakLogout)}`,
    );
  });

  it('round-trips each hop through a query-string parse', () => {
    const url = buildFederatedLogoutUrl(fullConfig);

    const siteminder = new URL(url!);
    const keycloak = new URL(siteminder.searchParams.get('returl')!);
    const cognito = new URL(keycloak.searchParams.get('post_logout_redirect_uri')!);

    expect(siteminder.searchParams.get('retnow')).toBe('1');
    expect(keycloak.searchParams.get('client_id')).toBe('fsa-cognito-idir-dev-4088');
    expect(cognito.origin).toBe('https://pool.auth.ca-central-1.amazoncognito.com');
    expect(cognito.searchParams.get('client_id')).toBe('cognito-client-id');
    expect(cognito.searchParams.get('logout_uri')).toBe('https://app.example.com/logout');
  });

  it.each([
    'logoutSiteminderUrl',
    'logoutKeycloakUrl',
    'logoutKeycloakClientId',
    'cognitoDomain',
    'userPoolClientId',
    'redirectSignOut',
  ] as const)('returns null when %s is missing', (field) => {
    expect(buildFederatedLogoutUrl({ ...fullConfig, [field]: undefined })).toBeNull();
    expect(buildFederatedLogoutUrl({ ...fullConfig, [field]: '  ' })).toBeNull();
  });

  it('returns null when config is undefined', () => {
    expect(buildFederatedLogoutUrl(undefined)).toBeNull();
  });
});

describe('openIdirRealmLogoutPopup', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('opens the idir-realm variant of the Keycloak logout URL as a named popup', () => {
    const fakePopup = {} as Window;
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(fakePopup);

    const result = openIdirRealmLogoutPopup(fullConfig);

    expect(openSpy).toHaveBeenCalledWith(
      'https://test.loginproxy.gov.bc.ca/auth/realms/idir/protocol/openid-connect/logout',
      'csp-idir-realm-logout',
      expect.stringContaining('popup'),
    );
    expect(result).toBe(fakePopup);
  });

  it('returns null without opening when the Keycloak logout URL is missing', () => {
    const openSpy = vi.spyOn(window, 'open');
    expect(openIdirRealmLogoutPopup({ ...fullConfig, logoutKeycloakUrl: undefined })).toBeNull();
    expect(openIdirRealmLogoutPopup(undefined)).toBeNull();
    expect(openSpy).not.toHaveBeenCalled();
  });

  it('returns null when the URL is not a standard-realm endpoint', () => {
    const openSpy = vi.spyOn(window, 'open');
    expect(
      openIdirRealmLogoutPopup({
        ...fullConfig,
        logoutKeycloakUrl: 'https://example.com/auth/realms/custom/protocol/openid-connect/logout',
      }),
    ).toBeNull();
    expect(openSpy).not.toHaveBeenCalled();
  });

  it('returns null when window.open throws', () => {
    vi.spyOn(window, 'open').mockImplementation(() => {
      throw new Error('blocked');
    });
    expect(openIdirRealmLogoutPopup(fullConfig)).toBeNull();
  });
});

describe('clearStoredTokens', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('removes only the Amplify token keys for the given client id', () => {
    window.localStorage.setItem('CognitoIdentityServiceProvider.cognito-client-id.user.idToken', 'token');
    window.localStorage.setItem('CognitoIdentityServiceProvider.cognito-client-id.LastAuthUser', 'user');
    window.localStorage.setItem('CognitoIdentityServiceProvider.other-client.user.idToken', 'keep');
    window.localStorage.setItem('unrelated', 'keep');

    clearStoredTokens('cognito-client-id');

    expect(window.localStorage.getItem('CognitoIdentityServiceProvider.cognito-client-id.user.idToken')).toBeNull();
    expect(window.localStorage.getItem('CognitoIdentityServiceProvider.cognito-client-id.LastAuthUser')).toBeNull();
    expect(window.localStorage.getItem('CognitoIdentityServiceProvider.other-client.user.idToken')).toBe('keep');
    expect(window.localStorage.getItem('unrelated')).toBe('keep');
  });

  it('is a no-op without a client id', () => {
    window.localStorage.setItem('CognitoIdentityServiceProvider.x.idToken', 'keep');
    clearStoredTokens(undefined);
    expect(window.localStorage.getItem('CognitoIdentityServiceProvider.x.idToken')).toBe('keep');
  });
});
