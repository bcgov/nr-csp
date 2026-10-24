import { describe, expect, it } from 'vitest';

import { parseRoleName, privilegesFromFamRoles, submitterClient, submitterRoleFor } from './famRoles';

// Parse cases ported from nr-fam's CssRoleNamingTest (the reference implementation).
describe('parseRoleName', () => {
  it.each([
    ['CHR_FREP_EDITOR_DISTRICT-DCC', 'CHR_FREP_EDITOR', 'DISTRICT', 'DCC'],
    ['FOM_SUBMITTER_FOREST_CLIENT-00001018', 'FOM_SUBMITTER', 'FOREST_CLIENT', '00001018'],
    ['CSP_SUBMITTER_FOREST_CLIENT-00012345', 'CSP_SUBMITTER', 'FOREST_CLIENT', '00012345'],
    ['FREP_EDITOR_REGION-KOOTENAY_BOUNDARY', 'FREP_EDITOR', 'REGION', 'KOOTENAY_BOUNDARY'],
    ['A_B_C_DISTRICT-DQU', 'A_B_C', 'DISTRICT', 'DQU'],
  ])('parses %s', (name, base, type, value) => {
    expect(parseRoleName(name)).toEqual({ base, scopes: [{ type, value }] });
  });

  it('keeps a multi-word scope type intact rather than splitting on the last underscore', () => {
    const parsed = parseRoleName('FOM_SUBMITTER_FOREST_CLIENT-00001018');
    expect(parsed.base).not.toBe('FOM_SUBMITTER_FOREST');
    expect(parsed.scopes.map((s) => s.type)).not.toContain('CLIENT');
  });

  it.each(['FREP_EDITOR', 'SOME-ROLE', 'PLAIN', '_DISTRICT-DCC', 'CSP_ADMIN'])('leaves %s unscoped', (name) => {
    expect(parseRoleName(name)).toEqual({ base: name, scopes: [] });
  });

  it('reads a compound name in written order', () => {
    expect(parseRoleName('FOM_SUBMITTER_DISTRICT-DCC_FOREST_CLIENT-00001012')).toEqual({
      base: 'FOM_SUBMITTER',
      scopes: [
        { type: 'DISTRICT', value: 'DCC' },
        { type: 'FOREST_CLIENT', value: '00001012' },
      ],
    });
  });

  it('keeps a hyphen in the base role', () => {
    expect(parseRoleName('SOME-ROLE_DISTRICT-DCC')).toEqual({
      base: 'SOME-ROLE',
      scopes: [{ type: 'DISTRICT', value: 'DCC' }],
    });
  });

  it('never reads a FAM expiry sidecar as the role it governs', () => {
    expect(parseRoleName('FAM:EXPIRES:2026-09-30:CSP_SUBMITTER_FOREST_CLIENT-00012345').base).not.toBe('CSP_SUBMITTER');
  });
});

describe('submitterClient', () => {
  it('reads the client of an exact submitter grant, and round-trips submitterRoleFor', () => {
    expect(submitterClient(submitterRoleFor('00012345'))).toBe('00012345');
  });

  it.each([
    'CSP_SUBMITTER',
    'CSP_SUBMITTER_DISTRICT-DCC',
    'CSP_SUBMITTER_DISTRICT-DCC_FOREST_CLIENT-00012345',
    'CSP_SUBMITTER_FOREST_CLIENT-1234567',
    'CSP_SUBMITTER_FOREST_CLIENT-000123456',
    'CSP_SUBMITTER_FOREST_CLIENT-0001234A',
    'ILCR_SUBMITTER_FOREST_CLIENT-00012345',
    'CSP_ADMIN_FOREST_CLIENT-00012345',
  ])('rejects %s', (role) => {
    expect(submitterClient(role)).toBeUndefined();
  });
});

describe('privilegesFromFamRoles', () => {
  const none = { privileges: [], forestClientNumbers: [] };

  it.each(['idir', 'azureidir'])('gives %s staff their exact unscoped roles', (idp) => {
    expect(privilegesFromFamRoles(idp, ['CSP_VIEW', 'CSP_ADMIN', 'ADMIN', 'OTHER_CSP_APPROVE'])).toEqual({
      privileges: ['ADMIN', 'VIEW'],
      forestClientNumbers: [],
    });
  });

  it('ignores case and padding, as the backend does', () => {
    expect(privilegesFromFamRoles('idir', ['csp_admin', ' CSP_VIEW', 'Csp_Approve '])).toEqual({
      privileges: ['ADMIN', 'APPROVE', 'VIEW'],
      forestClientNumbers: [],
    });
    expect(privilegesFromFamRoles('bceidbusiness', [' csp_submitter_forest_client-00012345 '])).toEqual({
      privileges: ['SUBMITTER'],
      forestClientNumbers: ['00012345'],
    });
  });

  it('matches whole staff codes only', () => {
    expect(privilegesFromFamRoles('idir', ['ADMIN', 'NRS_CSP_ADMIN', 'CSP_ADMINISTRATOR', ''])).toEqual(none);
  });

  it('never gives staff the industry role', () => {
    expect(privilegesFromFamRoles('azureidir', ['CSP_SUBMITTER_FOREST_CLIENT-00012345'])).toEqual(none);
  });

  it('gives a Business BCeID submitter their forest clients, sorted and distinct', () => {
    expect(
      privilegesFromFamRoles('bceidbusiness', [
        'CSP_SUBMITTER_FOREST_CLIENT-00000002',
        'CSP_SUBMITTER_FOREST_CLIENT-00000001',
        'CSP_SUBMITTER_FOREST_CLIENT-00000002',
      ]),
    ).toEqual({ privileges: ['SUBMITTER'], forestClientNumbers: ['00000001', '00000002'] });
  });

  it('never gives Business BCeID a staff role', () => {
    expect(privilegesFromFamRoles('bceidbusiness', ['CSP_ADMIN', 'CSP_VIEW'])).toEqual(none);
  });

  it('gives Business BCeID nothing without a client-scoped grant', () => {
    expect(privilegesFromFamRoles('bceidbusiness', ['CSP_SUBMITTER'])).toEqual(none);
    expect(privilegesFromFamRoles('bceidbusiness', [])).toEqual(none);
  });

  it('ignores FAM bookkeeping roles', () => {
    expect(
      privilegesFromFamRoles('bceidbusiness', ['FAM:EXPIRES:2026-09-30:CSP_SUBMITTER_FOREST_CLIENT-00012345']),
    ).toEqual(none);
  });

  it.each([undefined, '', 'bceidboth', 'github', 'IDIR'])('gives identity provider %s nothing', (idp) => {
    expect(privilegesFromFamRoles(idp, ['CSP_ADMIN', 'CSP_SUBMITTER_FOREST_CLIENT-00012345'])).toEqual(none);
  });
});
