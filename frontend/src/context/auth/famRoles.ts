import { STAFF_ROLES, type Role } from './permissions';

/**
 * Maps an identity provider and its FAM role codes to CSP privileges, mirroring the backend's
 * CspAuthorities. IDIR gets ADMIN, APPROVE or VIEW; Business BCeID gets SUBMITTER and its
 * clients from CSP_SUBMITTER_FOREST_CLIENT-00012345 roles; anything else gets nothing.
 */

const STAFF_PROVIDERS: readonly string[] = ['idir', 'azureidir'];
export const BCEID_BUSINESS = 'bceidbusiness';
const FAM_SUBMITTER = 'CSP_SUBMITTER';

const SCOPE_TYPES = ['FOREST_CLIENT', 'DISTRICT', 'REGION'] as const; // longest first
const CLIENT_NUMBER = /^[0-9]{8}$/;

export interface RoleScope {
  type: string;
  value: string;
}

/** Port of FAM's CssRoleNaming.parse: strips scope suffixes from the right. */
export function parseRoleName(roleName: string): { base: string; scopes: RoleScope[] } {
  const scopes: RoleScope[] = [];
  let remaining = roleName;
  for (;;) {
    const hyphen = remaining.lastIndexOf('-');
    if (hyphen < 0) break;
    const head = remaining.slice(0, hyphen);
    const type = SCOPE_TYPES.find((t) => head.endsWith(`_${t}`) && head.length > t.length + 1);
    if (!type) break;
    scopes.unshift({ type, value: remaining.slice(hyphen + 1) });
    remaining = head.slice(0, head.length - type.length - 1);
  }
  return { base: remaining, scopes };
}

/** The 8-digit client of a CSP_SUBMITTER_FOREST_CLIENT-00012345 role. */
export function submitterClient(role: string): string | undefined {
  const { base, scopes } = parseRoleName(role);
  if (base !== FAM_SUBMITTER || scopes.length !== 1) return undefined;
  const [scope] = scopes;
  return scope.type === 'FOREST_CLIENT' && CLIENT_NUMBER.test(scope.value) ? scope.value : undefined;
}

/** The FAM code granting SUBMITTER for one forest client. */
export const submitterRoleFor = (clientNumber: string) => `${FAM_SUBMITTER}_FOREST_CLIENT-${clientNumber}`;

export function privilegesFromFamRoles(
  identityProvider: string | undefined,
  famRoles: readonly string[],
): { privileges: Role[]; forestClientNumbers: string[] } {
  const roles = famRoles.map((role) => role.trim().toUpperCase()).filter((role) => role && !role.startsWith('FAM:'));
  if (identityProvider && STAFF_PROVIDERS.includes(identityProvider)) {
    return { privileges: STAFF_ROLES.filter((role) => roles.includes(`CSP_${role}`)), forestClientNumbers: [] };
  }
  if (identityProvider !== BCEID_BUSINESS) return { privileges: [], forestClientNumbers: [] };
  const clients = [...new Set(roles.map(submitterClient).filter((c): c is string => c !== undefined))].sort();
  return clients.length
    ? { privileges: ['SUBMITTER'], forestClientNumbers: clients }
    : { privileges: [], forestClientNumbers: [] };
}
