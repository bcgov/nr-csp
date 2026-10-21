import { BCEID_BUSINESS, privilegesFromFamRoles, submitterRoleFor } from './famRoles';
import { ROLES } from './permissions';

import type { Role } from './permissions';
import type { AuthUser } from './types';

/**
 * The local mock user (dev and E2E only), built from the mock role selector's choice and sent to
 * the backend's mock filter so the API enforces the same identity.
 */

export const MOCK_ROLE_KEY = 'csp.mockRole';
/** Comma-separated forest clients of the mock SUBMITTER. */
export const MOCK_FOREST_CLIENTS_KEY = 'csp.mockForestClients';
const DEFAULT_MOCK_FOREST_CLIENTS = ['00000001'];

export function getStoredRole(): Role {
  const stored = localStorage.getItem(MOCK_ROLE_KEY);
  return (ROLES as readonly string[]).includes(stored ?? '') ? (stored as Role) : 'ADMIN';
}

function getStoredForestClients(): string[] {
  const clients = (localStorage.getItem(MOCK_FOREST_CLIENTS_KEY) ?? '')
    .split(',')
    .map((client) => client.trim())
    .filter(Boolean);
  return clients.length ? clients : DEFAULT_MOCK_FOREST_CLIENTS;
}

/** What a real CSS token would carry for the stored mock role. */
function mockIdentity(): { identityProvider: string; roles: string[] } {
  const role = getStoredRole();
  return role === 'SUBMITTER'
    ? { identityProvider: BCEID_BUSINESS, roles: getStoredForestClients().map(submitterRoleFor) }
    : { identityProvider: 'idir', roles: [`CSP_${role}`] };
}

export function mockUser(): AuthUser {
  const { identityProvider, roles } = mockIdentity();
  return {
    username: 'mock-user',
    idirUsername: 'mock-user',
    displayName: 'Mock User',
    email: 'mock@example.com',
    identityProvider,
    roles,
    ...privilegesFromFamRoles(identityProvider, roles),
  };
}

/** Headers the backend's mock filter reads. */
export function mockAuthHeaders(): Record<string, string> {
  const { identityProvider, roles } = mockIdentity();
  return { 'X-Mock-Identity-Provider': identityProvider, 'X-Mock-Roles': roles.join(',') };
}
