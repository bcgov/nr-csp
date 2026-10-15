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
  /**
   * FAM role codes (e.g. "CSP_VIEW") from `client_roles`, falling back to
   * `resource_access.<client>.roles`; trimmed, upper-cased, without `FAM:` roles.
   */
  roles: string[];
  /**
   * Derived app-level permissions extracted from {@link roles}.
   * Values are uppercase role constants: "ADMIN" | "APPROVE" | "VIEW".
   */
  privileges: Role[];
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
