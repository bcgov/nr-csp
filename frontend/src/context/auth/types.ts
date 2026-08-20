import type { Role } from './permissions';

/** Which identity provider the user authenticated through. */
export type IdpProvider = 'IDIR' | 'BCEID';

export interface AuthUser {
  username: string;
  /**
   * IDIR username from the `custom:idp_username` id-token claim (e.g. "JSMITH").
   * This is the value the backend resolves as the principal and writes to audit
   * fields like `entryUserID`, so prefer it over {@link username} (a Cognito id)
   * anywhere the UI shows who entered or submitted a record.
   */
  idirUsername?: string;
  displayName?: string;
  email: string;
  /** Raw Cognito group names from the id-token (e.g. "CSP_SUBMITTER"). */
  roles: string[];
  /**
   * Derived app-level permissions extracted from {@link roles}.
   * Values are uppercase role constants: "ADMIN" | "APPROVE" | "VIEW".
   */
  privileges: Role[];
  /**
   * Identity provider the user authenticated through, derived from the
   * `custom:idp_name` id-token claim. Defaults to "IDIR" when the claim is
   * absent. BCeID users are restricted to a small subset of pages/routes —
   * see ProtectedRoute and navigation.ts.
   */
  idpProvider: IdpProvider;
}

export interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isSigningOut: boolean;
  signIn: (provider: IdpProvider) => Promise<void>;
  signOut: () => Promise<void>;
}
