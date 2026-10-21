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

  it('trims and upper-cases role codes before matching', () => {
    const user = famUserFromAccessToken(token({ client_roles: ['csp_view', ' CSP_ADMIN '] }));
    expect(user).toMatchObject({ roles: ['CSP_VIEW', 'CSP_ADMIN'], privileges: ['ADMIN', 'VIEW'] });
  });

  it('gives a Business BCeID submitter its forest clients and no staff role', () => {
    const user = famUserFromAccessToken(
      token({
        identity_provider: 'bceidbusiness',
        idir_username: undefined,
        bceid_username: 'ACME_USER',
        client_roles: [
          'CSP_SUBMITTER_FOREST_CLIENT-00000002',
          'CSP_ADMIN',
          'CSP_SUBMITTER_FOREST_CLIENT-00000001',
          'FAM:EXPIRES:2026-09-30:CSP_SUBMITTER_FOREST_CLIENT-00000001',
        ],
      }),
    );
    expect(user).toMatchObject({
      username: 'ACME_USER',
      identityProvider: 'bceidbusiness',
      privileges: ['SUBMITTER'],
      forestClientNumbers: ['00000001', '00000002'],
    });
    expect(user.roles).not.toContain('FAM:EXPIRES:2026-09-30:CSP_SUBMITTER_FOREST_CLIENT-00000001');
  });

  it('gives an IDIR token no industry role', () => {
    const user = famUserFromAccessToken(token({ client_roles: ['CSP_SUBMITTER_FOREST_CLIENT-00000001'] }));
    expect(user).toMatchObject({ privileges: [], forestClientNumbers: [] });
  });

  it('gives a Business BCeID user without a client-scoped role nothing', () => {
    const user = famUserFromAccessToken(
      token({ identity_provider: 'bceidbusiness', bceid_username: 'ACME_USER', client_roles: ['CSP_SUBMITTER'] }),
    );
    expect(user).toMatchObject({ privileges: [], forestClientNumbers: [] });
  });

  it('gives another identity provider nothing', () => {
    const user = famUserFromAccessToken(token({ identity_provider: 'github', client_roles: ['CSP_ADMIN'] }));
    expect(user.privileges).toEqual([]);
  });

  it('gives a token for another client nothing', () => {
    const user = famUserFromAccessToken(
      token({
        azp: 'other-client',
        identity_provider: 'bceidbusiness',
        client_roles: ['CSP_SUBMITTER_FOREST_CLIENT-00000001'],
      }),
    );
    expect(user).toMatchObject({ privileges: [], forestClientNumbers: [] });
  });
});
