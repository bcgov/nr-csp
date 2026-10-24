import type { Role } from './permissions';
import type { SignOutReason } from './signOutReason';

/** The signed-in user, read from CSS access-token claims by `famUserFromAccessToken`. */
export interface AuthUser {
  /** {@link idirUsername} when present, otherwise the Keycloak subject (`sub`, e.g. "<guid>@azureidir"). */
  username: string;
  /**
   * Provider-specific directory username (e.g. "JSMITH"): the `idir_username` claim
   * for IDIR/IDIR MFA, or `bceid_username` for Business BCeID. This is the value the
   * backend resolves as the principal and writes to audit fields like `entryUserID`,
   * so prefer it over {@link username} anywhere the UI shows who entered or submitted
   * a record.
   */
  idirUsername?: string;
  /** `display_name`, then `name`, then `given_name` and `family_name`. */
  displayName?: string;
  email: string;
  /** The `identity_provider` claim: "idir"/"azureidir" for staff, "bceidbusiness" for industry. */
  identityProvider?: string;
  /**
   * FAM role codes (e.g. "CSP_VIEW", "CSP_SUBMITTER_FOREST_CLIENT-00012345") from
   * `client_roles`, falling back to `resource_access.<client>.roles`; trimmed,
   * upper-cased, without `FAM:` roles.
   */
  roles: string[];
  /**
   * Derived app-level permissions extracted from {@link roles} by
   * `privilegesFromFamRoles`: "ADMIN" | "APPROVE" | "VIEW" for IDIR staff, or
   * "SUBMITTER" for a Business BCeID industry user. Empty means no CSP access.
   */
  privileges: Role[];
  /**
   * Forest clients a SUBMITTER may upload for and see history of, one per
   * CSP_SUBMITTER_FOREST_CLIENT-<number> role. Empty for staff, who see every client.
   */
  forestClientNumbers: string[];
}

export interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isSigningOut: boolean;
  loginError?: string;
  signIn: (provider?: LoginProvider) => Promise<void>;
  /** A `'timeout'` reason makes the welcome screen say the session expired; defaults to a deliberate sign-out. */
  signOut: (reason?: SignOutReason) => Promise<void>;
}

export type LoginProvider = 'idir' | 'business-bceid';
