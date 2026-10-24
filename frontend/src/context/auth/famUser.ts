import { env } from '@/env';
import { trimTrailingSlashes } from '@/utils/url';

import { BCEID_BUSINESS, privilegesFromFamRoles } from './famRoles';

import type { AuthUser } from './types';

const stringClaim = (value: unknown): string | undefined => (typeof value === 'string' ? value : undefined);

export function famUserFromAccessToken(token: string): AuthUser {
  const payload = token.split('.')[1];
  if (!payload) throw new Error('Invalid access token');
  const claims = JSON.parse(
    new TextDecoder().decode(
      Uint8Array.from(atob(payload.replaceAll('-', '+').replaceAll('_', '/')), (char) => char.codePointAt(0) ?? 0),
    ),
  ) as Record<string, unknown>;
  const provider = stringClaim(claims.identity_provider);
  const username = stringClaim(provider === BCEID_BUSINESS ? claims.bceid_username : claims.idir_username);
  const rolesClaim =
    (Array.isArray(claims.client_roles) && claims.client_roles.length ? claims.client_roles : undefined) ??
    (claims.resource_access as Record<string, { roles?: unknown }> | undefined)?.[env.VITE_OIDC_CLIENT_ID ?? '']?.roles;
  const roles = Array.isArray(rolesClaim)
    ? rolesClaim
        .filter((role): role is string => typeof role === 'string')
        .map((role) => role.trim().toUpperCase())
        .filter((role) => !role.startsWith('FAM:'))
    : [];
  const issuer = env.VITE_OIDC_ISSUER_URI;
  const clientMatches =
    claims.azp === env.VITE_OIDC_CLIENT_ID &&
    claims.iss === (issuer ? trimTrailingSlashes(issuer) : issuer) &&
    claims.typ === 'Bearer';
  const { privileges, forestClientNumbers } = clientMatches
    ? privilegesFromFamRoles(provider, roles)
    : { privileges: [], forestClientNumbers: [] };
  return {
    username: username ?? stringClaim(claims.sub) ?? '',
    idirUsername: username,
    displayName:
      stringClaim(claims.display_name) ??
      stringClaim(claims.name) ??
      [stringClaim(claims.given_name), stringClaim(claims.family_name)].filter(Boolean).join(' '),
    email: stringClaim(claims.email) ?? '',
    identityProvider: provider,
    roles,
    privileges,
    forestClientNumbers,
  };
}
