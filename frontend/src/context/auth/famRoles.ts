import { STAFF_ROLES, type Role } from './permissions';

/**
 * Maps an identity provider and its FAM role codes to CSP privileges. Mirrors
 * the backend's CspAuthorities, which is what the API enforces:
 *
 * - IDIR staff get ADMIN/APPROVE/VIEW from the exact unscoped codes
 *   CSP_ADMIN/CSP_APPROVE/CSP_VIEW, and never the industry role.
 * - Business BCeID users get SUBMITTER, plus the forest clients they may act
 *   for, from exact CSP_SUBMITTER_FOREST_CLIENT-<8 digits> grants (FAM issues
 *   one per client and never the bare code), and never a staff role.
 * - Any other identity provider gets nothing.
 *
 * Codes are trimmed and upper-cased before matching, as the backend does.
 * FAM: bookkeeping roles (e.g. FAM:EXPIRES:…) are ignored.
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

/**
 * Splits a role name into its base role and scope suffixes, in written order.
 * Port of FAM's CssRoleNaming.parse: suffixes are stripped from the right, and a
 * hyphen is a scope separator only when what precedes it ends in a known scope
 * type — never split on the last underscore (FOREST_CLIENT has one).
 */
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

/** The forest client of an exact CSP_SUBMITTER_FOREST_CLIENT-<8 digits> grant. */
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
