import { describe, expect, it, vi } from 'vitest';

import { famUserFromAccessToken } from './famUser';

vi.mock('@/env', () => ({
  env: {
    VITE_OIDC_ISSUER_URI: 'https://sso.test/realms/standard///',
    VITE_OIDC_CLIENT_ID: 'csp-browser',
  },
}));

function token(claims: Record<string, unknown>): string {
  const bytes = new TextEncoder().encode(
    JSON.stringify({
      iss: 'https://sso.test/realms/standard',
      azp: 'csp-browser',
      typ: 'Bearer',
      identity_provider: 'azureidir',
      idir_username: 'JSMITH',
      client_roles: ['CSP_VIEW'],
      ...claims,
    }),
  );
  const payload = btoa(Array.from(bytes, (byte) => String.fromCodePoint(byte)).join(''))
    .replaceAll('+', '-')
    .replaceAll('/', '_')
    .split('=')[0];
  return `header.${payload}.signature`;
}

describe('FAM access-token profile', () => {
  it('decodes Unicode names and matches an issuer with trailing separators', () => {
    const user = famUserFromAccessToken(token({ display_name: 'José 林', email: 'jose@example.test' }));
    expect(user).toMatchObject({
      username: 'JSMITH',
      idirUsername: 'JSMITH',
      displayName: 'José 林',
      email: 'jose@example.test',
      privileges: ['VIEW'],
    });
  });

  it('does not stringify non-string profile claims', () => {
    const user = famUserFromAccessToken(
      token({
        idir_username: {},
        sub: {},
        display_name: {},
        name: [],
        given_name: {},
        family_name: 'Family',
        email: {},
      }),
    );
    expect(user).toMatchObject({ username: '', displayName: 'Family', email: '' });
    expect(user.idirUsername).toBeUndefined();
  });

  it('uses a string subject only when the directory username is absent', () => {
    const user = famUserFromAccessToken(token({ idir_username: undefined, sub: 'user-uuid', name: 'Display Name' }));
    expect(user).toMatchObject({ username: 'user-uuid', displayName: 'Display Name' });
    expect(user.idirUsername).toBeUndefined();
  });

  it('preserves explicitly empty string claims', () => {
    const user = famUserFromAccessToken(
      token({ idir_username: '', sub: 'user-uuid', display_name: '', name: 'Unused fallback', email: '' }),
    );
    expect(user).toMatchObject({ username: '', idirUsername: '', displayName: '', email: '' });
  });
});
